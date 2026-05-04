(ns ls-client.monaco-integration
  "Integration between Monaco editor and LSP client.
   Handles editor events, applies server responses, manages decorations.")

;; ============================================================================
;; Editor State Management
;; ============================================================================

(defn create-editor-state
  "Create state tracking for an editor instance."
  [monaco-instance editor-instance]
  {:monaco monaco-instance
   :editor editor-instance
   :uri nil
   :version 0
   :decorations []})

(defn set-editor-uri
  "Update the document URI for this editor."
  [state uri]
  (assoc state :uri uri))

;; ============================================================================
;; Editor Event Listeners
;; ============================================================================

(defn on-content-change
  "Register handler for editor content changes."
  [editor callback]
  (let [model (.getModel editor)
        disposable (.onDidChangeModelContent editor
                     (fn [event]
                       (let [full-text (.getValue model)
                             version (.getVersionId model)]
                         (callback event full-text version))))]
    #(.dispose disposable)))

(defn on-cursor-move
  "Register handler for cursor movement."
  [editor callback]
  (let [disposable (.onDidChangeCursorPosition editor
                     (fn [event]
                       (callback event (-> event .-position))))]
    #(.dispose disposable)))

(defn on-selection-change
  "Register handler for selection changes."
  [editor callback]
  (let [disposable (.onDidChangeModelContent editor
                     (fn [event]
                       (callback event (-> editor .getSelections))))]
    #(.dispose disposable)))

;; ============================================================================
;; Applying Server Results
;; ============================================================================

(defn apply-diagnostics
  "Apply diagnostic markers from the language server."
  [monaco editor diagnostics uri]
  (let [model (.getModel editor)
        decorations 
        (map (fn [diag]
               (let [start-line (-> diag :range :start :line inc)
                     start-col (-> diag :range :start :character inc)
                     end-line (-> diag :range :end :line inc)
                     end-col (-> diag :range :end :character inc)
                     severity (get {1 "Error" 2 "Warning" 3 "Information" 4 "Hint"}
                                   (:severity diag) "Error")]
                 {:range (.. monaco -Range (new start-line start-col end-line end-col))
                  :options {:inlineClassName (str "error-" severity)
                            :glyphMarginClassName "diagnostic-glyph"
                            :glyphMarginHoverMessage #js {:value (:message diag)}
                            :minimap {:color "#FF0000"
                                      :position 2}}}))
             diagnostics)]
    (.setDecorations editor "errors" (clj->js decorations))))

(defn clear-diagnostics
  "Clear all diagnostic decorations."
  [editor]
  (.setDecorations editor "errors" #js []))

(defn show-hover
  "Show hover tooltip at cursor position."
  [editor hover-response line character]
  (when hover-response
    (let [contents (:contents hover-response)]
      (js/console.log "[Monaco] Showing hover:" contents))))

(defn show-completion
  "Trigger completion suggestions."
  [editor completions]
  (when (> (count completions) 0)
    (js/console.log "[Monaco] Showing" (count completions) "completions")))

(defn show-definition
  "Navigate to or show definition location."
  [editor definition]
  (let [location (if (map? definition) 
                   definition 
                   (first (if (vector? definition) definition [definition])))]
    (when location
      (js/console.log "[Monaco] Definition at:" location))))

(defn apply-workspace-edit
  "Apply a workspace edit from the language server."
  [monaco editor edit]
  (let [changes (:changes edit {})
        model (.getModel editor)]
    (doseq [[uri edits] changes]
      (if (= uri (:uri (meta editor)))
        (let [operations
              (map (fn [edit-item]
                     (let [start-line (-> edit-item :range :start :line inc)
                           start-col (-> edit-item :range :start :character inc)
                           end-line (-> edit-item :range :end :line inc)
                           end-col (-> edit-item :range :end :character inc)
                           new-text (:newText edit-item)]
                       {:range (.. monaco -Range (new start-line start-col end-line end-col))
                        :text new-text}))
                   edits)]
          (.executeEdits model "lsp-edit" (clj->js operations)))
        (js/console.log "[Monaco] Skipping edit in" uri "- opening other files not implemented")))))

;; ============================================================================
;; Completion Popup Integration
;; ============================================================================

(defn- get* [string item]
  (or ((keyword string) item) (get item string) ""))

(defn- begins-with-word? [current-word item]
  (let [label (get* "label" item)]
    (or (empty? current-word)
        (.startsWith (.toLowerCase label) (.toLowerCase current-word)))))

(defn- ->suggestion-item [item]
  (let [label (get* "label" item)
        detail (get* "detail" item)]
    #js {:label label
         :insertText (get* "insertText" item)
         :detail detail
         :documentation detail
         :sortText (get* "sortText" item)
         :filterText label
         :kind (get* "kind" item)}))

(defn register-completion-provider
  "Register LSP-based completion provider with Monaco."
  [monaco language-id completion-handler]
  (let [disposable (.. monaco -languages
                       (registerCompletionItemProvider
                        language-id
                        #js {:provideCompletionItems
                             (fn [model position context]
                               (js/Promise.
                                (fn [resolve reject]
                                  (let [word-info (.getWordAtPosition model position)
                                        partial-word (if word-info (.-word word-info) "")
                                        result-promise (completion-handler position partial-word)]
                                    (.then result-promise
                                           (fn [completions]
                                             (if (or (nil? completions) (empty? completions))
                                               (do
                                                 (js/console.log "[Monaco] No completions, returning empty array")
                                                 (resolve #js {:suggestions #js []}))
                                               (let [word-info (.getWordAtPosition model position)
                                                     current-word (if word-info (.-word word-info) "")
                                                     suggestions (->> completions
                                                                      (filter #(begins-with-word? current-word %))
                                                                      (mapv ->suggestion-item)
                                                                      (clj->js))] 
                                                 (resolve #js {:suggestions suggestions}))))
                                           (fn [err]
                                             (js/console.error "[Monaco] Error in completion handler:" err)
                                             (reject err)))))))}))]
    #(.dispose disposable)))


(defn register-hover-provider
  "Register LSP-based hover provider with Monaco."
  [monaco language-id hover-handler]
  (let [disposable (.. monaco -languages
                       (registerHoverProvider
                        language-id
                        #js {:provideHover
                             (fn [model position context]
                               (js/Promise.
                                (fn [resolve reject]
                                  (let [result-promise (hover-handler position)]
                                    (.then result-promise
                                           (fn [hover-info]
                                             (if hover-info
                                               (resolve #js {:contents #js {:value (or (:contents hover-info) "")}})
                                               (resolve nil)))
                                           (fn [err]
                                             (reject err)))))))}))]
    #(.dispose disposable)))

(defn- ->start-line [input]
  (-> input :range :start :line))

(defn- ->end-line [input]
  (-> input :range :end :line))

(defn- ->character [input]
  (-> input :range :start :character))

(defn- ->end-character [input]
  (-> input :range :end :character))

(defn- ->range [input]
  {:range {:start {:line (->start-line input) :character (->character input)}
           :end {:line (->end-line input) :character (->end-character input)}}})

(defn register-definition-provider
  "Register LSP-based definition provider with Monaco."
  [monaco language-id definition-handler]
  (let [disposable (.. monaco -languages
                       (registerDefinitionProvider
                        language-id
                        #js {:provideDefinition
                             (fn [model position context]
                               (js/Promise.
                                (fn [resolve reject]
                                  (let [result-promise (definition-handler position)]
                                    (.then result-promise
                                           (fn [definition]
                                             (if definition
                                               (if (vector? definition)
                                                 (resolve (clj->js (map (fn [loc]
                                                                          {:uri (:uri loc)
                                                                           :range (->range loc)})
                                                                        definition)))
                                                 (resolve (clj->js {:uri (:uri definition)
                                                                    :range (->range definition)})))
                                               (resolve nil)))
                                           (fn [err]
                                             (reject err)))))))}))]
    #(.dispose disposable)))

;; ============================================================================
;; Conversion Utilities
;; ============================================================================

(defn monaco-position->lsp
  "Convert Monaco position to LSP position."
  [position]
  (let [line (max 0 (dec (.-lineNumber position)))
        character (max 0 (dec (.-column position)))]
    {:line line :character character}))

(defn lsp-position->monaco
  "Convert LSP position to Monaco position."
  [monaco position line-height]
  (.. monaco -Position (new (inc (:line position)) (inc (:character position)))))

(defn lsp-range->monaco
  "Convert LSP range to Monaco range."
  [monaco range]
  (let [start (:start range)
        end (:end range)]
    (.. monaco -Range (new (inc (:line start)) (inc (:character start))
                           (inc (:line end)) (inc (:character end))))))