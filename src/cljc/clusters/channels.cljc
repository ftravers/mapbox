(ns clusters.channels
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go go-loop]]))
  (:require #?(:cljs [cljs.core.async :as async]
               :clj [clojure.core.async :as async :refer [go go-loop]])))

(defn setup-channels [pub-fn]
  "The function creates three artifacts: 

1. write-to-chan: This is the channel to which you should write all
your messages into.

2. tap-me-mult: This is the mult that you should tap with any
additional channels that you'd like to create a clone of the
write-to-chan.

3. a-pub-queue: publication queue that is created conceptually on the
write-to-chan with the supplied pub-fn. 

This might be considered a lower level api with more flexibility than we need."
  (let [write-to-ch (async/chan)
        tap-me-mult (async/mult write-to-ch)
        pub-ch (async/chan)
        _ (async/tap tap-me-mult pub-ch)
        a-pub-queue (async/pub pub-ch pub-fn)]
    [write-to-ch tap-me-mult a-pub-queue]))

(defn pub-n-everything [pub-topic-fn]
  "Create a pub channel that we can subscribe to and return a
    channel that gets all the messages. 
* write-to-ch: send messages for broadcast here
* all-ch: a channel that all messages can be read from
* subscriber-fn: takes one val, the pub-topic-val, and returns a
  channel on which messages which match that value will be returned.

This is a narrower API for the specific use case of needing one
everything channel for logging, and a pub queue for signing
up (subscribing) for different message types."
  (let [[write-to-ch tap-me-mult pub-queue] (setup-channels pub-topic-fn)
        all-ch (async/chan)
        subscriber-fn #(let [sub-ch (async/chan)]
                         (async/sub pub-queue % sub-ch)
                         sub-ch)]
    (async/tap tap-me-mult all-ch)
    
    [write-to-ch all-ch pub-queue]))

(defn sub-to-pub [pub-fn-val a-pub-queue]
  "Given a publication queue, a-pub-queue, and a value, pub-fn-val,
  that the pub-fn can evaluate a message to, return a channel that
  those messages will be sent to."
  (let [output-ch (async/chan)]
    (async/sub a-pub-queue pub-fn-val output-ch)
    output-ch))

;; ---------- below not needed

(defn print-channel [label ch]
  "Print everything that comes over channel 'ch'"
  (go-loop []
    #?(:cljs (.log js/console label (str (async/<! ch)))
       :clj (println label (str (async/<! ch))))
    (recur)))

(defn send-msg [msg write-to-channel]
  "Send a message 'msg' on channel 'write-to-channel'."
  (go (async/>! write-to-channel msg)))
  
(defn send-some-testdata [input-ch]
  (send-msg "abc123" input-ch)
  (send-msg {:msg-type :dogs :disposition "friendly"} input-ch)
  (send-msg {:msg-type :cats :disposition "aloof"} input-ch))
  
(defn test-channels []
  (let [
        ;; create a publication-queue that calls the function
        ;; ':msg-type' on messages that get written to 'write-to-ch'
        [write-to-ch tap-me-mult msg-type-pub] (setup-channels :msg-type)

        ;; make a channel that receives messages that the following
        ;; expression evaluates to true for:
        ;;
        ;; (= :dogs (:msg-type msg))
        dogs-ch (sub-to-pub :dogs msg-type-pub)

        ;; make a channel that receives messages that the following
        ;; expression evaluates to true for:
        ;;
        ;; (= :cats (:msg-type msg))
        cats-ch (sub-to-pub :cats msg-type-pub)

        all-ch (async/chan)]

    ;; 'all-ch' will now receive all messages that get written to
    ;; 'write-to-ch'
    (async/tap tap-me-mult all-ch)

    (print-channel "Everything Channel: " all-ch)
    (print-channel "Dogs Channel: " dogs-ch)
    (print-channel "Cats Channel: " cats-ch)

    write-to-ch))

(comment (send-some-testdata (test-channels)))

(defn test-pne []
  (let [[input-ch all-out-ch pub-queue] (pub-n-everything :msg-type)
        dogs-ch (sub-to-pub :dogs pub-queue)
        cats-ch (sub-to-pub :cats pub-queue)]

    (print-channel "Everything Channel: " all-out-ch)
    (print-channel "Dogs Channel: " dogs-ch)
    (print-channel "Cats Channel: " cats-ch)

    input-ch))

(comment (send-some-testdata (test-pne)))

(defn two-pubs []
  (let [input-ch (async/chan)
        p2 (async/pub input-ch (constantly true))
        p1 (async/pub input-ch :msg-type)
        sub-ch (async/chan)
        all-ch (async/chan)]
    (async/sub p1 :dogs sub-ch)
    (async/sub p2 true all-ch)
    [input-ch sub-ch all-ch]))

(defn print-2-pubs []
  (let [[in-ch sub-ch all-ch] (two-pubs)]
    (print-channel "Dog Subscription: " sub-ch)
    (print-channel "Everything: " all-ch)
    in-ch))

(comment (send-some-testdata (print-2-pubs)))
