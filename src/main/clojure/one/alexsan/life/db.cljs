(ns one.alexsan.life.db
  (:require [re-frame.core :as frame]
            [one.alexsan.life.cell :as cell]))

(defn trigger-generation [ms event]
  {:id     ::trigger-generation
   :before identity
   :after  (fn [context]
             (let [active? (-> context :effects :db :active?)]
               (cond-> context
                 active? (update-in [:effects :fx] (fnil conj [])
                                    [::periodic! {:ms ms :event event}]))))})

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
   (println "setting")
   (js/setTimeout #(frame/dispatch event) ms)))

(frame/reg-event-db
 :init
 [(trigger-generation 1000 [:update-world])]
 (fn [_ _]
   {:active?    true
    :generation 0
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
 [(trigger-generation 1000 [:update-world])]
 (fn [db []]
   (assoc db :active? true)))

(frame/reg-sub
 :world
 (fn [db _]
   (:world db)))

(frame/reg-sub
 :cell-count
 :<- [:world]
 (fn [world _]
   (count world)))

(frame/reg-sub
 :cursor
 (fn [db _]
   (:cursor db)))

(frame/reg-sub
 :generation
 (fn [db _]
   (:generation db)))

(frame/reg-sub
 :active?
 (fn [db _]
   (:active? db)))

(frame/reg-event-db
 :update-world
 [(create-world-when-empty)
  (trigger-generation 1000 [:update-world])]
 (fn [db _]
   (-> db
       (update :world cell/update-world)
       (update :generation inc))))
