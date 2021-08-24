(ns metafacture-playground.core
  (:require
   [reagent.dom :as rdom]
   [re-frame.core :as re-frame]
   [metafacture-playground.events :as events]
   [metafacture-playground.views :as views]
   [metafacture-playground.config :as config]
   [re-pressed.core :as rp]))


(defn dev-setup []
  (when config/debug?
    (println "dev mode")))

(defn ^:dev/after-load mount-root []
  (re-frame/clear-subscription-cache!)
  (let [root-el (.getElementById js/document "app")]
    (rdom/unmount-component-at-node root-el)
    (rdom/render [views/main-panel] root-el)))

(defn init []
  (let [href (-> js/window .-location .-href)
        window-height (.-innerHeight js/window)]
    (re-frame/dispatch-sync [::events/initialize-db href window-height]))
  (re-frame/dispatch-sync [::rp/add-keyboard-event-listener "keydown"])
  (dev-setup)
  (mount-root))
