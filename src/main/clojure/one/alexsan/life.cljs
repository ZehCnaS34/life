(ns one.alexsan.life
  (:require [goog.dom :as gdom]
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

(defn delay-sync [ref event]
  (letfn [(handler [_ _ _ new-value]
            (>> (conj event new-value)))]
    (let [handler (util/debounce 10 handler)]
      (println "adding watch")
      (add-watch ref ::delay-sync handler))))

(defn speed-adjuster
  []
  (let [speed    (<< [:speed])
        internal (r/atom nil)]
    (delay-sync internal [:set-speed])
    [:span
     [:label "Speed " speed]
     [:input {:type      :range
              :min       20
              :max       10000
              :value     (or @internal speed)
              :on-change #(>> [:set-speed (util/event-value %)])}]]))

(defn action-view
  []
  (let [active? (<< [:active?])]
    [:div.actions
     [:div.stats
      [stat-view "Generation" [:generation]]
      [stat-view "Cells" [:cell-count]]]
     [:div.controls
      [speed-adjuster]
      [:button {:on-click #(>> [:random-world])} "new world"]
      [:button {:on-click #(>> [:clear-world])} "clear world"]
      [:button
       {:on-click #(>> [(if active? :pause :play)])}
       (if active? "pause" "play")]]]))

(defn world-view
  []
  (let [w (<< [:world])]
    [:div.world
     {:on-mouse-down  #(>> [:place-cell])
      :on-mouse-leave #(>> [:clear-cursor])
      :on-mouse-move  (util/debounce 50 #(>> [:set-cursor-position (util/event->cell-pos %)]))}
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
