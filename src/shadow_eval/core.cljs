(ns shadow-eval.core
  (:require
    [cljs.js :as cljs]
    [shadow.cljs.bootstrap.browser :as shadow.bootstrap]
    [shadow-eval.queue :as queue]
    ))

(defonce c-state (cljs/empty-state))
(defonce !eval-ready? (atom false))

(defn eval* [source cb]
  (let [options 
        {:eval cljs/js-eval
         ;; use the :load function provided by shadow-cljs,
         ;; which uses the bootstrap build's index.transit.json
         ;; file to map namespaces to files.
         :load (partial shadow.bootstrap/load c-state)
         :context :expr}
        f (fn [x] (when (:error x)
                     (js/console.error (ex-cause (:error x))))
             (tap> x) (cb x))]
    (cljs/eval-str c-state (str source) "[test]" options f)))

(defonce ^:export evalstar eval*)

(defonce eval-queue (new queue/FunctionQueue #queue[] false))

(defn eval! [source cb]
  (queue/conj! eval-queue
               (fn [done]
                 (eval* source
                        (fn [result]
                              (cb result)
                              (done))))))

(defn ^:export cljeval [source cb]
  (queue/conj! eval-queue
               (fn [done]
                 (eval* source
                        (fn [result]
                              (cb result)
                              (done))))))

(comment
  (tap> c-state))

(defn ^:dev/after-load init []
  (js/console.log "fuckthat")
  (shadow.bootstrap/init c-state
                         {:path "/js/bootstrap"
                          :load-on-init '#{shadow-eval.user}}
                         #(reset! !eval-ready? true)))

(js/console.log "fuckit")



