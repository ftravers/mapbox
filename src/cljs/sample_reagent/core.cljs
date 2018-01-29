(ns sample-reagent.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]])
  (:require
   [cljs.core.async :as async :refer [put! chan <! >! timeout close! mult tap]]
   [re-com.core :as recom]
   [reagent.core :as reagent]
   [sample-reagent.common-utils :as com]
   [sample-reagent.mapbox :as mapbox]
   [sample-reagent.create-clusters :as clusters]))

;; This page is broken up into two sub components, the first is the
;; list of clusters and regions.  The second is the interactive mapbox
;; map of regions.  To allow these two components to communicate,
;; without causing un-necessary coupling a pub-sub queue is used to
;; handle inter component communication.  Here is a list of events
;; that are sent:


;; map-state documentation
{;; everything lives under this root 
 :geography
 {
  ;; This is the queue (core async channel) upon which
  ;; information (events/messages) should be sent that need handling
  ;; by a component outside the current components scope.
  :send-queue (async/chan)

  ;; Contains a function that takes one argument, a keyword that
  ;; represents the type of message this channel will listen for.  For
  ;; example, it could be the message: :region-list-modified, the
  ;; would have as payload the list of regions that have been modified
  :make-subscription (fn [event-name])

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
  (reagent/atom {:geography
                 {:regions
                  {0 {:region/name "San Mateo County, CA"}
                   1 {:region/name "Siskiyou County, CA"}
                   2 {:region/name "Lassen County, CA"}
                   3 {:region/name "Del Norte County, CA"}
                   4 {:region/name "Lake County, CA"}
                   5 {:region/name "San Joaquin County, CA"}
                   6 {:region/name "Butte County, CA"}
                   7 {:region/name "Marin County, CA"}
                   8 {:region/name "Nevada County, CA"}
                   9 {:region/name "Mariposa County, CA"}
                   10 {:region/name "Amador County, CA"}
                   11 {:region/name "Modoc County, CA"}
                   12 {:region/name "Humboldt County, CA"}
                   13 {:region/name "San Diego County, CA"}
                   14 {:region/name "Santa Barbara County, CA"}
                   15 {:region/name "San Francisco County, CA"}
                   16 {:region/name "Orange County, CA"}
                   17 {:region/name "Santa Clara County, CA"}
                   18 {:region/name "Mono County, CA"}
                   19 {:region/name "Kings County, CA"}
                   20 {:region/name "Colusa County, CA"}
                   21 {:region/name "San Benito County, CA"}
                   22 {:region/name "Tuolumne County, CA"}
                   23 {:region/name "Sacramento County, CA"}
                   24 {:region/name "Sierra County, CA"}
                   25 {:region/name "Monterey County, CA"}
                   26 {:region/name "Mendocino County, CA"}
                   27 {:region/name "San Luis Obispo County, CA"}
                   28 {:region/name "El Dorado County, CA"}
                   29 {:region/name "Santa Cruz County, CA"}
                   30 {:region/name "Alpine County, CA"}
                   31 {:region/name "Madera County, CA"}
                   32 {:region/name "Yolo County, CA"}
                   33 {:region/name "Sutter County, CA"}
                   34 {:region/name "Merced County, CA"}
                   35 {:region/name "Inyo County, CA"}
                   36 {:region/name "Stanislaus County, CA"}
                   37 {:region/name "Plumas County, CA"}
                   38 {:region/name "Alameda County, CA"}
                   39 {:region/name "Tulare County, CA"}
                   40 {:region/name "Napa County, CA"}
                   41 {:region/name "Shasta County, CA"}
                   42 {:region/name "Riverside County, CA"}
                   43 {:region/name "Sonoma County, CA"}
                   44 {:region/name "Ventura County, CA"}
                   45 {:region/name "Contra Costa County, CA"}
                   46 {:region/name "Glenn County, CA"}
                   47 {:region/name "Calaveras County, CA"}
                   48 {:region/name "Trinity County, CA"}
                   49 {:region/name "Solano County, CA"}
                   50 {:region/name "Kern County, CA"}
                   51 {:region/name "Placer County, CA"}
                   52 {:region/name "San Bernardino County, CA"}
                   53 {:region/name "Los Angeles County, CA"}
                   54 {:region/name "Fresno County, CA"}
                   55 {:region/name "Imperial County, CA"}
                   56 {:region/name "Tehama County, CA"}
                   57 {:region/name "Yuba County, CA"}}
                  :clusters
                  {0 {:cluster/name "Top Shelf Valley"
                      :class "card"
                      :selected-regions [0 1 2 3]}
                   1 {:cluster/name "Inland Desert"
                      :class "card"
                      :selected-regions [4 5 6 7]}}}}))

;; must call from go block
(defn get-message [db msg-type]
  (let [pub-queue (get-in @db [:geography :msg-type-pub-queue])
        sub-channel (chan)]
    (async/sub pub-queue msg-type sub-channel)
    sub-channel))

(defn all-msgs [db]
  (let [m (get-in @db [:geography :mult])
        msgs-ch (chan)]
    (tap m msgs-ch)
    msgs-ch))

(def sample-msgs
  {:toggle-region-membership
   {:msg-type :toggle-region-membership
    :cluster-internal-id 2
    :region-id 2}

   :cluster-deleted
   {:msg-type :cluster-deleted
    :cluster-id 2}

   :new-cluster-created
   {:msg-type :new-cluster-created
    :cluster-name "abc123"}
   })

;; BUG: seems the queues are filling up after second put



(defn send-msg [msg-name]
  (let [send-queue (get-in @map-state [:geography :send-queue])
        msg (msg-name sample-msgs)]
    ;; (com/debug "MSG: " msg)
    (go
      (>! send-queue msg))))

;; Setup event listeners
(defn init [db]
  ;; Setup the pub/sub queue
  (let [send-queue (async/chan)
        m (mult send-queue)
        send-queue-copy (chan)]
    (tap m send-queue-copy)
    (swap! db assoc-in [:geography :mult] m)
    (swap! db assoc-in [:geography :send-queue] send-queue)
    (swap! db assoc-in [:geography :msg-type-pub-queue] (async/pub send-queue-copy :msg-type)))
  


  ;; ALL EVENTS will get captured by the following loop, used for
  ;; logging, etc...
  (let [toggle-region-membership-ch (get-message db :toggle-region-membership)
        new-cluster-ch (get-message db :new-cluster-created)
        cluster-deleted-ch (get-message db :cluster-deleted)]
    (go-loop []
      (alt!
        toggle-region-membership-ch ([msg] (com/debug "Region Membership Changed."))
        cluster-deleted-ch ( [msg] (com/debug "Cluster deleted."))
        new-cluster-ch ([msg] (com/debug "New Cluster")))
      (recur)))

  (go-loop []
    (com/debug "Event: " (<! (all-msgs db)))
    (recur))

  


  ;; ---------------- Events --------------
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

  )



(defn title []
  [:div "This is the title"])

(defn body [map-state]
  [recom/h-box
   :gap "10px"
   :justify :around
   :children [
              [clusters/clusters map-state]
              (if (com/cluster-selected? map-state)
                [mapbox/the-map map-state])]])

(defn page [map-state]
  [recom/modal-panel
   :child [recom/v-box
           :children [[title]
                      [body map-state]]]])

(defn on-js-reload []
  ;; (init map-state)
  (reagent/render [page map-state] (.getElementById js/document "app")))

(defn ^:export main []
  (on-js-reload))

(on-js-reload)



;; (go-loop []
;;   (let [{:keys [text]} (<! output-chan)]
;;     (println text)
;;     (recur)))

;; (>!! input-chan {:msg-type :greeting :text "hi"})
