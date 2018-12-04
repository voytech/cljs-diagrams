(ns core.state)

(defn create-app-state [id {:keys [width height renderer events] :as config}]
  (atom {:dom {:id id
               :width width
               :height height}
         :events (:events config)}))

(defn renderer-state [app-state]
  (-> app-state deref :renderer))

(defn diagram-state [app-state]
  (-> app-state deref :diagram))

(defn behaviours-state [app-state]
  (-> app-state deref :behaviours))

(defn update-state [app-state path new-state]
  (swap! app-state assoc-in path new-state))

(defn with-sub-state [app-state path handler]
  (let [section-state (get-in @app-state path)]
    (handler section-state (fn [new-state] (update-state app-state path new-state)))))
