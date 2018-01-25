(ns sample-reagent.common-utils
  (:require
   [clojure.string :as string]
   [reagent.core :as reagent]))

;; ----------------------- logging
(def log-levels {:trace 1
                 :debug 3
                 :info 5
                 :warning 7
                 :error 8
                 :fatal 10})

(def log-threshold (log-levels :trace))

(defn log
  [level & msg]
  (if (>= level log-threshold)
    (.log js/console (apply str msg))))

(defn info [& msg]
  (log (log-levels :info) msg))

(defn debug [& msg]
  (log (log-levels :debug) msg))

(defn trace [& msg]
  (log (log-levels :trace) msg))

(defn nil-or-empty? [strng]
  (or (nil? strng) (empty? strng)))
;; ---------------------------- clusters

(defn sub [db path]
  "db is reagent/atom"
  (reagent/cursor db path))

(defn setsync [db path val]
  (swap! db assoc-in path val))

(defn setasync [db path val]
  (setsync db path val))

(defn dissoc-in
  [m [k & ks]]
  (if-not ks
    (dissoc m k)
    (assoc m k (dissoc-in (m k) ks))))

(defn rm [db path]
  (swap! db dissoc-in path))

;; (defn rm [path]
;;   (a-util/disock-in-sync path))

(defn all-regions [map-state]
  "List of all regions."
  (sub map-state [:geography :regions]))

(defn get-region [map-state region-name]
  (flatten
   (filter
    (fn [[id {name :region/name}]]
      (= name region-name))
                   (get-in @map-state [:geography :regions]))))

(defn get-region-id [map-state region-name]
  (let [rgn (get-region map-state region-name)]
    (if-not (empty? rgn) (first rgn))))

(defn selected-cluster [map-state]
  "Get selected cluster ID."
  (sub map-state [:geography :selected-cluster]))

(defn cluster-selected? [map-state]
  "Is there a selected cluster?"
  (not (nil? @(selected-cluster map-state))))

(defn selected-regions [map-state]
  "List of currently selected regions for active cluster."
  (sub map-state [:geography :clusters @(selected-cluster map-state) :selected-regions]))

(defn selected-regions-names [map-state]
  (let [selected-region-ids @(selected-regions map-state)
        all-regions (get-in @map-state [:geography :regions])]
    (mapv #(:region/name (all-regions %)) selected-region-ids)))

(defn all-clusters [map-state]
  (sub map-state [:geography :clusters]))

(defn set-cluster-class [map-state clstr-id class]
  (debug (str  "Setting cluster id: " clstr-id ", to class: " class))
  (setasync map-state [:geography :clusters clstr-id :class] class))

(defn set-selected-cluster [map-state clstr-id]
  "call this function when a cluster is selected"
  ;; (infof "Setting Selected Cluster ID: %s" clstr-id)
  (if @(selected-cluster map-state) (set-cluster-class map-state @(selected-cluster map-state) "card"))
  (set-cluster-class map-state clstr-id "card-selected")
  (setsync map-state [:geography :selected-cluster] clstr-id)
  (setasync map-state [:geography :selected-regions] @(selected-regions map-state)))

(defn get-not-hovered-clstr-class [map-state clstr-id]
  (if (= clstr-id @(selected-cluster map-state))
    "card-selected"
    "card"))

(defn region-selected? [map-state rgn-id]
  (boolean (some #{rgn-id} @(sub map-state [:geography :selected-regions]))))

(defn determine-new-selected-regions-lst [map-state rgn-id]
  "given the current list of selected regions, and the currently
selected region, return a new list that either adds the new region to
the list or if it exists in the list return a new list that doesn't
contain it."
(let [curr-cluster @(selected-cluster map-state)
        curr-regions @(sub map-state [:geography :selected-regions])]
    (if (region-selected? map-state rgn-id)
      (remove #{rgn-id} curr-regions)
      (conj curr-regions rgn-id))))

(defn set-selected-regions! [map-state new-regions-lst]
  "We have a copy of the selected regions at
  path: [:geography :selected-regions] because we can subscribe to
  this.  Not sure its possible to create a map subscription that
  contains a variable, rather than just a static path."
  (let [curr-cluster @(selected-cluster map-state)]
    (if-not (nil? curr-cluster)
     (do
       (setasync
        map-state
        [:geography :clusters @(selected-cluster map-state) :selected-regions]
        new-regions-lst)

       (setasync
        map-state
        [:geography :selected-regions]
        new-regions-lst)))))

(defn toggle-region-membership [map-state rgn-id]
  "Call this to add or remove a region from the selected-regions
  list."
  (let [new-regions (determine-new-selected-regions-lst map-state rgn-id)]
    (set-selected-regions! map-state new-regions)))

(defn sort-db-map [m sort-key db-key]
  (sort-by sort-key (for [[k v] m] (assoc v db-key k))))

(defn get-next-db-id [db]
  (inc (apply max (keys db))))

(defn add-new-cluster [map-state]
  (let [cluster-name @(sub map-state [:geography :new-cluster-name])
        next-db-id (get-next-db-id @(all-clusters map-state))
        cluster (assoc @(all-clusters map-state) next-db-id
                       {:cluster/name cluster-name
                        :class "card-selected"
                        :selected-regions []})]
    (if-not (nil-or-empty? cluster-name)
      (do (setsync map-state [:geography :clusters] cluster)
          (set-selected-cluster map-state next-db-id))))
  (setasync map-state [:geography :new-cluster-name] ""))

(defn delete-cluster [map-state clstr-id]
  (if (= clstr-id @(selected-cluster map-state))
    (do (rm map-state [:geography :selected-cluster])
        (rm map-state [:geography :selected-regions])))
  (rm map-state [:geography :clusters clstr-id]))


