(ns cljs-demo.pizza
  (:use-macros [clojure.core.match.js :only [match]])
  (:require    [cljs.nodejs :as node]))

(def commander (node/require "commander"))

(defn has?
  [what]
  (= true (aget commander (name what))))

(doto commander
  (.version "0.0.1")
  (.option "-p, --peppers" "add peppers")
  (.option "-P, --pinapple" "add pinapple")
  (.option "-e, --pepperoni" "add pepperoni")
  (.option "-s, --sardines" "add sardines")
  (.parse process.argv))

(defn start [& _]
  (let [p (has? :peppers) 
        P (has? :pinapple)
        e (has? :pepperoni)
        s (has? :sardines)]
    (match  [p    P    e s    ]
            [true true _ _    ] (println "SWEET!")
            [_    _    _ false] (println "must have fishies!!!")
            [_    true _ true ] (println "REALLY?! are you crazy") )))

(set! *main-cli-fn* start)
