(ns metafacture-playground.views
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as re-frame]
   [metafacture-playground.subs :as subs]
   [clojure.string :as clj-str]
   [lambdaisland.uri :refer [uri]]
   [cljsjs.semantic-ui-react]
   [goog.object]
   [re-pressed.core :as rp]))

;;; Using semantic ui react components

(def semantic-ui js/semanticUIReact)

(defn component
  "Get a component from sematic-ui-react:

    (component \"Button\")
    (component \"Menu\" \"Item\")"
  [k & ks]
  (if (seq ks)
    (apply goog.object/getValueByKeys semantic-ui k ks)
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

;;; Color and Theming

(def color "blue")
(def basic-buttons? true)

; Sizes of input fields

(def data-config
  {:name "data"
   :width 16
   :rows 5})

(def fix-config
  {:name "fix"
   :width 8
   :rows 15})

(def flux-config
  {:name "flux"
   :width 8
   :rows 15})

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
    (fn [_]
      [:> label
       {:attached "top right"
        :on-click #(re-frame/dispatch [:collapse-panel panel-path @collapsed?])}
       [:> icon
        {:name
         (if @collapsed?
           "chevron down"
           "chevron up")
         :style {:margin 0}
         :size "large"}]])))

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

;;; Register keydown rules

(defn register-keydown-rules []
  (let [data (re-frame/subscribe [::subs/field-value :data])
        flux (re-frame/subscribe [::subs/field-value :flux])
        fix (re-frame/subscribe [::subs/field-value :fix])]
    (re-frame/dispatch
     [::rp/set-keydown-rules {:event-keys [[[:process @data @flux @fix]
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

;;; Control Panel

(defn process-button []
  (let [data (re-frame/subscribe [::subs/field-value :data])
        flux (re-frame/subscribe [::subs/field-value :flux])
        fix  (re-frame/subscribe [::subs/field-value :fix])]
    (fn []
      [:> popup
       {:content (reagent/as-element [:div
                                      "Shortcut: "
                                      [:> label {:size "tiny"} "Ctrl + Enter"]])
        :on "hover"
        :trigger (reagent/as-element (simple-button {:content "Process"
                                                     :dispatch-fn [:process @data @flux @fix]
                                                     :icon-name "play"}))
        :position "bottom left"}])))

(defn share-link [link-type label-text]
  (let [link (re-frame/subscribe [::subs/link link-type])]
    (fn [link-type label-text]
      [:> form-field
       [:> label
        {:id (str link-type "link-label")
         :color color
         :content label-text}]
       [:> input
        {:id (str link-type "-link-input")
         :action {:color color
                  :icon "copy"
                  :on-click #(re-frame/dispatch [:copy-link @link])
                  :alt "Copy link"
                  :disabled (not @link)}
         :placeholder (if-not @link "Nothing to share..." "")
         :default-value (or @link "")
         :readOnly true}]])))

(defn share-links []
  [:> form
   [share-link :api-call "Result call"]
   [share-link :workflow "Workflow"]])

(defn share-button []
  (let [uri (-> js/window .-location .-href uri (assoc :query nil))
        data (re-frame/subscribe [::subs/field-value :data])
        flux (re-frame/subscribe [::subs/field-value :flux])
        fix (re-frame/subscribe [::subs/field-value :fix])]
    (fn []
      [:> popup
       {:children (reagent/as-element [share-links])
        :on "click"
        :position "bottom left"
        :wide "very"
        :trigger (reagent/as-element (simple-button {:content "Share" :icon-name "share alternate" :dispatch-fn [:generate-links uri @data @flux @fix]}))}])))

(defn control-panel []
  [:> segment {:raised true}
   [simple-button {:content "Load sample" :dispatch-fn [:load-sample] :icon-name "code"}]
   [simple-button {:content "Clear all" :dispatch-fn [:clear-all] :icon-name "erase"}]
   [process-button]
   [share-button]])

;;; Input fields

(defn editor [{:keys [name rows]}]
  (let [value (re-frame/subscribe [::subs/field-value (keyword name)])
        cursor-position (re-frame/subscribe [::subs/cursor-position (keyword name)])]
    (reagent/create-class
     {:display-name (str name "-editor")
      :component-did-update  #(.setSelectionRange
                               (js/document.getElementById (str name "-editor"))
                               @cursor-position
                               @cursor-position)
      :reagent/render
      (fn [id]
        [:> form
         [screenreader-label name (str name "-editor")]
         [:> textarea
          {:id (str name "-editor")
           :style {:padding 0
                   :border "none"}
           :value (or @value "")
           :fluid "true"
           :rows rows
           :on-change #(do
                         (re-frame/dispatch-sync [:edit-input-value (keyword name) (-> % .-target .-value)])
                         (re-frame/dispatch-sync [:update-cursor-position
                                                  (keyword name)
                                                  (-> % .-target .-selectionStart)]))}]])})))

(defn editor-panel [config]
  (let [path [:input-fields (-> config :name keyword)]
        collapsed? (re-frame/subscribe [::subs/collapsed? path])]
    (fn [config]
      [:> grid-column {:width (:width config)}
       [:> segment {:raised true}
        [title-label (:name config)]
        [collapse-label path]
        [:> divider {:style {:margin "1.5rem 0 0.5rem 0"}}]
        (when-not @collapsed?
          [editor config])]])))

;;; Result field

(defn result []
  (let [content (re-frame/subscribe [::subs/process-result])
        loading? (re-frame/subscribe [::subs/result-loading?])
        collapsed? (re-frame/subscribe [::subs/collapsed? [:result]])]
    (fn []
      (when-not @collapsed?
        (if @loading?
          [:> segment {:basic true}
           [:> loader {:active true
                       :style {:padding "1.5rem"}}]]

          [:> form
           [screenreader-label "Result" "result-panel"]
           [:> textarea {:id "result-panel"
                         :placeholder "No result"
                         :value (or @content "")
                         :rows (count (clj-str/split-lines @content))
                         :fluid "true"
                         :style {:border "none"}
                         :readOnly true}]])))))

(defn result-panel [width]
  [:> grid-column {:width width}
   [:> segment {:raised true}
    [title-label "Result"]
    [collapse-label [:result]]
    [:> divider {:style {:margin "1.5rem 0"}}]
    [result]]])

;;; Main panel

(defn main-panel []

  (register-keydown-rules)

  [:> container
   [:> segment

    [page-header]

    [control-panel]

    [:> grid {:stackable true}

     [editor-panel data-config]

     [editor-panel flux-config]

     [editor-panel fix-config]

     [result-panel (:width result-config)]]]])
