#!/usr/bin/env bb

(ns dev
  (:require [babashka.pods :as pods]
            [babashka.process :refer [process]]
            [clojure.java.io :as io]
            [clojure.tools.cli :as cli]
            [script]))

(def ^:const cli-options
  [[nil "--example" "Monitor and build site-example instead of site"]])

(pods/load-pod 'org.babashka/fswatcher "0.0.5")
(require '[pod.babashka.fswatcher :as fw])

(def pwd (System/getProperty "user.dir"))
(def page-to-open (io/file pwd "publish/index.html"))

(defn run!
  "Run a command, inherit stdio, return exit code."
  [& args]
  (-> (process args {:inherit true}) deref :exit))

(defn refresh-browser
  []
  (run! "open" (str page-to-open))
  (println (str "Opened newly generated page " page-to-open)))

(defn change-handler
  [event]
  (when (= :write (:type event))
    (println "Detected write event")
    (run! "bb" "build-example")
    (println "Generated new site")
    (refresh-browser)))

(defn -main
  [& args]

  (let [parsed-opts (cli/parse-opts args cli-options)
        options (:options parsed-opts)
        site (if (:example options) "site-example" "site")]
    (fw/watch site change-handler {:recursive true}))

  @(promise))

(script/run -main *command-line-args*)