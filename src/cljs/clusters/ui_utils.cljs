(ns sample-reagent.ui-utils
  (:require [reagent.core :as reagent]))

(defn sub [db path]
  "db is reagent/atom"
  (reagent/cursor db path))
