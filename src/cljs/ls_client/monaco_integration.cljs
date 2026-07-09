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

;; ============================================================================
;; Editor Event Listeners
;; ============================================================================

(defn on-content-change
  "Register handler for editor content changes."
  [^js editor callback]
  (let [model (.getModel editor)
        disposable (.onDidChangeModelContent editor
                     (fn [event]
                       (let [full-text (.getValue model)
                             version (.getVersionId model)]
                         (callback event full-text version))))]
    #(.dispose disposable)))

;; ============================================================================
;; Applying Server Results
;; ============================================================================

(defn apply-diagnostics
  "Apply diagnostic markers from the language server."
  [^js monaco ^js editor diagnostics uri]
  (let [decorations 
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
         :documentation (get* "documentation" item)
         :sortText (get* "sortText" item)
         :filterText label
         :kind (get* "kind" item)}))

(defn register-completion-provider
  "Register LSP-based completion provider with Monaco."
  [^js monaco language-id completion-handler]
  (let [disposable (.. monaco -languages
                       (registerCompletionItemProvider
                        language-id
                        #js {:provideCompletionItems
                             (fn [^js model ^js position context]
                               (js/Promise.
                                (fn [resolve reject]
                                  (let [;; Extract partial word including hyphens and other valid identifier chars
                                        line-number (.-lineNumber position)
                                        column (dec (.-column position))
                                        line-content (.getLineContent model line-number)
                                        ;; Find the start of the word by looking backwards, including hyphens and underscores
                                        word-start (loop [i (- column 1)]
                                                     (if (< i 0)
                                                       0
                                                       (let [char (.charAt line-content i)]
                                                         (if (re-matches #"[a-zA-Z0-9_\-]" char)
                                                           (recur (- i 1))
                                                           (+ i 1)))))
                                        partial-word (if (< word-start column)
                                                       (.substring line-content word-start (- column 1))
                                                       "")
                                        result-promise (completion-handler position partial-word)]
                                    (.then result-promise
                                           (fn [completions]
                                             (if (or (nil? completions) (empty? completions))
                                               (do
                                                 (js/console.log "[Monaco] No completions, returning empty array")
                                                 (resolve #js {:suggestions #js []}))
                                               (let [suggestions (->> completions
                                                                      (filter #(begins-with-word? partial-word %))
                                                                      (mapv ->suggestion-item)
                                                                      (clj->js))]
                                                 (resolve #js {:suggestions suggestions}))))
                                           (fn [err]
                                             (js/console.error "[Monaco] Error in completion handler:" err)
                                             (reject err)))))))}))]
    #(.dispose disposable)))


(defn register-hover-provider
  "Register LSP-based hover provider with Monaco."
  [^js monaco language-id hover-handler]
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
  [^js monaco language-id definition-handler]
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
  [^js position]
  (let [line (max 0 (dec (.-lineNumber position)))
        character (max 0 (dec (.-column position)))]
    {:line line :character character}))

(defn lsp-position->monaco
  "Convert LSP position to Monaco position."
  [^js monaco position line-height]
  (.. monaco -Position (new (inc (:line position)) (inc (:character position)))))

(defn lsp-range->monaco
  "Convert LSP range to Monaco range."
  [^js monaco range]
  (let [start (:start range)
        end (:end range)]
    (.. monaco -Range (new (inc (:line start)) (inc (:character start))
                           (inc (:line end)) (inc (:character end))))))