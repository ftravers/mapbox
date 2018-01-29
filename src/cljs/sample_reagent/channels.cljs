(ns sample-reagent.channels
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require
   [cljs.core.async :as async]))

(defn setup-channels [pub-fn]
  "The function creates three artifacts: 

1. write-to-chan: This is the channel to which you should write all
your messages into.

2. tap-me-mult: This is the mult that you should tap with any
additional channels that you'd like to create a clone of the
write-to-chan.

3. a-pub-queue: publication queue that is created conceptually on the
write-to-chan with the supplied pub-fn. "
  (let [write-to-ch (async/chan)
        tap-me-mult (async/mult write-to-ch)
        pub-ch (async/chan)
        _ (async/tap tap-me-mult pub-ch)
        a-pub-queue (async/pub pub-ch pub-fn)]
    [write-to-ch tap-me-mult a-pub-queue]))

(defn sub-to-pub [pub-fn-val a-pub-queue]
  "Given a publication queue, a-pub-queue, and a value, pub-fn-val,
  that the pub-fn can evaluate a message to, return a channel that
  those messages will be sent to."
  (let [output-ch (async/chan)]
    (async/sub a-pub-queue pub-fn-val output-ch)
    output-ch))

(comment
  (defn print-channel [label ch]
    "Print everything that comes over channel 'ch'"
    (go-loop []
      (.log js/console label (str (async/<! ch)))
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

  (def write-ch (test-channels))

  (send-some-testdata write-ch))
