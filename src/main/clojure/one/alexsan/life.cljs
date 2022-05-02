(ns one.alexsan.life
  (:require [clojure.pprint :as pp]
            [goog.dom :as gdom]
            [reagent.core :as r]
            [one.alexsan.life.cell :as cell]
            [one.alexsan.life.db :as db]
            [one.alexsan.life.util :as util]
            [re-frame.core :as frame]
            [reagent.dom :as rdom]
            [cljs.core.async :as asy]))

(def << (comp deref frame/subscribe))
(def >>! frame/dispatch-sync)
(def >> frame/dispatch)
(def root (gdom/getElement "root"))

(defn stat-view [label sub]
  (let [value (<< sub)]
    [:span.stat [:span.label label] [:span.value value]]))

(defn action-view
  []
  (let [active? (<< [:active?])]
    [:div.actions
     [:div.stats
      [stat-view "Generation" [:generation]]
      [stat-view "Cells" [:cell-count]]]
     [:div.controls
      [:button {:on-click #(>> [:random-world])} "new world"]
      [:button {:on-click #(>> [:clear-world])} "clear world"]
      [:button
       {:on-click #(>> [(if active? :pause :play)])}
       (if active? "pause" "play")]]]))

(defn world-view
  []
  (let [w (<< [:world])]
    [:div.world
     {:on-mouse-down #(>> [:place-cell])
      :on-mouse-leave #(>> [:clear-cursor])
      :on-mouse-move  #(>> [:set-cursor-position (util/event->cell-pos %)])}
     (for [cell w]
       [:div.cell
        {:data-x (:x cell)
         :data-y (:y cell)
         :style  (cell/style cell)
         :key    (cell/react-key cell)}])]))

(defn world-container-view
  []
  (let [c (<< [:cursor])]
    [:div.world-container
     [:div.cursor
      {:style (if c (cell/style c) {:display :none})
       :key   (if c (cell/react-key c) "-")}]
     [world-view]
     ]))

(defn view
  []
  [:<>
   [world-container-view]
   [action-view]])

(defn ^:dev/after-load render []
  (rdom/render [view] root))

(defn main []
  (>> [:init])
  (render)
  )
