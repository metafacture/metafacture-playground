(ns ls-client.jsonrpc
  "JSON-RPC 2.0 protocol implementation over WebSocket.
   Handles request/response matching, async message routing, and error handling."
  (:require [cljs.core.async :as a :refer [<! >! go chan close!]]))

;; ============================================================================
;; Message ID Counter
;; ============================================================================

(def ^:private message-id-counter (atom 0))

(defn- next-id []
  (swap! message-id-counter inc))

;; ============================================================================
;; Request/Response Registry
;; ============================================================================

(defn ^:private normalize-id
  "Ensure ID is a number for consistent lookup."
  [id]
  (if (string? id)
    (js/parseInt id)
    id))

(defn create-message-registry
  "Create a registry for tracking pending request/response pairs."
  []
  {:pending (atom {})
   :notifications (atom [])})

(defn register-request
  "Register a pending JSON-RPC request and its response channel."
  [registry id response-chan]
  (let [normalized-id (normalize-id id)]
    (swap! (:pending registry) assoc normalized-id response-chan)))

(defn get-response-chan
  "Return the response channel for a pending request and remove it from the registry."
  [registry id]
  (let [pending (:pending registry)
        normalized-id (normalize-id id)
        ch (get @pending normalized-id)]
    (when ch
      (swap! pending dissoc normalized-id))
    ch))

;; ============================================================================
;; Connection Management
;; ============================================================================

(defn- serialize-message
  "Serialize a Clojure map into JSON for the browser WebSocket."
  [msg]
  (clj->js msg))

(defn- deserialize-message
  "Deserialize a JSON message received from the WebSocket into Clojure data."
  [msg]
  (cond
    (object? msg)
    (js->clj msg :keywordize-keys true)

    (map? msg)
    (if (some string? (keys msg))
      (reduce-kv (fn [m k v] (assoc m (keyword k) v)) {} msg)
      msg)

    :else
    msg))

(defn- message-handler-loop
  "Continuously read messages from the server and route them appropriately."
  [stream registry notification-handler]
  (letfn [(read-next! []
            (a/take! (:in stream)
                     (fn [msg]
                       (when msg
                         (try
                           (let [msg-clj (deserialize-message msg)
                                 id-raw (:id msg-clj)
                                 id (when id-raw (normalize-id id-raw))
                                 method (:method msg-clj)]
                             (cond
                               id
                               (if-let [response-chan (get-response-chan registry id)]
                                 (do
                                   (a/put! response-chan msg-clj)
                                   (close! response-chan))
                                 (js/console.warn "[JSONRPC] Received response for unknown request ID:" id))

                               method
                               (notification-handler msg-clj)

                               :else
                               (js/console.warn "[JSONRPC] Received unexpected message:" (clj->js msg-clj))))
                           (catch js/Error e
                             (js/console.error "[JSONRPC] Error processing message:" e)))
                         (read-next!)))))]
    (read-next!)))

(defn- create-ws-stream
  "Create a stream-like object from a native WebSocket with JSON handling."
  [ws]
  (let [in-chan (chan)
        out-chan (chan)
        drain-out! (fn drain-out! []
                     (a/take! out-chan
                              (fn [msg]
                                (when msg
                                  (try
                                    (when (= 1 (.-readyState ws))
                                      (.send ws (js/JSON.stringify msg)))
                                    (catch js/Error e
                                      (js/console.error "[JSONRPC] Failed to send message:" e)))
                                  (drain-out!)))))]
    (set! (.-onmessage ws)
      (fn [event]
        (try
          (let [json-str (.-data event)
                msg (js/JSON.parse json-str)]
            (a/put! in-chan msg))
          (catch js/Error e
            (js/console.error "[JSONRPC] Failed to parse message:" e)))))
    (drain-out!)
    {:in in-chan :out out-chan :ws ws}))

(def ^:private max-reconnect-attempts 10)
(def ^:private initial-reconnect-delay 1000)
(def ^:private max-reconnect-delay 30000)

