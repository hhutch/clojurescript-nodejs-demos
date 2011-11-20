(ns cljs-demo.getit
  (:use-macros [clojure.core.match.js :only [match]])
  (:require    [cljs.nodejs :as node]))

;; ## This application does not compile under :advanced

;; ## All of these libraries are available via npm
(def commander (node/require "commander"))
(def http (node/require "http"))
(def jsdom (node/require "jsdom"))
(def url (node/require "url"))
(def fs (node/require "fs"))

;; Borrowed from http://mmcgrana.github.com/2011/09/clojurescript-nodejs.html
(defn clj->js
  "Recursively transforms ClojureScript maps into Javascript objects,
   other ClojureScript colls into JavaScript arrays, and ClojureScript
   keywords into JavaScript strings."
  [x]
  (cond
    (string? x) x
    (keyword? x) (name x)
    (map? x) (.strobj (reduce (fn [m [k v]]
               (assoc m (clj->js k) (clj->js v))) {} x))
    (coll? x) (apply array (map clj->js x))
    :else x))

;; Borrowed from http://mmcgrana.github.com/2011/09/clojurescript-nodejs.html
(defn url-parse
  "Returns a map with parsed data for the given URL."
  [u]
  (let [raw (js->clj (.parse url u))]
    {:protocol (.substr (get raw "protocol")
                        0 (dec (.length (get raw "protocol"))))
     :host (get raw "hostname")
     :port (js/parseInt (get raw "port"))
     :method "GET"      ;; this is ghetto
     :path (get raw "pathname")}))

(doto commander
  (.version "0.0.1")
  (.option "-u, --url [type]" "URL to fetch")
  (.option "-r, --recursive" "Recursively fetch")
  (.option "-a, --fetchall" "fetchall dependencies")
  (.parse process.argv))

(def page-data (atom []))

(defn start [& _]
  (if (.url commander)
    (let [params (url-parse (.url commander))
          req (. http (request (clj->js params)
             (fn [res]
               (doto res
                 (.setEncoding "utf8")
                 (.on "data" (fn [chunk]
                                 (swap! page-data assoc (count @page-data) chunk)))
                 (.on "end" (fn [] 
                                (let [parts (js->clj (.split (:path params) "/"))
                                      file-name (first (reverse parts))]
                                  (. fs (writeFileSync file-name (apply str @page-data) )))
                                (if (.recursive commander)
                                    (let [window (.. jsdom (jsdom (apply str @page-data)) (createWindow))
                                          anchors (.. window document (getElementsByTagName "a"))]
                                      (doseq [i (range (.length anchors))]
                                        (let [tag (. anchors (item i))]
                                          (println (str (.innerHTML tag) ": " (. tag (getAttribute "href")))))) )) ))) )))]
        (doto req
          (.write "data\n")
          (.write "data\n")
          (.end) ) )))

(set! *main-cli-fn* start)
