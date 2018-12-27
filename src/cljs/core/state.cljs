(ns core.state)

(defn create-app-state [id {:keys [width height renderer events] :as config}]
  (let [app-state (atom {:dom {:id id :width width :height height}
                         :events (:events config)
                         :diagram {}})]
      {:get-state      (fn [root]
                          (get-in @app-state [root]))
       :get-in-state   (fn [root relative-path]
                          (let [absolute-path (concat [root] relative-path)]
                             (get-in @app-state absolute-path)))
       :assoc-state    (fn [root relative-path new-state]
                          (let [absolute-path (concat [root] relative-path)]
                             (swap! app-state assoc-in absolute-path new-state)))
       :dissoc-state   (fn [root relative-path]
                          (let [last-entry (last relative-path)
                                absolute-path (concat [root] (drop-last relative-path))]
                             (swap! app-state update-in absolute-path dissoc last-entry)))}))

(defn get-state [app-state root]
  ((:get-state app-state) root))

(defn get-in-state [app-state root relative-path]
  ((:get-in-state app-state) root relative-path))

(defn assoc-state [app-state root relative-path new-state]
  ((:assoc-state app-state) root relative-path new-state))

(defn dissoc-state [app-state root relative-path]
  ((:dissoc-state app-state) root relative-path))

(defn get-diagram-state [app-state]
  (get-state app-state :diagram))

(defn get-in-diagram-state [app-state relative-path]
  (get-in-state app-state :diagram relative-path))

(defn assoc-diagram-state [app-state relative-path new-state]
  (assoc-state app-state :diagram relative-path new-state))

(defn dissoc-diagram-state [app-state relative-path]
  (dissoc-state app-state :diagram relative-path))

(defn get-event-bus-state [app-state]
  (get-state app-state :event-bus))

(defn get-in-event-bus-state [app-state relative-path]
  (get-in-state app-state :event-bus relative-path))

(defn assoc-event-bus-state [app-state relative-path new-state]
  (assoc-state app-state :event-bus relative-path new-state))

(defn dissoc-event-bus-state [app-state relative-path]
  (dissoc-state app-state :event-bus relative-path))

(defn get-events-state [app-state]
  (get-state app-state :events))

(defn get-in-events-state [app-state relative-path]
  (get-in-state app-state :events relative-path))

(defn assoc-events-state [app-state relative-path new-state]
  (assoc-state app-state :events relative-path new-state))

(defn dissoc-events-state [app-state relative-path]
  (dissoc-state app-state :events relative-path))

(defn get-behaviours-state [app-state]
  (get-state app-state :behaviours))

(defn get-in-behaviours-state [app-state relative-path]
  (get-in-state app-state :behaviours relative-path))

(defn assoc-behaviours-state [app-state relative-path new-state]
  (assoc-state app-state :behaviours relative-path new-state))

(defn dissoc-behaviours-state [app-state relative-path]
  (dissoc-state app-state :behaviours relative-path))

(defn get-renderer-state [app-state]
  (get-state app-state :renderer))

(defn get-in-renderer-state [app-state relative-path]
  (get-in-state app-state :renderer relative-path))

(defn assoc-renderer-state [app-state relative-path new-state]
  (assoc-state app-state :renderer relative-path new-state))

(defn dissoc-renderer-state [app-state relative-path]
  (dissoc-state app-state :renderer relative-path))

(defn get-extensions-state [app-state]
  (get-state app-state :extensions))

(defn get-in-extensions-state [app-state relative-path]
  (get-in-state app-state :extensions relative-path))

(defn assoc-extensions-state [app-state relative-path new-state]
  (assoc-state app-state :extensions relative-path new-state))

(defn dissoc-extensions-state [app-state relative-path]
  (dissoc-state app-state :extensions relative-path))

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
