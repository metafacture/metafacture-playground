(ns ls-client.jsonrpc
  "JSON-RPC 2.0 protocol implementation over WebSocket.
   Handles request/response matching, async message routing, and error handling."
  (:require [cljs.core.async :as a :refer [<! >! go go-loop chan close!]]))

;; ============================================================================
;; Message ID Counter
;; ============================================================================

(def ^:private message-id-counter (atom 0))

(defn- next-id []
  (swap! message-id-counter inc))

;; ============================================================================
;; Request/Response Registry
;; ============================================================================

(defn ^:private normalize-id [id]
  "Ensure ID is a number for consistent lookup."
  (if (string? id)
    (js/parseInt id)
    id))

(defn create-message-registry []
  "Create a registry to track pending requests and match responses."
  {:pending (atom {})
   :notifications (atom [])})

(defn register-request [registry id response-chan]
  "Register a pending request with its response channel."
  (let [normalized-id (normalize-id id)]
    (swap! (:pending registry) assoc normalized-id response-chan)))

(defn get-response-chan [registry id]
  "Get the response channel for a request ID."
  (let [pending (:pending registry)
        normalized-id (normalize-id id)
        chan (get @pending normalized-id)]
    (when chan
      (swap! pending dissoc normalized-id))
    chan))

(defn store-notification [registry notification]
  "Store an unsolicited notification from the server."
  (swap! (:notifications registry) conj notification))

(defn get-notifications [registry]
  "Get all stored notifications."
  (let [notifs @(:notifications registry)]
    (reset! (:notifications registry) [])
    notifs))

;; ============================================================================
;; Connection Management
;; ============================================================================

(defn ^:private serialize-message [msg]
  "Serialize a message for sending over WebSocket."
  (clj->js msg))

(defn ^:private deserialize-message [msg]
  "Deserialize a message received from WebSocket.
   haslett with fmt/json gives us a ClojureScript map with string keys.
   Always convert string keys to keywords for consistent access."
  (cond
    ;; If it's a JS object, convert to ClojureScript with keyword keys
    (object? msg)
    (js->clj msg :keywordize-keys true)
    
    ;; If it's already a ClojureScript map, check if keys are strings or keywords
    (map? msg)
    (if (some string? (keys msg))
      ;; String keys - convert to keywords
      (reduce-kv (fn [m k v] (assoc m (keyword k) v)) {} msg)
      ;; Already has keyword keys
      msg)
    
    ;; Fallback
    :else
    msg))

(defn ^:private message-handler-loop
  "Continuously read messages from the server and route them appropriately."
  [stream registry notification-handler]
  (go-loop []
    (let [msg (<! (:in stream))]
      (when msg
        (try
          (let [msg-clj (deserialize-message msg)
                id-raw (:id msg-clj)
                id (when id-raw (normalize-id id-raw))
                method (:method msg-clj)]
            (cond
              id
              ;; This is a response - look for matching request
              (if-let [response-chan (get-response-chan registry id)]
                (do
                  (>! response-chan msg-clj)
                  (close! response-chan))
                (js/console.warn "[JSONRPC] ✗ Received response for unknown request ID:" id))

              method
              ;; Server push notification
              (notification-handler msg-clj)

              :else
              ;; Neither response nor notification
              (js/console.warn "[JSONRPC] ⚠ Received unexpected message:" (clj->js msg-clj))))
          (catch js/Error e
            (js/console.error "[JSONRPC] ✗ Error processing message:" e))))
      (recur))))

(defn ^:private create-ws-stream [ws]
  "Create a stream-like object from a native WebSocket with JSON handling."
  (let [in-chan (chan)
        out-chan (chan)]
    ;; Set up message handler - parse JSON and feed into in-chan
    (set! (.-onmessage ws)
      (fn [event]
        (go
          (try
            (let [json-str (.-data event)
                  msg (js/JSON.parse json-str)]
              (>! in-chan msg))
            (catch js/Error e
              (js/console.error "[JSONRPC] Failed to parse message:" e))))))
    
    ;; Set up output - consume from out-chan, stringify JSON, and send over WebSocket
    (go-loop []
      (when-let [msg (<! out-chan)]
        (try
          (.send ws (js/JSON.stringify msg))
          (catch js/Error e
            (js/console.error "[JSONRPC] Failed to send message:" e)))
        (recur)))
    
    {:in in-chan :out out-chan :ws ws}))

