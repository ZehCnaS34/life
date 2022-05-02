(ns one.alexsan.life
  (:require [clojure.pprint :as pp]
            [goog.dom :as gdom]
            [reagent.core :as r]
            [reagent.dom :as rdom]
            [cljs.core.async :as asy]))

(def max-coord 50)

(def live-rules
  {2 :live
   3 :live})

(def dead-rules
  {3 :spawn})

(defprotocol ReactKey
  (react-key [this]))

(defprotocol Style
  (style [this]))

(defprotocol InBound
  (in-bound? [this]))

(defrecord Cell [x y]
  InBound
  (in-bound? [this]
    (and (<= 0 x (dec max-coord))
         (<= 0 y (dec max-coord))))
  Style
  (style [this]
    (let [x% (str (* 100 x) "%")
          y% (str (* 100 y) "%")]
      {:position  :absolute
       :height    "2%"
       :width     "2%"
       :transform (str "translate(" x% "," y% ")")}))
  ReactKey
  (react-key [this]
    (str x ":" y)))

(defn cell [x y]
  (->Cell x y))

(defn surrounding [{:keys [x y] :as self}]
  (->> (for [x'    (range (dec x) (inc (inc x)))
             y'    (range (dec y) (inc (inc y)))
             :let  [neighbor (->Cell x' y')]
             :when (and (not= neighbor self)
                        (in-bound? neighbor))]
         neighbor)
       (into #{})))

(defn live-neighbor-count [world {:keys [x y] :as self}]
  (->> (for [x'    (range (dec x) (inc (inc x)))
             y'    (range (dec y) (inc (inc y)))
             :let  [neighbor (->Cell x' y')]
             :when (and (not= neighbor self)
                        (contains? world neighbor)
                        (in-bound? neighbor))]
         neighbor)
       (into #{})
       (count)))

(defn rand-world [n]
  (->> (for [x (range n)
             y (range n)]
         (cell x y))
       (random-sample 0.2)
       (into #{})))

(def root (gdom/getElement "root"))

(defonce world (r/atom (rand-world max-coord)))

(defn world-view
  []
  (let [w @world]
    [:div.world
     (for [cell w]
       [:div.cell
        {:style (style cell)
         :key   (react-key cell)}])]))

(defn get-affected-cells
  [world]
  (distinct (mapcat surrounding world)))

(defn update-world
  [world]
  (let [affected (get-affected-cells world)
        living   (for [cell  affected
                       :let  [live? (contains? world cell)
                              c (live-neighbor-count world cell)]
                       :when (or (and (not live?) (= c 3))
                                 (and live? (or (= c 2) (= c 3))))]
                   cell)]
    (into #{} living)))

(defonce time-loop
  (asy/go-loop []
    (asy/<! (asy/timeout 1000))
    (swap! world update-world)
    (recur))
  )

(defn action-view
  []
  [:div.actions
   [:button {:on-click #(swap! world update-world)} "step"]])

(defn view
  []
  [:<>
   [world-view]
   [action-view]])

(defn ^:dev/after-load main []
  (rdom/render [view] root)
  )
