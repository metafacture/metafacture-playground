(ns ls-client.lsp
  "Language Server Protocol (LSP) client implementation.
   Handles initialization, document events, and LSP message formatting.")

;; ============================================================================
;; LSP Message Builders
;; ============================================================================

(def default-client-capabilities
  "Minimal set of client capabilities."
  {:textDocument {:synchronization {:didSave true
                                    :didChange 1}
                 :completion {:completionItem {}}
                 :hover {}
                 :definition {}
                 :references {}
                 :codeAction {}}
   :workspace {:workspaceFolders true}})

(defn initialize-message
  "Build LSP initialize request."
  [root-uri lang-id]
  {:processId (if (and (exists? js/process) (.-pid js/process))
                (.-pid js/process)
                nil)
   :rootUri root-uri
   :initializationOptions {}
   :capabilities default-client-capabilities
   :trace "verbose"})

(defn did-open-message
  "Build textDocument/didOpen notification."
  [uri lang-id version text]
  {:textDocument {:uri uri
                  :languageId lang-id
                  :version version
                  :text text}})

(defn did-change-message
  "Build textDocument/didChange notification for incremental updates."
  [uri version changes]
  {:textDocument {:uri uri :version version}
   :contentChanges changes})

(defn did-change-full-message
  "Build textDocument/didChange notification for full document sync."
  [uri version text]
  {:textDocument {:uri uri :version version}
   :contentChanges [{:text text}]})

(defn did-save-message
  "Build textDocument/didSave notification."
  ([uri]
   {:textDocument {:uri uri}})
  ([uri text]
   {:textDocument {:uri uri}
    :text text}))

(defn did-close-message
  "Build textDocument/didClose notification."
  [uri]
  {:textDocument {:uri uri}})

(defn hover-message
  "Build textDocument/hover request."
  [uri line character]
  {:textDocument {:uri uri}
   :position {:line line :character character}})

(defn completion-message
  "Build textDocument/completion request."
  ([uri line character]
   {:textDocument {:uri uri}
    :position {:line line :character character}})
  ([uri line character context]
   {:textDocument {:uri uri}
    :position {:line line :character character}
    :context context})
  ([uri line character partial-word trigger-kind]
   {:textDocument {:uri uri}
    :position {:line line :character character}
    :context {:triggerKind trigger-kind
              :partialWord partial-word}}))

(defn definition-message
  "Build textDocument/definition request."
  [uri line character]
  {:textDocument {:uri uri}
   :position {:line line :character character}})

(defn code-action-message
  "Build textDocument/codeAction request."
  [uri range diagnostics]
  {:textDocument {:uri uri}
   :range range
   :context {:diagnostics diagnostics}})

(defn references-message
  "Build textDocument/references request."
  [uri line character include-declaration]
  {:textDocument {:uri uri}
   :position {:line line :character character}
   :context {:includeDeclaration include-declaration}})

(defn shutdown-message
  "Build shutdown request."
  []
  nil)

(defn exit-message
  "Build exit notification."
  []
  nil)

;; ============================================================================
;; LSP Client State Management
;; ============================================================================

(defn create-document-version-tracker
  "Create a tracker for document versions by URI."
  []
  (atom {}))

(defn init-document-version
  "Initialize version tracking for a document."
  [tracker uri]
  (swap! tracker assoc uri 0))

(defn get-document-version
  "Get the current version of a document."
  [tracker uri]
  (get @tracker uri 0))

(defn increment-document-version
  "Increment and return the new version of a document."
  [tracker uri]
  (let [new-version (inc (get @tracker uri 0))]
    (swap! tracker assoc uri new-version)
    new-version))

;; ============================================================================
;; Response Parsing Helpers
;; ============================================================================

(defn parse-diagnostics
  "Parse publish diagnostics notification."
  [params]
  {:uri (:uri params)
   :diagnostics (:diagnostics params [])})

(defn parse-hover-response
  "Parse textDocument/hover response."
  [response]
  (if (nil? response)
    nil
    {:contents (if (string? (:contents response))
                 (:contents response)
                 (:value (:contents response)))
     :range (:range response)}))

(defn parse-completion-response
  "Parse textDocument/completion response."
  [response]
  (if (map? response)
    (:items response [])
    response))

(defn parse-definition-response
  "Parse textDocument/definition response."
  [response]
  response)

(defn parse-references-response
  "Parse textDocument/references response."
  [response]
  (if (vector? response) response [response]))