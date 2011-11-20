(ns clojurescript-nodejs-demos.meter
  (:require [cljs.nodejs :as node]))

(def multimeter (node/require "multimeter"))

; ## Currently you will have to run 'reset' command to get sane terminal settings after running this
(defn start [& _]
  (let [multi (multimeter. node/process)]
    (. multi (on "^C"
                 #((. (.charm multi) (cursor true))
                   (.. multi (write "\n") (destroy))
                   (. node/process (exit)) )))
    (. (.charm multi) (cursor false))

    (. multi (drop (fn [bar] 
                     (def iv (js/setInterval 
                               #(let [p (. bar (percent))] 
                                  (. bar (percent (+ p 1))) 
                                  (if (>= p 100) 
                                    (do 
                                      (js/clearInterval. iv) 
                                      (. (.charm multi) (cursor true)) 
                                      (.. multi (write "\n") (destroy)))) )
                               25)))) )))

(set! *main-cli-fn* start)
