(ns cljs-demo.getit
  (:use-macros [clojure.core.match.js :only [match]])
  (:require    [goog.dom :as dom]
                [cljs.nodejs :as node]))

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

(def page-data (atom []))

(defn list-handler [val]
  (. val (split ",")))

(doto commander
  (.version "0.0.1")
  (.usage "[options] <url>")
  (.option "-o, --outfile [type]" "the file to download to")
  (.option "-r, --showresources <items>" "a comma separated list of tags to parse for" list-handler)
  (.parse process.argv))

(defn download-file
  [url]
    (let [params (url-parse url)
          req (. http (request (clj->js params)
             (fn [res]
               (doto res
                 (.setEncoding "utf8")
                 (.on "data" (fn [chunk]
                                 (swap! page-data assoc (count @page-data) chunk)))
                 (.on "end" (fn [] 
                              (if (or (not    (.outfile commander))
                                      (not (= (.outfile commander) "-")))
                                (let [parts (js->clj (.split (:path params) "/"))
                                      file-name (if (.outfile commander)
                                                    (.outfile commander)
                                                    (first (reverse parts)))]
                                  (. fs (writeFileSync file-name (apply str @page-data) )))
                                ; intended that passing -o - would print to stdout, but commander can't handle this
                                (prn (apply str @page-data)) )
                              (if (.showresources commander)
                                  (let [window (.. jsdom (jsdom (apply str @page-data)) (createWindow))
                                        tag-list (js->clj (.showresources commander))]
                                    ;; ref for trying to get goog.dom parsing in nodejs
                                    ;(. goog.dom (setDocument (.document window)))
                                    ;anchors (dom/getElementsByTagNameAndClass "a")]
                                    (doseq [tag tag-list]
                                      (let [anchors (.. window document (getElementsByTagName tag))]
                                        (prn (str "--- Showing: " tag " ---"))
                                        (doseq [i (range (.length anchors))]
                                          (let [elem (. anchors (item i))
                                                info (cond (= "a" tag) (str ": " (. elem (getAttribute "href")))
                                                           (= "img" tag) (str ": " (. elem (getAttribute "src")))
                                                           :else "") ]
                                            ;; NOTE: this currently throws errors if the innerHTML is not plain textnode
                                            (prn (str (.innerHTML elem) info)))) )) ))
                              ))) )))]
        (doto req
          (.write "data\n")
          (.write "data\n")
          (.end) ) ) )
  
; '(:link :a :img :script :style)

(defn start [& _]
  (let [url-list (js->clj (.args commander))]
    (if (url-list 0)
      (download-file (url-list 0) ))))

(set! *main-cli-fn* start)
