(ns metafacture-playground.views
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as re-frame]
   [metafacture-playground.subs :as subs]
   [metafacture-playground.events :as events]
   [clojure.string :as clj-str]
   [lambdaisland.uri :refer [uri]]
   [cljsjs.semantic-ui-react]
   [goog.object :as g]
   [re-pressed.core :as rp]
   ["@monaco-editor/react" :as monaco-react]
   [metafacture-playground.utils :as utils]))

;;; Using semantic ui react components

(def semantic-ui js/semanticUIReact)

(defn component
  "Get a component from sematic-ui-react:

    (component \"Button\")
    (component \"Menu\" \"Item\")"
  [k & ks]
  (if (seq ks)
    (apply g/getValueByKeys semantic-ui k ks)
    (goog.object/get semantic-ui k)))

(def button-group (component "Button" "Group"))
(def button (component "Button"))
(def header (component "Header"))
(def container (component "Container"))
(def segment (component "Segment"))
(def grid (component "Grid"))
(def grid-column (component "Grid" "Column"))
(def form (component "Form"))
(def form-field (component "Form" "Field"))
(def loader (component "Loader"))
(def label (component "Label"))
(def icon (component "Icon"))
(def image (component "Image"))
(def input (component "Input"))
(def popup (component "Popup"))
(def message (component "Message"))
(def menu (component "Menu"))
(def menu-item (component "Menu" "Item"))
(def dropdown (component "Dropdown"))
(def dropdown-menu (component "Dropdown" "Menu"))
(def dropdown-item (component "Dropdown" "Item"))

;;; Using monaco editor react component

(def monaco-editor (g/get monaco-react "default"))

;;; Color and Theming

(def color "blue")
(def basic-buttons? true)

; Config of input fields

(def focused-editor :data)

;;; Utils

(defn link-label [name href]
  [:> label {:content name
             :as "a"
             :href href
             :target "_blank"}])

(defn title-label [name]
  [:> menu-item
   {:id (str name "-label")
    :size "large"
    :color color
    :style {:font-weight "bold"}
    :active true}
   name])

(defn collapse-label [editor]
  (let [collapsed? (re-frame/subscribe [::subs/collapsed? editor])]
    [:> menu-item
     {:on-click #(re-frame/dispatch [::events/collapse-panel editor @collapsed?])
      :icon (if @collapsed? "chevron down" "chevron up")
      :active true
      :position "right"
      :size "large"}]))

(defn screenreader-label [name for]
  [:label {:style {:position "absolute"
                   :height "1px"
                   :width "1px"
                   :overflow "hidden"}
           :for for}
   (clj-str/capitalize name)])

(defn simple-button [{:keys [content dispatch-fns icon-name fluid style as htmlFor]}]
  [:> button
   (merge {:id (-> content (clj-str/replace " " "-") (str "-button"))
           :basic basic-buttons?
           :color color
           :fluid fluid}
          (when dispatch-fns
            {:onClick #(doseq [fn dispatch-fns]
                         (re-frame/dispatch fn))})
          (when style
            {:style style})
          (when as
            {:as as})
          (when htmlFor
            {:htmlFor htmlFor}))
   (when icon-name
     [:> icon {:name icon-name}])
   content])

(defn- font-size []
  (-> js/window
      (.getComputedStyle (-> js/document (.getElementById "app")))
      (.getPropertyValue "font-size")
      (clj-str/replace "px" "")
      js/parseFloat))

;;; Register keydown rules

(defn register-keydown-rules []
  (let [data (re-frame/subscribe [::subs/editor-content :data])
        flux (re-frame/subscribe [::subs/editor-content :flux])
        transformation (re-frame/subscribe [::subs/editor-content :transformation])]
    (re-frame/dispatch
     [::rp/set-keydown-rules {:event-keys [[[::events/process @data @flux @transformation]
                                            [{:ctrlKey true
                                              :keyCode 13}]]]
                              :always-listen-keys [{:ctrlKey true
                                                    :keyCode 13}]}])))

;;; Page Header

(defn page-header []
  [:> header  {:as "h1"}
   [:> image {:alt "Metafacture Ant"
              :src "images/metafacture-logo.png"}]
   "Metafacture Playground"
    (let [backend-versions (re-frame/subscribe [::subs/backend-versions])]
      (for [[version-name {:keys [version-label link]}] @backend-versions]
        ^{:key version-name}
        [link-label (str (name version-name) " " version-label) link]))])

;;; Message Panel

(defn message-panel []
  (let [{:keys [content details type]} @(re-frame/subscribe [::subs/message])
        show-details? (re-frame/subscribe [::subs/error-details-visible?])]
    (when content
      [:> segment
       [:> message (merge {:header (-> type name clj-str/capitalize)
                           :on-dismiss #(re-frame/dispatch [::events/dismiss-message])
                           type true}
                          (if (sequential? content)
                            {:list content}
                            {:content content}))]
       (when details
         (if @show-details?
           [:> message {:header "Details"
                        :content details
                        :on-dismiss #(re-frame/dispatch [::events/show-error-details false])
                        type true
                        :style {:white-space "pre-wrap"}}]
           [:> button {:fluid true
                       :style {:margin 0}
                       :basic true
                       :on-click #(re-frame/dispatch [::events/show-error-details true])}
            "Show Details"]))])))

;;; Control Panel

(defn is-group? [entry]
  (try
    (when (or (-> entry val :data)
              (-> entry val :flux)
              (-> entry val :transformation)) false)
    (catch :default _
      true)))

(defn dropdown-entries [entries]
  [:> dropdown-menu
   (doall
    (for [entry entries]
      (if (is-group? entry)
        (let [dropdown-open? (re-frame/subscribe [::subs/dropdown-open? (key entry)])]
          ^{:key (key entry)}
           [:> dropdown {:text (key entry)
                         :open @dropdown-open?
                         :item true
                         :on-click #(re-frame/dispatch [::events/open-dropdown (key entry) (not @dropdown-open?)])}
            (dropdown-entries (val entry))])
        (let [[k _] entry
              display-name (utils/display-name k)
              dropdown-value (re-frame/subscribe [::subs/dropdown-active-item])]
          ^{:key k}
          [:> dropdown-item
           {:key k
            :text display-name
            :value display-name
            :active (= display-name @dropdown-value)
            :selected (= display-name @dropdown-value)
            :onClick #(re-frame/dispatch [::events/load-example display-name])}]))))])

(defn examples-dropdown []
  [:> button-group {:color color
                    :basic true}
   (let [dropdown-value (re-frame/subscribe [::subs/dropdown-active-item])
         dropdown-open? (re-frame/subscribe [::subs/dropdown-open? "main"])]
     [:> dropdown
      {:className "icon"
       :button true
       :labeled true
       :text (or @dropdown-value "Load Examples")
       :open @dropdown-open?
       :on-blur #(re-frame/dispatch [::events/open-dropdown "main" false])
       :on-click #(re-frame/dispatch [::events/open-dropdown "main" (not @dropdown-open?)])}
      (dropdown-entries @(re-frame/subscribe [::subs/examples]))])])

(defn process-button []
  (let [data (re-frame/subscribe [::subs/editor-content :data])
        flux (re-frame/subscribe [::subs/editor-content :flux])
        transformation  (re-frame/subscribe [::subs/editor-content :transformation])]
    [:> popup
     {:content (reagent/as-element [:div
                                    "Shortcut: "
                                    [:> label {:size "tiny"} "Ctrl + Enter"]])
      :on "hover"
      :trigger (reagent/as-element (simple-button {:content "Process"
                                                   :dispatch-fns [[::events/process @data @flux @transformation]]
                                                   :icon-name "play"
                                                   :style {:margin-left "0.1em"}}))
      :position "bottom left"}]))

(defn share-link [link-type label-text]
  (let [link (re-frame/subscribe [::subs/link link-type])]
    [:> form-field
     [:> label
      {:id (str link-type "link-label")
       :color color
       :content label-text}]
     [:> input
      {:id (str link-type "-link-input")
       :action {:color color
                :icon "copy"
                :on-click #(re-frame/dispatch [::events/copy-link @link])
                :alt "Copy link"
                :disabled (not @link)}
       :placeholder (if-not @link "Nothing to share..." "")
       :value (or @link "")
       :readOnly true}]]))

(defn share-links []
  [:> form
   [share-link :workflow "Workflow"]
   [share-link :api-call "Result call"]])

(defn share-button []
  (let [uri (-> js/window .-location .-href uri (assoc :query nil))
        data (re-frame/subscribe [::subs/editor-content :data])
        data-variable (re-frame/subscribe [::subs/file-variable :data])
        flux (re-frame/subscribe [::subs/editor-content :flux])
        transformation (re-frame/subscribe [::subs/editor-content :transformation])
        transformation-variable (re-frame/subscribe [::subs/file-variable :transformation])]
    [:> popup
     {:children (reagent/as-element [share-links])
      :on "click"
      :position "bottom left"
      :wide "very"
      :trigger (reagent/as-element (simple-button {:content "Share"
                                                   :icon-name "share alternate"
                                                   :dispatch-fns [[::events/generate-links
                                                                   uri
                                                                   {:data {:variable @data-variable
                                                                           :content @data}
                                                                    :flux {:content @flux}
                                                                    :transformation {:variable @transformation-variable
                                                                                     :content @transformation}}]]}))}]))

(defn control-panel []
  [:> segment {:raised true}
   [examples-dropdown]
   [simple-button {:content "Clear"
                   :dispatch-fns [[::events/edit-editor-content :data "" :other]
                                  [::events/edit-editor-content :flux "" :other]
                                  [::events/edit-editor-content :transformation "" :other]
                                  [::events/edit-editor-content :result "" :other]]
                   :icon-name "erase"
                   :style {:margin-left "0.3em"}}]
   [process-button]
   [share-button]
   [simple-button {:content "Import Workflow"
                   :icon-name "upload"
                   :as "label"
                   :htmlFor "files"}]
   [:> input {:type "file"
              :id "files"
              :name "files"
              :style {:display "none"}
              :multiple true
              :on-change #(re-frame/dispatch [::events/on-read-file-list (g/getValueByKeys % "target" "files")])}]
   [simple-button {:content "Export Workflow"
                   :dispatch-fns [[::events/export-workflow
                                   {:data {:content @(re-frame/subscribe [::subs/editor-content :data])
                                           :variable @(re-frame/subscribe [::subs/file-variable :data])}
                                    :flux {:content @(re-frame/subscribe [::subs/editor-content :flux])}
                                    :transformation {:content @(re-frame/subscribe [::subs/editor-content :transformation])
                                                     :variable @(re-frame/subscribe [::subs/file-variable :transformation])}}]]
                   :icon-name "download"}]])

;;; Editors

(defn set-end-of-line [editor]
  (let [lf 0]
    (-> (js-invoke editor "getModel")
        (js-invoke "setEOL" lf))))

(defn add-keydown-rules [monaco editor]
  (let [control-command (g/getValueByKeys monaco "KeyMod" "CtrlCmd")
        enter (g/getValueByKeys monaco "KeyCode" "Enter")
        chord-fn (g/getValueByKeys monaco "KeyMod" "chord")]
    (js-invoke editor "addAction" (clj->js {:id "process"
                                            :label "Process Workflow"
                                            :run  #(re-frame/dispatch [::events/process
                                                                       @(re-frame/subscribe [::subs/editor-content :data])
                                                                       @(re-frame/subscribe [::subs/editor-content :flux])
                                                                       @(re-frame/subscribe [::subs/editor-content :transformation])])
                                            :keybindings [(bit-or control-command enter)
                                                          (chord-fn (bit-or control-command enter))]}))))

(defn set-up-editor [focus-on-load editor monaco]
  (set-end-of-line editor)
  (add-keydown-rules monaco editor)
  (when focus-on-load (js-invoke editor "focus")))

(defn editor [editor-k]
  (let [value (re-frame/subscribe [::subs/editor-content editor-k])
        height (re-frame/subscribe [::subs/height editor-k 5 (font-size)])
        k (re-frame/subscribe [::subs/key-count editor-k])
        language (re-frame/subscribe [::subs/monaco-language editor-k])]
    [screenreader-label (str (name editor-k) "-editor")]
    [:> monaco-editor
     {:key @k
      :className (str (name editor-k) "-editor")
      :default-value (or @value "")
      :on-mount (partial set-up-editor (= editor-k focused-editor))
      :language @language
      :height @height
      :theme "light"
      :options {:dragAndDrop true
                :minimap {:enabled false}}
      :on-change #(re-frame/dispatch [::events/edit-editor-content editor-k %])}]))

(defn editor-panel [editor-k]
  (let [collapsed? (re-frame/subscribe [::subs/collapsed? editor-k])
        disabled? (re-frame/subscribe [::subs/disabled? editor-k])
        width (re-frame/subscribe [::subs/width editor-k])
        label (re-frame/subscribe [::subs/label editor-k])]
    [:> grid-column {:width @width}
     [:> segment {:raised true
                  :disabled @disabled?}
      [:> menu
       {:color color
        :stackable true}
       [title-label @label]
       [collapse-label editor-k]]
      (when-not @collapsed?
        [editor editor-k])]]))

;;; Result field

(defn result []
  (let [content (re-frame/subscribe [::subs/process-result])
        loading? (re-frame/subscribe [::subs/result-loading?])
        collapsed? (re-frame/subscribe [::subs/collapsed? :result])
        language (re-frame/subscribe [::subs/monaco-language :result])
        height (-> @content (clj-str/split #"\r?\n" -1) count (* 19))]
    (when-not @collapsed?
      (if @loading?
        [:> segment {:basic true}
         [:> loader {:active true
                     :style {:padding "1.5em"}}]]
        [:div
         [screenreader-label "result-editor"]
         [:> monaco-editor
          {:className "result-editor"
           :value (or @content "No Result")
           :language @language
           :height height
           :theme "light"
           :options {:minimap {:enabled false}
                     :readOnly true
                     :scrollBeyondLastLine false}}]]))))

(defn result-panel []
  (let [width (re-frame/subscribe [::subs/width :result])
        label (re-frame/subscribe [::subs/label :result])]
  [:> grid-column {:width @width}
   [:> segment {:raised true}
    [:> menu
     {:color color
      :stackable true}
     [title-label @label]
     [collapse-label [:result]]]
    [result]]]))

;;; Main panel

(defn main-panel []

  (register-keydown-rules) ;; Attention: keydown rules don't work in the monaco editors so they need to be defined in the editors again

  (.addEventListener js/window "resize"
                     #(re-frame/dispatch [::events/window-resize (.-innerHeight js/window)]))

  [:> container
   [:> segment

    [page-header]

    [message-panel]

    [control-panel]

    [:> grid {:stackable true}

     [editor-panel :data]

     [editor-panel :flux]

     [editor-panel :transformation]

     [result-panel]]]])
