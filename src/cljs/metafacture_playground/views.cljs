(ns metafacture-playground.views
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as re-frame]
   [metafacture-playground.subs :as subs]
   [metafacture-playground.events :as events]
   [metafacture-playground.db :as db]
   [clojure.string :as clj-str]
   [lambdaisland.uri :refer [uri]]
   [cljsjs.semantic-ui-react]
   [goog.object :as g]
   [re-pressed.core :as rp]
   [clojure.pprint]
   ["@monaco-editor/react" :as monaco-react]))

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

(def button (component "Button"))
(def header (component "Header"))
(def container (component "Container"))
(def segment (component "Segment"))
(def grid (component "Grid"))
(def grid-column (component "Grid" "Column"))
(def form (component "Form"))
(def form-field (component "Form" "Field"))
(def loader (component "Loader"))
(def textarea (component "Form" "TextArea"))
(def label (component "Label"))
(def icon (component "Icon"))
(def divider (component "Divider"))
(def image (component "Image"))
(def input (component "Input"))
(def popup (component "Popup"))
(def message (component "Message"))

;;; Using monaco editor react component

(def monaco-editor (g/get monaco-react "default"))

;;; Color and Theming

(def color "blue")
(def basic-buttons? true)

; Config of input fields

(def focused-editor "data")

(def data-config
  {:name "data"
   :width 16
   :language "text/plain"
   :height-divider 3})

(def fix-config
  {:name "fix"
   :width 8
   :language "text/plain"})

(def flux-config
  {:name "flux"
   :width 8
   :language "text/plain"})

(def result-config
  {:width 16})

;;; Utils

(defn title-label [name]
  [:> label
   {:id (str name "-label")
    :attached "top left"
    :size "large"
    :color color}
   (clj-str/capitalize name)])

