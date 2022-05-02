(ns one.alexsan.life.util)

(defn mouse-position [e]
  (let [rect (.. e -target (getBoundingClientRect))
        x    (-  (.-clientX e) (.-left rect))
        y    (- (.-clientY e) (.-top rect))]
    {:x x :y y}))

(defn mouse-pos->cell-pos [pos]
  (let [cell-size (/ 700 50)
        tf        (comp int #(/ % cell-size))]
    (-> pos
        (update :x tf)
        (update :y tf))))

(def event->cell-pos (comp mouse-pos->cell-pos mouse-position))

(defn timeout [m f]
  (let [handle (js/setTimeout f m)]
    #(js/clearTimeout handle)))

(defn debounce [ms f]
  (let [handle (atom nil)]
    (fn [& args]
      (when-let [handle (deref handle)] (handle))
      (reset! handle (timeout ms #(apply f args))))))

(defn event-value [event]
  (.. event -target -value))
