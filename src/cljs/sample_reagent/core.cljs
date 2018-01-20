(ns sample-reagent.core
  (:require
   [reagent.core :as reagent]))

(def curr-region (atom nil))
(def selected-regions (atom #{}))
(def mp (atom nil))
(def feat (reagent/atom nil))
(def event (atom nil))
(def layers (js-obj "layers" (clj->js ["california-counties"])))
(def pt (atom nil))
(def map-origin (atom nil))
(defn obj-keys [obj]
  (sort (js->clj (.keys js/Object obj))))

(defn set-map-origin! []
  (let [rect (.getBoundingClientRect (.getElementById js/document "map"))]
    (reset! map-origin {:x (.-left rect) :y (.-top rect)})))

(defn set-layer-filter [layer filter]
  (.setFilter @mp layer (clj->js filter)))

(defn set-selected-regions []
  (set-layer-filter "california-counties-selected"
                    (concat ["in" "region"] (into [] @selected-regions))))

(defn set-no-hovers []
  (set-layer-filter "california-counties-hover" ["==" "region" ""]))

(defn load-map []
  (aset js/mapboxgl "accessToken" "pk.eyJ1IjoiZmVudG9udHJhdmVycyIsImEiOiJjamNpa3JobnozbnB6MnFsbG9sbmlhMzdrIn0.vh2s3V2spqauYIHskuyGuQ")
  (reset! mp (js/mapboxgl.Map. (clj->js {:container "map"
                                         :style "mapbox://styles/fentontravers/cjck1z0ca1tki2ss7dvud3bk6"})))
  (aset (.getCanvas @mp) "style" "cursor" "default")
  (set-map-origin!)

  ;; (.setFilter @mp "california-counties-selected" (clj->js (concat ["in" "region"] [])))

  (.on @mp "style.load" (comp set-selected-regions set-no-hovers))

  )

(defn qrf-fn [pt]
  (let [qrf (.queryRenderedFeatures @mp (clj->js pt) layers)]
    (if (= (count qrf) 0)
      nil
      qrf)))

(defn get-feat [pt]
  "where pt = [34 2]"
  (if-let [qrf (qrf-fn pt)]
    (.-region (.-properties (first qrf)))
    nil))

(defn toggle-select-region []
  (.log js/console "toggling selected region")
  (reset!
   selected-regions
   (if (contains? @selected-regions @curr-region)
     (remove #{@curr-region} @selected-regions)
     (set (conj @selected-regions @curr-region))))
  (set-selected-regions)
  (.log js/console "REGIONS: " (str  @selected-regions))
  )

(defn data-under-cursor [evt]
  (let [loc
        [(- (.-clientX evt) (:x @map-origin))
         (- (.-clientY evt) (:y @map-origin))]
        qrf (get-feat loc)]
    (reset! curr-region qrf)
    (if qrf
      (set-layer-filter "california-counties-hover" ["==" "region" qrf])
      (set-no-hovers))
    (.log js/console (str loc (if qrf (str " : " qrf))))))

(defn the-map []
  (reagent/create-class
   {:component-did-mount load-map
    :reagent-render
    (fn []
      
      [:div {:id "map"
             :on-mouse-move data-under-cursor
             :on-mouse-up toggle-select-region
             :style {"width" "800px"
                     "height" "800px"}}])}))

(comment
  ;; nothing at this point
  (get-feat [782 659])

  ;; find a point that actually returns something...
  (get-feat [460 650])

  ;; highlights 'nocal' and 'socal'
  
  
  (.persist evt)
  (reset! pt (.-point evt))
  (reset! event evt))

(defn some-component [ratom]
  [:div (:text @ratom)])

(defn page [state]
  [:table
   [:tbody
    [:tr
     [:td [some-component state]]
     ;; [mapbox state]
     [:td [the-map]]]]
   ])

(defonce app-state
  (reagent/atom {:text "Hello, what is your name??"}))

(defn on-js-reload []
  (reagent/render [page app-state]
                  (.getElementById js/document "app")))

(defn ^:export main []
  (on-js-reload))

(on-js-reload)
