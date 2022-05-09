(ns one.alexsan.life.gl)


(defprotocol Render
  (start [this params])
  (stop [this params]))

(defprotocol Renderable
  (init [this gl])
  (render [this deps])
  (fini [this gl]))


(defprotocol RenderContainer
  (add-renderable [this renderable]))

(defrecord Renderer [gl state]
  RenderContainer
  (add-renderable [this renderable]
    (assert (satisfies? Renderable renderable))
    (swap! state update :renderables (fnil conj []) renderable)
    nil)
  Render
  (start [this params]
    (doseq [renderable (-> state deref :renderables)]
      (init renderable gl))
    (swap! state assoc :running? true))
  (stop [this params]
    (swap! state assoc :running? false)
    (doseq [renderable (-> state deref :renderables)]
      (fini renderable gl))))

(defn invoke-when [ref prop handler]
  (add-watch ref ::invoke-when
             (fn listener [_ _ _ value]
               (when (prop value)
                 (handler)))))

(defn make-renderer [gl]
  (let [state   (atom {:running? false
                       :renderables []})
        handler (fn handler []
                  (let [{:keys [running? renderables]} @state]
                    (when running?
                      (js/requestAnimationFrame handler)
                      (doseq [r renderables]
                        (render r {})))))]
    (invoke-when state :running? handler)
    (->Renderer gl state)))

(defn context [canvas]
  (.getContext canvas "webgl2"))
