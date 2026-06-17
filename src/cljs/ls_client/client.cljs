(ns ls-client.client
  "Main Language Server Client for Monaco editor.
   Orchestrates JSON-RPC communication, LSP protocol, and Monaco integration."
  (:require [cljs.core.async :as a :refer [<! go]]
            [ls-client.jsonrpc :as jsonrpc]
            [ls-client.lsp :as lsp]
            [ls-client.monaco-integration :as monaco-int]))

;; ============================================================================
;; Utilities
;; ============================================================================

(defn ^:private channel->promise
  "Convert a core.async channel to a JavaScript Promise."
  [ch]
  (js/Promise.
   (fn [resolve reject]
     (go
       (try
         (let [result (<! ch)]
           (resolve result))
         (catch js/Error e
           (reject e)))))))

;; ============================================================================
;; Client State
;; ============================================================================

(defn create-client-state
  "Create the main client state object."
  [ws-connection lang-id editor-state]
  {:ws-connection ws-connection
   :lang-id lang-id
   :editor-state editor-state
   :initialized false
   :doc-versions (lsp/create-document-version-tracker)
   :disposables []
   :supported-features #{}})

;; ============================================================================
;; Client Lifecycle
;; ============================================================================

(defn initialize-server
  "Send initialize request to the language server."
  [client root-uri]
  (go
    (try
      (let [ws (:ws-connection client)
            init-params (lsp/initialize-message root-uri (:lang-id client))
            response-ch (.send ws "initialize" (clj->js init-params))
            response (<! response-ch)]
        (if (:error response)
          (throw (js/Error. (str "Initialize failed: " (:message response))))
          (assoc client :initialized true)))
      (catch js/Error e
        (js/console.error "[Client] Initialization failed:" e)
        (throw e)))))

(defn open-document
  "Open a document in the language server."
  [client uri text]
  (go
    (try
      (when (:initialized client)
        (lsp/init-document-version (:doc-versions client) uri)
        (.notify (:ws-connection client) 
                 "textDocument/didOpen"
                 (clj->js (lsp/did-open-message uri (:lang-id client) 1 text))))
      (catch js/Error e
        (js/console.error "[Client] Failed to open document:" e)
        (throw e)))))

(defn change-document
  "Notify the server of document changes (full text sync)."
  [client uri text]
  (go
    (try
      (when (:initialized client)
        (let [version (lsp/increment-document-version (:doc-versions client) uri)]
          (.notify (:ws-connection client)
                   "textDocument/didChange"
                   (clj->js (lsp/did-change-full-message uri version text)))))
      (catch js/Error e
        (js/console.error "[Client] Failed to update document:" e)
        (throw e)))))

;; ============================================================================
;; LSP Features
;; ============================================================================

(defn request-hover
  "Request hover information for a position."
  [client uri line character]
  (go
    (try
      (let [ws (:ws-connection client)
            hover-params (lsp/hover-message uri line character)
            response-ch (.send ws "textDocument/hover" (clj->js hover-params))
            response (<! response-ch)]
        (if (:error response)
          nil
          (lsp/parse-hover-response (:result response))))
      (catch js/Error e
        (js/console.log "[Client] Hover request failed:" e)
        nil))))

(defn request-completion
  "Request completions for a position."
  [client uri line character partial-word]
  (go
    (try
      (let [ws (:ws-connection client)
            completion-params (if (empty? partial-word)
                                (lsp/completion-message uri line character)
                                (lsp/completion-message uri line character partial-word 1))
            ;;_ (js/console.log "[Client] Completion params being sent:" (clj->js completion-params))
            response-ch (.send ws "textDocument/completion" (clj->js completion-params))
            response (<! response-ch)]
        ;;(js/console.log "[Client] Completion response:" (clj->js response))
        (if (:error response)
          (do
            (js/console.warn "[Client] Completion error:" (:message response))
            [])
          (let [parsed (lsp/parse-completion-response (:result response))
                ;; Enhance items with sortText and filterText for Monaco filtering
                enhanced (map (fn [item]
                               (let [label (or (:label item) "")
                                     sort-text (or (:sortText item) label)
                                     filter-text (or (:filterText item) label)]
                                 (assoc item
                                        :sortText sort-text
                                        :filterText filter-text)))
                             parsed)]
            enhanced)))
      (catch js/Error e
        (js/console.log "[Client] Completion request failed:" e)
        []))))

(defn request-definition
  "Request definition location for a symbol."
  [client uri line character]
  (go
    (try
      (let [ws (:ws-connection client)
            def-params (lsp/definition-message uri line character)
            response-ch (.send ws "textDocument/definition" (clj->js def-params))
            response (<! response-ch)]
        (when-not (:error response)
          (:result response)))
      (catch js/Error e
        (js/console.log "[Client] Definition request failed:" e)
        nil))))

;; ============================================================================
;; Monaco Integration
;; ============================================================================

(defn setup-editor-integration
  "Set up Monaco editor integration with language server."
  [client ^js monaco ^js editor uri]
  (go
    (try
      (let [;; Set up content change listener
            on-change-dispose
            (monaco-int/on-content-change
             editor
             (fn [event full-text version]
               (go (<! (change-document client uri full-text)))))
            
            ;; Register completion provider
            completion-dispose
            (monaco-int/register-completion-provider
             monaco
             (:lang-id client)
             (fn [position partial-word] 
               (let [lsp-pos (monaco-int/monaco-position->lsp position)
                     response-ch (request-completion client uri (:line lsp-pos) (:character lsp-pos) partial-word)]
                 ;;(js/console.log "[Client] Requested completion for:" lsp-pos "with partial-word:" partial-word)
                 (channel->promise response-ch))))
            
            ;; Register hover provider
            hover-dispose
            (monaco-int/register-hover-provider
             monaco
             (:lang-id client)
             (fn [position]
               (let [lsp-pos (monaco-int/monaco-position->lsp position)
                     response-ch (request-hover client uri (:line lsp-pos) (:character lsp-pos))]
                 (channel->promise response-ch))))
            
            ;; Register definition provider
            def-dispose
            (monaco-int/register-definition-provider
             monaco
             (:lang-id client)
             (fn [position]
               (let [lsp-pos (monaco-int/monaco-position->lsp position)
                     response-ch (request-definition client uri (:line lsp-pos) (:character lsp-pos))]
                 (channel->promise response-ch))))
            
            disposables [on-change-dispose completion-dispose hover-dispose def-dispose]]
        
        (update client :disposables concat disposables))
      (catch js/Error e
        (js/console.error "[Client] Failed to set up editor integration:" e)
        (js/console.error "[Client] Stack:" (.-stack e))
        (throw e)))))
;; ============================================================================
;; Server Notifications (Push from Server)
;; ============================================================================

(defn setup-notification-handlers
  "Set up handlers for server-initiated notifications."
  [client ^js monaco ^js editor]
  (fn [notification]
    (let [method (:method notification)
          params (:params notification)]
      (case method
        "textDocument/publishDiagnostics"
        (let [diag-info (lsp/parse-diagnostics params)]
          (monaco-int/apply-diagnostics monaco editor (:diagnostics diag-info) (:uri diag-info)))
        
        "window/logMessage"
        (js/console.log "[Server]" (:message params))
        
        "window/showMessage"
        (js/console.log "[Server Alert]" (:message params))
        
        (js/console.log "[Server Notification]" method params)))))

;; ============================================================================
;; Public API - Connect and Initialize
;; ============================================================================

(defn connect
  "Connect to a language server and set up Monaco integration."
  [ws-url ^js monaco ^js editor language-id root-uri]
  (go
    (try
      ;; Register the language with Monaco if not already registered
      (.. monaco -languages (register #js {:id language-id}))
      
      (let [;; Create JSON-RPC connection
            ws-connection (<! (jsonrpc/connect
                               ws-url
                               {:on-notification
                                (fn [notif]
                                  (js/console.log "[JSONRPC Notification]" (:method notif)))}))]
        
        ;; Check if connection failed
        (if (and (map? ws-connection) (:error ws-connection))
          ;; Connection failed - return error
          (do
            (js/console.warn "✗ Language server unavailable")
            ws-connection)
          
          ;; Connection succeeded - continue with setup
          (let [;; Create client state
                editor-state (monaco-int/create-editor-state monaco editor)
                client (create-client-state ws-connection language-id editor-state)
                ;; Initialize server
                client (<! (initialize-server client root-uri))
                ;; Set up notification handlers
                notification-handler (setup-notification-handlers client monaco editor)
                ;; Set up editor integration
                client (<! (setup-editor-integration client monaco editor root-uri))
                ;; Open the initial document
                model (.getModel editor)
                text (.getValue model)]
            ;; Update the WS connection's notification handler (it's a JS object, not a ClojureScript map)
            (aset ws-connection "on-notification" notification-handler)
            (<! (open-document client root-uri text))
            client)))
        (catch js/Error e
          (js/console.error "[Client] Failed to connect:" e)
          (throw e)))))