(defn create-connection [url]
  "Create a WebSocket connection using native API.
   Returns a channel that resolves to:
   - {:error false :stream :registry} on success
   - {:error true :message} on failure."
  (let [result-ch (chan)]
    (go
      (let [result-sent (atom false)
            ws (js/WebSocket. url)]
        
        ;; Set up success handler
        (set! (.-onopen ws)
          (fn []
            (when-not @result-sent
              (reset! result-sent true)
              (go
                (let [stream (create-ws-stream ws)
                      registry (create-message-registry)]
                  (>! result-ch {:error false :stream stream :registry registry}))))))
        
        ;; Set up error handler
        (set! (.-onerror ws)
          (fn [error]
            (when-not @result-sent
              (reset! result-sent true)
              (js/console.warn "[JSONRPC] WebSocket error")
              (go
                (>! result-ch {:error true :message "Language server unavailable"})))))
        
        ;; Set up close handler
        (set! (.-onclose ws)
          (fn []
            (when-not @result-sent
              (reset! result-sent true)
              (js/console.warn "[JSONRPC] Connection closed")
              (go
                (>! result-ch {:error true :message "Language server unavailable"})))))
        
        ;; Timeout
        (<! (a/timeout 5000))
        (when-not @result-sent
          (reset! result-sent true)
          (.close ws)
          (>! result-ch {:error true :message "Connection timeout"}))))
    result-ch))


;; ============================================================================
;; Utility: Promise to Channel Conversion
;; ============================================================================

(defn ^:private promise->channel
  "Convert a JavaScript Promise to a core.async channel."
  [promise]
  (let [ch (chan)]
    (.then promise
           (fn [result]
             (go (>! ch {:ok true :value result})))
           (fn [error]
             (go (>! ch {:ok false :error error}))))
    ch))

;; ============================================================================
;; Public API
;; ============================================================================

(defn connect
  "Connect to a JSON-RPC server over WebSocket with error handling.
   
   Returns a channel that resolves to:
   - connection map (on success): {:send, :notify, :disconnect, :registry}
   - error map (on failure): {:error true, :message}"
  [url & [{:keys [on-notification]
           :or {on-notification (fn [msg] (js/console.log "[JSONRPC] Notification:" msg))}}]]
  
  (go
    (let [conn-result (<! (create-connection url))]
      (if (:error conn-result)
        ;; Connection failed
        conn-result
        ;; Connection succeeded - start message loop and return interface
        (let [{:keys [stream registry]} conn-result]
          ;; Start the message loop in the background
          (message-handler-loop stream registry on-notification)
          
          ;; Return connection interface
          #js {:send (fn [method params]
                       "Send a request and wait for response. Returns a channel."
                       (let [id (next-id)
                             response-chan (chan)
                             msg {:jsonrpc "2.0"
                                  :id id
                                  :method method
                                  :params params}
                             result-chan (chan)]
                         (register-request registry id response-chan)
                         (go
                           (try
                             ;; Send the message
                             (>! (:out stream) (serialize-message msg))
                             ;; Wait for response
                             (let [response (<! response-chan)]
                               (>! result-chan (if (:error response)
                                                 {:error true :message (str (:message (:error response)))}
                                                 {:error false :result (:result response)})))
                             (catch js/Error e
                               (>! result-chan {:error true :message (.-message e)}))))
                         result-chan))

               :notify (fn [method params]
                         "Send a notification (no response expected)."
                         (let [msg {:jsonrpc "2.0"
                                    :method method
                                    :params params}]
                           (go
                             (try
                               (>! (:out stream) (serialize-message msg))
                               (catch js/Error e
                                 (js/console.error "[JSONRPC] Notify failed:" e))))))

               :disconnect (fn []
                             "Close the connection."
                             (.close (:ws stream)))

               :stream stream
               :registry registry})))))