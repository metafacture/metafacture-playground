(ns metafacture-playground.views
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as re-frame]
   [metafacture-playground.subs :as subs]
   [clojure.string :as clj-str]
   [cljsjs.semantic-ui-react]
   [goog.object]))

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

(def semantic-ui-button (component "Button"))
(def header (component "Header"))
(def container (component "Container"))
(def segment (component "Segment"))
(def grid (component "Grid"))
(def grid-column (component "Grid" "Column"))
(def form (component "Form"))
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
        :onClick #(re-frame/dispatch [:collapse-panel panel-path @collapsed?])}
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

(defn button [{:keys [content dispatch-fn icon-name]}]
  [:> semantic-ui-button
   (merge {:id (-> content (clj-str/replace " " "-") (str "-button"))
           :basic basic-buttons?
           :color color}
          (when dispatch-fn
            {:onClick #(re-frame/dispatch dispatch-fn)}))
   (when icon-name
     [:> icon {:name icon-name}])
   content])

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
      [button {:content "Process"
               :dispatch-fn [:process @data @flux @fix]
               :icon-name "play"}])))

(defn share-links []
  (let [api-call-link (re-frame/subscribe [::subs/link :api-call])]
    (fn []
      [:> input
       {:id "api-call-link-input"
        :action {:color color
                 :icon "copy"
                 :on-click #(re-frame/dispatch [:copy-link @api-call-link])
                 :alt "Copy"}
        :placeholder (if-not @api-call-link "Nothing to share..." "")
        :default-value (or @api-call-link "")
        :disabled (not @api-call-link)
        :label "Result call"
        :readOnly true}])))

(defn share-button []
  [:> popup
   {:content (reagent/as-element [share-links])
    :on "click"
    :position "right center"
    :flowing true
    :trigger (reagent/as-element (button {:content "Share" :icon-name "share alternate"}))}])

(defn control-panel []
  [:> segment {:raised true}
   [button {:content "Load sample" :dispatch-fn [:load-sample] :icon-name "code"}]
   [button {:content "Clear all" :dispatch-fn [:clear-all] :icon-name "erase"}]
   [process-button]
   [share-button]])

;;; Input fields

(defn editor [{:keys [name]}]
  (let [value (re-frame/subscribe [::subs/field-value (keyword name)])]
    (fn [{:keys [name rows]}]
      [:> form
       [screenreader-label name (str name "-editor")]
       ^{:key @value} [:> textarea
                       {:id (str name "-editor")
                        :style {:padding 0
                                :border "none"}
                        :default-value (or @value "")
                        :fluid "true"
                        :rows rows
                        :on-blur #(re-frame/dispatch [:edit-input-value (keyword name) (-> % .-target .-value)])}]])))

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
  [:> container
   [:> segment

    [page-header]

    [control-panel]

    [:> grid {:stackable true}

     [editor-panel data-config]

     [editor-panel flux-config]

     [editor-panel fix-config]

     [result-panel (:width result-config)]]]])
