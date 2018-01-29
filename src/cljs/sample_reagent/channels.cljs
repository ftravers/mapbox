(ns sample-reagent.channels
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]])
  (:require
   [cljs.core.async :as async :refer [put! chan <! >! timeout close! mult tap]]))

(def input-channel (chan))

(defn print-chan [label ch]
  (go-loop []
    (.log js/console label (str (<! ch)))
    (recur)))

(defn duplicate-output-channel []
  (let [m (mult input-channel)
        c1 (chan)
        c2 (chan)]
    (tap m c1)
    (tap m c2)
    [c1 c2]))

(defn test-duplicate-output-channel []
  (let [[ch1 ch2] (duplicate-output-channel)]
    (print-chan "channel 1" ch1 )
    (print-chan "channel 2" ch2 ))
  (go
    (dotimes [n 10]
      (>! input-channel (str "Blah: " n)))))

(defn pub-sub [pub-ch msg-type]
  (let [pub-queue (async/pub pub-ch :msg-type)
        cats-ch (chan)]
    (async/sub pub-queue msg-type cats-ch)
    cats-ch))

(defn feed [msg]
  (go (>! input-channel msg)))

(defn read-evry [ch]
  (go-loop []
    (.log js/console "everything channel: " (str (<! ch)))
    (recur)))

(defn test-pub-sub []
  (let [[pub-ch everything-ch] (duplicate-output-channel)
        cats-ch (pub-sub pub-ch :cats)]
    (print-chan "cats channel: " cats-ch)
    everything-ch
    ))

(defn gg []
  (feed "abc123")
  (feed {:msg-type :dogs :disposition "friendly"})
  (feed {:msg-type :cats :disposition "aloof"})
  (dotimes [n 5]
    (feed (str "Blah: " n))))