(defn collapse-label [panel-path]
  (let [collapsed? (re-frame/subscribe [::subs/collapsed? panel-path])]
    [:> label
     {:attached "top right"
      :on-click #(re-frame/dispatch [::events/collapse-panel panel-path @collapsed?])}
     [:> icon
      {:name
       (if @collapsed?
         "chevron down"
         "chevron up")
       :style {:margin 0}
       :size "large"}]]))

(defn screenreader-label [name for]
  [:label {:style {:position "absolute"
                   :height "1px"
                   :width "1px"
                   :overflow "hidden"}
           :for for}
   (clj-str/capitalize name)])

(defn simple-button [{:keys [content dispatch-fn icon-name]}]
  [:> button
   (merge {:id (-> content (clj-str/replace " " "-") (str "-button"))
           :basic basic-buttons?
           :color color}
          (when dispatch-fn
            {:onClick #(re-frame/dispatch dispatch-fn)}))
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
  (let [data (re-frame/subscribe [::subs/field-value :data])
        flux (re-frame/subscribe [::subs/field-value :flux])
        fix (re-frame/subscribe [::subs/field-value :fix])]
    (re-frame/dispatch
     [::rp/set-keydown-rules {:event-keys [[[::events/process @data @flux @fix]
                                            [{:ctrlKey true
                                              :keyCode 13}]]]
                              :always-listen-keys [{:ctrlKey true
                                                    :keyCode 13}]}])))

;;; Page Header

(defn page-header []
  [:> header  {:as "h1"}
   [:> image {:alt "Metafacture Ant"
              :src "images/metafacture-logo.png"}]
   "Metafacture Playground"])

;;; Message Panel

(defn- message-content [content type]
  (case type
    :error (str "ERROR: " content)
    :warning (str "WARNING: " content)
    :info (str "INFO: " content)
    :success (str "SUCCESS: " content)
    content))

(defn- message-type [type]
  (case type
    :error {:error true}
    :warning {:warning true}
    :info {:info true}
    :success {:success true}
    nil))

(defn message-panel []
  (let [{:keys [content type]} @(re-frame/subscribe [::subs/message])]
    (when content
      [:> message (merge {:content (message-content content type)
                          :on-dismiss #(re-frame/dispatch [::events/dismiss-message])}
                         (message-type type))])))

;;; Control Panel

(defn process-button []
  (let [data (re-frame/subscribe [::subs/field-value :data])
        flux (re-frame/subscribe [::subs/field-value :flux])
        fix  (re-frame/subscribe [::subs/field-value :fix])]
    [:> popup
     {:content (reagent/as-element [:div
                                    "Shortcut: "
                                    [:> label {:size "tiny"} "Ctrl + Enter"]])
      :on "hover"
      :trigger (reagent/as-element (simple-button {:content "Process"
                                                   :dispatch-fn [::events/process @data @flux @fix]
                                                   :icon-name "play"}))
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
   [share-link :api-call "Result call"]
   [share-link :workflow "Workflow"]])

(defn share-button []
  (let [uri (-> js/window .-location .-href uri (assoc :query nil))
        data (re-frame/subscribe [::subs/field-value :data])
        flux (re-frame/subscribe [::subs/field-value :flux])
        fix (re-frame/subscribe [::subs/field-value :fix])]
    [:> popup
     {:children (reagent/as-element [share-links])
      :on "click"
      :position "bottom left"
      :wide "very"
      :trigger (reagent/as-element (simple-button {:content "Share"
                                                   :icon-name "share alternate"
                                                   :dispatch-fn [::events/generate-links uri @data @flux @fix]}))}]))

(defn control-panel []
  [:> segment {:raised true}
   [simple-button {:content "Load sample" :dispatch-fn [::events/load-sample db/sample-fields] :icon-name "code"}]
   [simple-button {:content "Clear all" :dispatch-fn [::events/clear-all] :icon-name "erase"}]
   [process-button]
   [share-button]])

;;; Input fields

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
                                                                       @(re-frame/subscribe [::subs/field-value :data])
                                                                       @(re-frame/subscribe [::subs/field-value :flux])
                                                                       @(re-frame/subscribe [::subs/field-value :fix])])
                                            :keybindings [(bit-or control-command enter)
                                                          (chord-fn (bit-or control-command enter))]}))))

(defn set-up-editor [focus-on-load editor monaco]
  (set-end-of-line editor)
  (add-keydown-rules monaco editor)
  (when focus-on-load (js-invoke editor "focus")))

(defn editor [{:keys [name language height-divider]}]
  (let [editor-name (keyword name)
        value (re-frame/subscribe [::subs/field-value editor-name])
        height (re-frame/subscribe [::subs/editor-height editor-name 5 (font-size) height-divider])]
    [screenreader-label name (str name "-editor")]
    [:> monaco-editor
     {:className (str name "-editor")
      :value (or @value "")
      :on-mount (partial set-up-editor (= name focused-editor))
      :language language
      :height @height
      :theme "light"
      :options {:dragAndDrop true
                :minimap {:enabled false}}
      :on-change #(re-frame/dispatch-sync [::events/edit-input-value (keyword name) %])}]))

(defn editor-panel [config]
  (let [editor-name (-> config :name keyword)
        path [:input-fields editor-name]
        collapsed? (re-frame/subscribe [::subs/collapsed? path])
        width (re-frame/subscribe [::subs/editor-width editor-name])]
    (println editor-name ": width " @width)
    [:> grid-column {:width (or @width (:width config))}
     [:> segment {:raised true}
      [title-label (:name config)]
      [collapse-label path]
      [:> divider {:style {:margin "1.5em 0 0.5em 0"}}]
      (when-not @collapsed?
        [editor config])]]))

;;; Result field

(defn result []
  (let [content (re-frame/subscribe [::subs/process-result])
        loading? (re-frame/subscribe [::subs/result-loading?])
        collapsed? (re-frame/subscribe [::subs/collapsed? [:result]])]
    (when-not @collapsed?
      (if @loading?
        [:> segment {:basic true}
         [:> loader {:active true
                     :style {:padding "1.5em"}}]]
        [:> form
         [screenreader-label "Result" "result-panel"]
         [:> textarea {:id "result-panel"
                       :placeholder "No result"
                       :value (or @content "")
                       :rows (count (clj-str/split-lines @content))
                       :fluid "true"
                       :style {:border "none"}
                       :readOnly true}]]))))

(defn result-panel [width]
  [:> grid-column {:width width}
   [:> segment {:raised true}
    [title-label "Result"]
    [collapse-label [:result]]
    [:> divider {:style {:margin "1.5em 0"}}]
    [result]]])

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

     [editor-panel data-config]

     [editor-panel flux-config]

     [editor-panel fix-config]

     [result-panel (:width result-config)]]]])
