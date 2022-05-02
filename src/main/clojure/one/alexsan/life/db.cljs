(ns one.alexsan.life.db
  (:require [re-frame.core :as frame]
            [one.alexsan.life.cell :as cell]))

(defn trigger-generation [_ event]
  {:id     ::trigger-generation
   :before identity
   :after  (fn [context]
             (let [active? (-> context :effects :db :active?)
                   speed (-> context :effects :db :speed)]
               (cond-> context
                 active? (update-in [:effects :fx] (fnil conj [])
                                    [::periodic! {:ms speed :event event}]))))})

(def tg (trigger-generation 10 [:update-world]))

(defn create-world-when-empty []
  {:id     ::create-world-when-empty
   :before identity
   :after  (fn [context]
             (cond-> context
               (-> context :effects :db :world empty?)
               (update-in [:effects :fx] (fnil conj []) [:dispatch [:random-world]])))})

(frame/reg-fx
 ::periodic!
 (fn [{:keys [ms event]}]
   (js/setTimeout #(frame/dispatch event) ms)))

(frame/reg-event-db
 :init
 [tg]
 (fn [_ _]
   {:active?    true
    :generation 0
    :speed      1000
    :world      (cell/rand-world 50)}))

(frame/reg-event-db
 :random-world
 (fn [db _]
   (-> db
       (assoc :generation 0)
       (assoc :world (cell/rand-world 50))))
 )

(frame/reg-event-db
 :clear-world
 (fn [db _]
   (-> db
       (assoc :generation 0)
       (assoc :world #{}))))

(frame/reg-event-db
 :set-cursor-position
 (fn [db [_ pos]]
   (assoc db :cursor (cell/->Cursor (:x pos) (:y pos)))))

(frame/reg-event-db
 :clear-cursor
 (fn [db _]
   (dissoc db :cursor)))

(frame/reg-event-db
 :place-cell
 (fn [{:keys [cursor] :as db}_]
   (let []
     (if-not cursor
       db
       (update db :world conj (cell/->Cell (:x cursor) (:y cursor)))))))

(frame/reg-event-db
 :pause
 (fn [db []]
   (assoc db :active? false)))

(frame/reg-event-db
 :play
 [tg]
 (fn [db []]
   (assoc db :active? true)))

(defn reg-root-set [key]
  {:pre [(keyword? key)]}
  (let [event-key (keyword (str "set-" (name key)))]
    (frame/reg-event-db
     event-key
     (fn [db [_ value]]
       (println value)
       (assoc db key value)))))

(reg-root-set :speed)

(defn reg-root-sub [key]
  (frame/reg-sub key (fn [db _] (key db))))

(reg-root-sub :active?)
(reg-root-sub :generation)
(reg-root-sub :cursor)
(reg-root-sub :world)
(reg-root-sub :speed)

(frame/reg-sub
 :cell-count
 :<- [:world]
 (fn [world _]
   (count world)))

(frame/reg-event-db
 :update-world
 [(create-world-when-empty)
  tg]
 (fn [db _]
   (-> db
       (update :world cell/update-world)
       (update :generation inc))))
