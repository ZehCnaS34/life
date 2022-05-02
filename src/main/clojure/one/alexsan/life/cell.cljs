(ns one.alexsan.life.cell
  (:require [re-frame.core :as frame]))

(def max-coord 50)

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

(defrecord Cursor [x y]
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
    (str "cursor:" x ":" y)))

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
