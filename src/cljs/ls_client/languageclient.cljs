(ns ls-client.languageclient
  (:require
   ["vscode-ws-jsonrpc" :as vscode-jsonrpc]
   ["vscode-languageclient" :as vscode-lc]
   ["monaco-languageclient" :as mc]))


(def ls-ws-url "ws://localhost:8080/ls")

(def lang-id "flux")


(defn connect-language-client []
  (js/Promise.
   (fn [resolve reject]
     (try
       (let [ws (js/WebSocket. ls-ws-url)]

         (set! (.-onopen ws)
               (fn []
                 (js/console.log "LS WebSocket connection Open")
                 (try
                   ;; Convert WebSocket to a language server socket
                   (let [socket (vscode-jsonrpc/toSocket ws)

                         ;; Create message reader/writer
                         reader (new (vscode-jsonrpc/WebSocketMessageReader) socket)
                         writer (new (vscode-jsonrpc/WebSocketMessageWriter) socket)

                         ;; Create and configure the Monaco language client
                         language-client (new mc/MonacoLanguageClient
                                              #js {:name (str lang-id " Language Client")
                                                   :clientOptions #js {:documentSelector #js [lang-id]
                                                                       :errorHandler #js {:error (fn [] #js {:action (vscode-lc/ErrorAction.Continue)})
                                                                                          :closed (fn [] #js {:action (vscode-lc/CloseAction.DoNotRestart)})}}
                                                   :connectionProvider #js {:get (fn []
                                                                                   (js/Promise.resolve
                                                                                    #js {:reader reader
                                                                                         :writer writer}))}})]

                     ;; Start the language client
                     (.start language-client)

                     ;; Resolve with the client instance
                     (resolve language-client))

                   (catch js/Error e
                     (js/console.error "Error connecting to language server:" e)
                     (reject e)))))

         (set! (.-onerror ws)
               (fn [error]
                 (js/console.error "LS WebSocket connection error:" error)
                 (reject error))))))))