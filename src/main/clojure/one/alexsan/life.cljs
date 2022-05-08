(ns one.alexsan.life
  (:require [goog.dom :as gdom]
            [reagent.core :as r]
            [reagent.dom :as rdom]
            [one.alexsan.life.gl :as gl]
            [one.alexsan.life.cell :as cell]
            [one.alexsan.life.db :as db]
            [one.alexsan.life.util :as util]
            [re-frame.core :as frame]
            [cljs.core.async :as asy]))

(def << (comp deref frame/subscribe))
(def >>! frame/dispatch-sync)
(def >> frame/dispatch)
(def root (gdom/getElement "root"))

(defn stat-view [label sub]
  (let [value (<< sub)]
    [:span.stat [:span.label label] [:span.value value]]))

;; NOTE: This may contain a bug when used in a component.
(defn delay-sync [ref event]
  (letfn [(handler [_ _ _ new-value]
            (>> (conj event new-value)))]
    (let [handler (util/debounce 10 handler)]
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
              :max       1000
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

(defrecord Cell [x y]
  gl/Renderable
  (init [this gl]
    (println "initialize init"))
  (render [this deps]
    (println "render cell"))
  (fini [this gl]
    (println "fini")))

(defn world-view
  []
  (let [renderer* (atom nil)]
    (r/create-class
     {:component-did-mount
      (fn [this]
        (let [cell   (->Cell 0 0)
              node   (rdom/dom-node this)
              canvas (.querySelector node "canvas")
              gl     (gl/context canvas)]
          (reset! renderer*
                  (doto (gl/make-renderer gl (fn [dt]))
                    (gl/add-renderable cell)
                    (gl/start {})))))
      :component-will-unmount
      (fn [this]
        (when-let [r @renderer*]
          (gl/stop r {})))
      :render
      (fn [this]
        [:div.world [:canvas]])})))

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
