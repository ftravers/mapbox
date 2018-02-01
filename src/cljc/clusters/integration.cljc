(ns clusters.integration
  #?(:cljs
     (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]]))
  (:require
   [clusters.common-utils :as com]
   #?(:cljs [cljs.core.async :as async]
      :clj [clojure.core.async :as async :refer [go go-loop alt!]])))

(def test-data-1
  {:geography
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
        ;; add the following line in in the client somewhere
        ;; :class "card"
        :selected-regions [0 1 2 3]}
     1 {:cluster/name "Inland Desert"
        ;; add the following line in in the client somewhere
        ;; :class "card"
        :selected-regions [4 5 6 7]}}}})

(defn connect-to-backend [reply-ch subscribe-fn]
  "This simulates a real backend connection.  We will listen for
  specific messages by subscribing to messages using the
  'subscribe-fn' and we'll write our response messages into the
  'reply-ch'."
  (let [get-regions-ch  (subscribe-fn :get-regions)
        get-clusters-ch (subscribe-fn :get-clusters)]
    (go-loop []
      (alt!
        get-regions-ch
        ([msg]
         (com/debug "Integration: Getting Regions" msg)
         (async/>! reply-ch
             {:msg-type :region-list
              :data (-> test-data-1 :geography :regions)}))
        
        get-clusters-ch
        ([msg] (com/debug "Integration: Getting Clusters" msg)
         (async/>! reply-ch
             {:msg-type :cluster-list
              :data (-> test-data-1 :geography :clusters)})))
      (recur))))
