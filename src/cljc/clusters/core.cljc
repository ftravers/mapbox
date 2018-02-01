(ns clusters.core
  #?(:cljs
     (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]]))
  (:require
   #?(:cljs [cljs.core.async :as async]
      :clj [clojure.core.async :as async :refer [go go-loop alt!]])
   [clusters.channels :as pub-sub]
   [re-com.core :as recom]
   [reagent.core :as reagent]
   [clusters.common-utils :as com]
   [clusters.integration :as integration]
   [com.rpl.specter :as specter]
   #?(:cljs [clusters.mapbox :as mapbox])
   #?(:cljs [clusters.create-clusters :as clusters])))

#?(:cljs (enable-console-print!))

;; This page is broken up into two sub components, the first is the
;; list of clusters and regions.  The second is the interactive mapbox
;; map of regions.  To allow these two components to communicate,
;; without causing un-necessary coupling a pub-sub queue is used to
;; handle inter component communication.  Here is a list of events
;; that are sent:

{;; map-state documentation - everything lives under geography root 
 :geography
 {
  ;; This is the channel (core async queue) upon which
  ;; information (events/messages) should be sent that need handling
  ;; by a component outside the current components scope.
  :send-ch (async/chan)

  ;; Contains a function that takes one argument, a keyword that
  ;; represents the type of message this channel will listen for.  For
  ;; example, it could be the message: :region-list-modified, the
  ;; would have as payload the list of regions that have been modified
  :make-subscription-fn (fn [event-name])

  ;; the full list of all regions
  :regions
  {;; key is region id
   0
   {;; full region name
    :region/name "San Mateo County, CA"}}

  ;; the users defined clusters, keys are cluster ID's
  :clusters
  {;; cluster ID
   0
   {:cluster/name "Top Shelf Valley"

    ;; the class the html element should be, can also be
    ;; "card-hover" or "card-selected"
    :class "card"

    ;; the region ids that correspond to this cluster
    :selected-regions [0 1 2 3]
    }}}}

(defonce map-state
  #?(:cljs (reagent/atom nil)
     :clj (atom nil)))

(defn set-prop [db prop val]
  (swap! db assoc-in (into [] (flatten (conj [:geography] prop))) val)
  db)

;; ------------- Handle Events ---------------
(defn toggle-region-membership [region-id]
  
  )

(defn recv-region-list [db regions]
  (com/debug (str "Got region data: " (count regions) " regions."))
  (com/debug regions)
  (set-prop db :regions regions))

(defn recv-cluster-list [db clusters]
  (com/debug "Got cluster data: " clusters)
  (->> clusters
       (specter/transform [specter/MAP-VALS] #(assoc % :class "card"))
       (set-prop db :clusters)))

(defn setup-event-listeners [sub-fn db]
  "Here we create the listeners for the different types of events in
  the system."
  (println "sub fn" (sub-fn :abc))
  (let [toggle-region-membership-ch (sub-fn :toggle-region-membership)
        cluster-created-ch (sub-fn :cluster-created)
        cluster-deleted-ch (sub-fn :cluster-deleted)
        recv-region-list-ch (sub-fn :region-list)
        recv-cluster-list-ch (sub-fn :cluster-list)]
    (go-loop []
      (com/trace "Event Loop waiting for next message.")
      (alt!
        ;; ---------------- Events --------------
        toggle-region-membership-ch
        ([msg] (com/debug "[EVENT]: Region Membership Changed." msg))

        cluster-deleted-ch
        ([msg] (com/debug "[EVENT]: Cluster deleted."))

        cluster-created-ch
        ([msg] (com/debug "[EVENT]: Cluster created."))

        recv-region-list-ch
        ([{regions :data}] (recv-region-list db regions))
        
        recv-cluster-list-ch
        ([{clusters :data}] (recv-cluster-list db clusters))
        )
      (recur))))

(defn pub-sub-init [db]
  "Setup the pub/sub channels and message debugging."
  (let [[send-ch all-msgs-ch subscription-fn]
        (pub-sub/pub-n-everything :msg-type)] 
    (-> db
        (set-prop [:send-ch] send-ch)
        (set-prop [:make-subscription-fn] subscription-fn))

    ;; setup debugging of all messages
    (go-loop []
      (let [msg (async/<! all-msgs-ch)
            type (:msg-type msg)
            body (dissoc msg :msg-type)]
        (com/trace (str "[ALL-MSGS type]: " type))
        (com/trace (str "[ALL-MSGS body]: " body)))
      (recur))

    (setup-event-listeners subscription-fn db)
    (integration/connect-to-backend send-ch subscription-fn)
    ))

(defn get-cluster-region-data [db]
  (let [send-ch (-> @db :geography :send-ch)]
    (go
      (async/>! send-ch {:msg-type :get-regions})
      (async/>! send-ch {:msg-type :get-clusters})
      )
    )
  )

(pub-sub-init map-state)
(get-cluster-region-data map-state)
;; Each cluster has a list of regions which 'belong' to it, or of
;; which it is made up of.  This event is used to add or remove a
;; region from a cluster.
;; (go-loop []
;;   (.log js/console "abc" (str (<! (get-message db :blah))))
;;   (recur))

;; (go-loop []
;;   (let [message :toggle-region-membership
;;         fields [cluster-internal-id region-name region-id]
;;         {:keys fields} (<! (get-message db message))]

;;     ;; update cluster with new list of regions
;;     )
;;   (recur))

;; (go-loop []
;;   (let [message :new-cluster-created
;;         fields [cluster-name]
;;         {:keys fields} (get-message db message)]
;;     ;; get next internal ID for cluster

;;     ;; external call to create new cluster
;;     ;; data: cluster-name
;;     )
;;   (recur))

;; (go-loop []
;;   (let [message :cluster-deleted
;;         fields [cluster-id]
;;         {:keys fields} (get-message db message)]
;;     ;; lookup external DB ID

;;     ;; call externally to delete the given cluster
;;     ;; data: extern-db-id of cluster
;;     )
;;   (recur))

;; ----------------- Mount main UI ------------------
#?(:cljs (defn title []
   [:div "Select a cluster to manage, or add a new one."]))

#?(:cljs (defn body [map-state]
   [recom/h-box
    :gap "10px"
    :justify :around
    :children [
               [clusters/clusters map-state]
               (if (com/cluster-selected? map-state)
                 [mapbox/the-map map-state])]]))

#?(:cljs (defn page [map-state]
   [recom/modal-panel
    :child [recom/v-box
            :children [[title]
                       [body map-state]]]]))

#?(:cljs (defn on-js-reload []
   ;; (init map-state)
   (reagent/render [page map-state] (.getElementById js/document "app"))))

#?(:cljs (defn ^:export main []
   (on-js-reload)))

#?(:cljs (on-js-reload))


