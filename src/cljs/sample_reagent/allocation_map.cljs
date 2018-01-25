(ns sample-reagent.allocation-map
  (:require
   [re-com.core :as recom]
   [reagent.core :as reagent]
   [sample-reagent.common-utils :as com]))

(def map-layer "calif-counties" )
(defn obj-keys [obj]
  (sort (js->clj (.keys js/Object obj))))

;; data that is use only in this file should be prefixed
;; with the following path:
(def this-file-data-prefix [:geography :mapbox])

(defn set-map-prop [map-state prop val]
  (swap! map-state assoc-in (conj this-file-data-prefix prop) val))

(defn get-map-prop [map-state prop]
  (get-in @map-state (conj this-file-data-prefix prop)))

(defn set-map-origin! [map-state]
  "We need to find the top left corner coordinates of the map so that in
'data-under-cursor' below, we can determine the relative x,y coordinate
of the cursor from its absolute coordinate."
  (let [rect (.getBoundingClientRect (.getElementById js/document "map"))]
    (set-map-prop map-state :x-coord-origin (.-left rect))
    (set-map-prop map-state :y-coord-origin (.-top rect))))

(defn set-layer-filter [mapbox layer filter]
  "Convenience"
  (.setFilter mapbox layer (clj->js filter)))

(defn make-selected-regions-visible [map-state]
  "When we select a county, we make visible the 'selected' layer for that 
county name by adding it to the selected-regions list.  The selected regions
list is populated by the function 'toggle-select-region', this merely changes
which counties are allowed to be shown."
  (set-layer-filter
   (get-map-prop map-state :map)
   "calif-counties-selected"
   (concat ["in" "name"] (com/selected-regions-names map-state))))

(defn set-no-hovers [mapbox]
  "When map first loads, hide the hovers layer by showing (filtering) only
those counties whose name is '', which is none of them, so effectively hide
each county." 
  (set-layer-filter mapbox "calif-counties-hover" ["==" "name" ""]))

(defn toggle-select-region [map-state]
  "Called when we click on a region in the map."
  (com/debug js/console "toggling selected region")
  (let [curr-region-name (get-map-prop map-state :curr-region)]
    (if-not (com/nil-or-empty? curr-region-name)
      (do (com/toggle-region-membership map-state (com/get-region-id map-state curr-region-name))
          (make-selected-regions-visible map-state)))))

(defn qrf-fn [map pt]
  (let [qrf (.queryRenderedFeatures map (clj->js pt) (js-obj "layers" (clj->js [map-layer])))]
    (if (= (count qrf) 0)
      nil
      qrf)))

(defn get-feat [map pt]
  "Retrieves the properties, such as county name from the given point
  on the map."
  (if-let [qrf (qrf-fn map pt)]
    (.-name (.-properties (first qrf)))
    nil))

(defn highlight-region-under-cursor [map-state county-name]
  (if-not (com/nil-or-empty? county-name)
    (let [map (get-map-prop map-state :map)]
      (set-layer-filter map "calif-counties-hover" ["==" "name" county-name])
      (set-no-hovers map))))

(defn data-under-cursor [map-state evt]
  (let [loc
        [(- (.-clientX evt) (get-in @map-state [:geography :mapbox :x-coord-origin]))
         (- (.-clientY evt) (get-in @map-state [:geography :mapbox :y-coord-origin]))]
        county-name (get-feat (get-map-prop map-state :map) loc)]
    (set-map-prop map-state :curr-region county-name)
    (highlight-region-under-cursor map-state county-name)
    (com/trace js/console (str loc (if county-name (str " : " county-name))))))

(defn load-map [map-state]
  (aset js/mapboxgl "accessToken" "pk.eyJ1IjoiZmVudG9udHJhdmVycyIsImEiOiJjamNpa3JobnozbnB6MnFsbG9sbmlhMzdrIn0.vh2s3V2spqauYIHskuyGuQ")
  (let [mapbox
        (js/mapboxgl.Map.
         (clj->js
          {:container "map"
           :zoom 5
           :center [-119.4179 37.5]
           :style "mapbox://styles/fentontravers/cjck1z0ca1tki2ss7dvud3bk6"}))]
    (set-map-prop map-state :map mapbox)
    (aset (.getCanvas mapbox) "style" "cursor" "default")
    (set-map-origin! map-state)
    (.on mapbox "style.load"
         (fn []
           (make-selected-regions-visible map-state)
           (set-no-hovers mapbox)))))

(defn the-map [map-state]
  (reagent/create-class
   {:component-did-mount (partial load-map map-state)
    :reagent-render
    (fn [map-state]
      [:div {:id "map"
             :on-mouse-move (partial data-under-cursor map-state)
             :on-mouse-up (partial toggle-select-region map-state)
             :style {"width" "500px"
                     "height" "600px"}}])}))