(defn connect
  "Connect to a JSON-RPC server over WebSocket with automatic reconnect support."
  [url & [{:keys [on-notification]
           :or {on-notification (fn [msg] (js/console.log "[JSONRPC] Notification:" msg))}}]]
  (let [state (atom {:ws nil
                     :stream nil
                     :registry nil
                     :connected false
                     :manual-close false
                     :reconnect-attempt 0
                     :reconnect-pending false})
        backoff (fn [attempt]
                  (min (* initial-reconnect-delay (js/Math.pow 2 attempt)) max-reconnect-delay))
        connect! (fn connect! []
                   (when-not (:manual-close @state)
                     (let [current (:ws @state)]
                       (when (or (nil? current)
                                 (= 3 (.-readyState current)))
                         (let [ws (js/WebSocket. url)
                               stream (create-ws-stream ws)
                               registry (create-message-registry)]
                           (swap! state assoc
                                  :ws ws
                                  :stream stream
                                  :registry registry
                                  :connected false
                                  :reconnect-attempt (max (:reconnect-attempt @state) 0))

                           (message-handler-loop stream registry on-notification)

                           (set! (.-onopen ws)
                                 (fn []
                                   (swap! state assoc :connected true :reconnect-attempt 0 :reconnect-pending false)
                                   (js/console.info "[JSONRPC] WebSocket connected")))

                           (set! (.-onerror ws)
                                 (fn [e]
                                   (js/console.warn "[JSONRPC] WebSocket error" e)
                                   (swap! state assoc :connected false)))

                           (set! (.-onclose ws)
                                 (fn []
                                   (swap! state assoc :ws nil :stream nil :registry nil :connected false)
                                   (when-not (:manual-close @state)
                                     (let [attempt (inc (:reconnect-attempt @state))
                                           delay (backoff attempt)]
                                       (when (and (< attempt max-reconnect-attempts)
                                                  (not (:reconnect-pending @state)))
                                         (swap! state assoc :reconnect-attempt attempt :reconnect-pending true)
                                         (js/console.info "[JSONRPC] Reconnecting in" delay "ms")
                                         (js/setTimeout (fn []
                                                          (swap! state assoc :reconnect-pending false)
                                                          (connect!))
                                                        delay))))))
                           ws)))))
        wait-for-open! (fn []
                         (go
                           (loop [elapsed 0]
                             (let [current (:ws @state)]
                               (cond
                                 (and current (= 1 (.-readyState current)))
                                 current

                                 (and (not (:manual-close @state)) (nil? current))
                                 (do
                                   (connect!)
                                   (<! (a/timeout 100))
                                   (recur (+ elapsed 100)))

                                 (and (not (:manual-close @state))
                                      (some? current)
                                      (not= 1 (.-readyState current)))
                                 (do
                                   (when (> elapsed 5000)
                                     (js/console.warn "[JSONRPC] Timed out waiting for WebSocket to open"))
                                   (<! (a/timeout 100))
                                   (recur (+ elapsed 100)))

                                 :else
                                 nil)))))
        send-fn (fn [method params]
                  (let [result-chan (chan)
                        id (next-id)
                        response-chan (chan)
                        msg {:jsonrpc "2.0"
                             :id id
                             :method method
                             :params params}]
                    (go
                      (try
                        (let [ws (<! (wait-for-open!))]
                          (if (and ws (= 1 (.-readyState ws)))
                            (do
                              (when-let [registry (:registry @state)]
                                (register-request registry id response-chan))
                              (>! (:out (:stream @state)) (serialize-message msg))
                              (let [response (a/alt! response-chan ([val] val)
                                                     (a/timeout 30000) :timed-out)]
                                (if (= :timed-out response)
                                  (do
                                    (when-let [registry (:registry @state)]
                                      (get-response-chan registry id))
                                    (>! result-chan {:error true :message "Timed out waiting for response"}))
                                  (>! result-chan
                                      (if (:error response)
                                        {:error true :message (str (:message (:error response)))}
                                        {:error false :result (:result response)})))))
                            (>! result-chan {:error true :message "WebSocket is closed"})))
                        (catch js/Error e
                          (js/console.error "[JSONRPC] Send failed:" e)
                          (>! result-chan {:error true :message (.-message e)}))))
                    result-chan))
        notify-fn (fn [method params]
                    (go
                      (try
                        (let [ws (<! (wait-for-open!))]
                          (when (and ws (= 1 (.-readyState ws)))
                            (let [stream (:stream @state)
                                  msg {:jsonrpc "2.0"
                                       :method method
                                       :params params}]
                              (>! (:out stream) (serialize-message msg)))))
                        (catch js/Error e
                          (js/console.error "[JSONRPC] Notify failed:" e)))))
        disconnect-fn (fn []
                        (swap! state assoc :manual-close true :connected false)
                        (when-let [ws (:ws @state)]
                          (.close ws)))
        connection (fn []
                     #js {:send send-fn
                          :notify notify-fn
                          :disconnect disconnect-fn
                          :stream (fn [] (:stream @state))
                          :registry (fn [] (:registry @state))
                          :state state})]
    (connect!)
    (connection)))
