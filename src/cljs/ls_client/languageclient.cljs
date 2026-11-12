(ns ls-client.languageclient
  "Language Server Client for Metafacture Playground.
   
   This module provides the main entry point for connecting a Monaco editor
   to a Language Server over WebSocket using the LSP protocol.
   
   Usage:
     (let [client (<! (connect-language-client monaco-ns editor-instance))]
       ;; client is now connected and ready
       ;; Use (.disconnect client) to close)
   
   No external libraries required - uses only native ClojureScript async patterns."
  (:require [cljs.core.async :as a :refer [go <!]]
            [ls-client.client :as lsp-client]))

;; ============================================================================
;; Configuration
;; ============================================================================

(def ^:const root-uri "file:///metafacture-playground/flux")

(defonce ^:private active-clients (atom {}))

(defn- disconnect-client-when-ready [client-ch]
  (go
    (when-let [client (<! client-ch)]
      (when-let [ws-connection (:ws-connection client)]
        (.disconnect ws-connection)))))

;; ============================================================================
;; Public API
;; ============================================================================

(defn connect-language-client
  "Connect a Monaco editor to a Language Server.
   
   This function:
   1. Establishes WebSocket connection to the language server
   2. Sends LSP initialize request
   3. Sets up Monaco editor integration (completions, hover, diagnostics)
   4. Opens the current document in the language server
   
   Args:
   - monaco: Monaco namespace (window.monaco)
   - editor: Monaco editor instance
   - ws-url: (optional) WebSocket URL, defaults to ws://localhost:8080/ls
   
   Returns:
   - Promise that resolves to client object with properties:
     - :ws-connection - JSON-RPC connection
     - :editor-state - Editor state tracking
     - :initialized - Boolean flag
     - Methods:
       - (.disconnect client) - Close connection
       - Internally handles: completions, hover, definition, diagnostics
   
   Example:
     (go
       (let [client (<! (connect-language-client window.monaco editor-instance))]
         (println \"Connected!\")
         ;; Editor now has language server features
         ))
   
   Error handling:
   - If connection fails, the promise rejects with an error
   - Logs all operations to browser console with [Client] prefix"
  
  ([monaco ^js editor ws-url lang-id]
   (let [key [ws-url lang-id]]
     (let [existing (get @active-clients key)]
       (if (and (map? existing)
                (identical? editor (:editor existing)))
         (:client-ch existing)
         (let [client-ch (go
                           (try
                             (js/console.log "[LanguageClient] Connecting to language server at" ws-url "for language" lang-id)
                             (lsp-client/connect ws-url monaco editor lang-id root-uri)
                             (catch js/Error e
                               (js/console.error "[LanguageClient] Connection failed:" e)
                               (throw e))))
               ^js editor-dispose
               (.onDidDispose editor
                              (fn []
                                (swap! active-clients
                                       (fn [clients]
                                         (if (identical? editor (get-in clients [key :editor]))
                                           (dissoc clients key)
                                           clients)))
                                (disconnect-client-when-ready client-ch)))
               entry {:editor editor
                      :client-ch client-ch
                      :editor-dispose editor-dispose}]
           (swap! active-clients assoc key entry)
           (when existing
             (when-let [^js dispose (:editor-dispose existing)]
               (.dispose dispose))
             (disconnect-client-when-ready
              (if (map? existing) (:client-ch existing) existing)))
           client-ch))))))
