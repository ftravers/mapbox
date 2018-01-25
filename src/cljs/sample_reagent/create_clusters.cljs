(ns sample-reagent.create-clusters
  (:require
   [re-com.core :as recom]
   [clojure.string :as string]
   [reagent.core :as reagent]
   [sample-reagent.common-utils :as com]))

(def ms (reagent/atom {:geography
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
;; ------------ HELPERS -------------------
;; (defn sub [path]
;;   (subscribe [:get-in path]))

;; (defn setasync [path val]
;;   (a-util/asock-in path val))

;; (defn setsync [path val]
;;   (a-util/asock-in-sync path val))


;; ;; ------------ UI -------------------

(defn region-row [map-state rgn-id curr-cluster]
  (let [curr-regions
        (com/sub map-state [:geography :selected-regions])
        class (reagent/atom "card")]
    (fn [map-state rgn-id _]
      ;; (.log js/console "Curr Selctd Regns: " @curr-regions)
      (let [region-name (:region/name (@(com/all-regions map-state) rgn-id))
            checked? (some #{rgn-id} @curr-regions)]
        ;; (.log js/console "Region Name: " region-name ", Region ID: " rgn-id)
        [recom/box
        :class @class
        :attr
        {:on-mouse-enter #(reset! class "card-hover")
         :on-mouse-leave #(reset! class "card")
         :on-click #(com/toggle-region-membership map-state rgn-id)}
        :child
        [recom/checkbox
         :model checked?
         :label region-name
         :on-change #()]]))))

(defn regions-lst [map-state curr-cluster]
  (let [all-rgns (com/all-regions map-state)]
    (fn [map-state curr-cluster]
     [recom/v-box
      :gap "10px"
      :children
      [
       (doall
        (for [{:keys [region/id]}
              (com/sort-db-map @(com/all-regions map-state) :region/name :region/id)]
          ^{:key id} [region-row map-state id curr-cluster]))]])))

(defn cluster-row [map-state clstr-id cluster]
  (let [class (com/sub map-state [:geography :clusters clstr-id :class])]
    (fn [map-state clstr-id clusters]
      ;; (info "cluster class" @class)
      [recom/h-box
       :gap "10px"
       :align :baseline
       :children
       [[recom/v-box
         :gap "10px"
         :width "90%"
         :attr
         {:on-mouse-enter #(com/set-cluster-class map-state clstr-id "card-hover")
          :on-mouse-leave #(com/set-cluster-class map-state clstr-id (com/get-not-hovered-clstr-class map-state clstr-id))
          :on-click #(com/set-selected-cluster map-state clstr-id)}
         :class @class
         :children
         [[recom/label :label (:cluster/name cluster)]]]
        [recom/md-icon-button
         :md-icon-name "zmdi-delete"
         :on-click (partial com/delete-cluster map-state clstr-id)]]])))

(defn create-cluster [map-state]
  (fn [map-state]
    [recom/h-box
     :gap "10px"
     :children
     [[recom/input-text
       :model (com/sub map-state [:geography :new-cluster-name])
       :on-change #(com/setasync map-state [:geography :new-cluster-name] %)]
      [recom/button
       :label "Add Cluster"
       :on-click (partial com/add-new-cluster map-state)]]]))

(defn cluster-lst [map-state]
  (let [clusters (com/all-clusters map-state)]
    (fn [map-state]
     [recom/v-box
      :gap "10px"
      :children
      (concat [[create-cluster map-state]]
              (doall
               (for [{:keys [cluster/id] :as clstr}
                     (com/sort-db-map @(com/all-clusters map-state) :cluster/name :cluster/id)]
                 ^{:key id} [cluster-row map-state id clstr])))])))

(defn clusters [map-state]
  (let [selctd-clstr (com/selected-cluster map-state)]
    (com/debug "Selected Cluster is: " @selctd-clstr)
    (if @selctd-clstr (com/set-selected-cluster map-state @selctd-clstr))
    (com/setsync map-state [:geography :new-cluster-name] "")
    (fn [map-state]
      [recom/v-box
       :gap "10px"
       :children
       [[recom/h-box
         :gap "10px"
         :justify :around
         :children [[cluster-lst map-state]
                    (if (com/cluster-selected? map-state)
                      [regions-lst map-state selctd-clstr])
                    ]]]])))
