#!/usr/bin/env bb

(ns dev
  (:require [babashka.pods :as pods]
            [babashka.process :refer [process]]
            [clojure.java.io :as io]
            [clojure.tools.cli :as cli]
            [script]))

(def ^:const port 4000)
(def ^:const cli-options
  [[nil "--example" "Monitor and build site-example instead of site"]])

(pods/load-pod 'org.babashka/fswatcher "0.0.5")
(require '[pod.babashka.fswatcher :as fw])

(def pwd (System/getProperty "user.dir"))
(def page-to-open (io/file pwd "publish/index.html"))

;; -- Util functions ---------------------------------------------------------------------------------------------------

(defn run!
  "Run a command, inherit stdio, return exit code."
  [& args]
  (-> (process args {:inherit true}) deref :exit))

(defn spawn! [& args]
  (process args {:inherit true}))

;; -- Browser sync -----------------------------------------------------------------------------------------------------

(defn start-browser-sync!
  []
  ;; Requires Node installed; uses npx so you don’t need to install globally
  (spawn! "npx" "browser-sync" "start"
          "--server" "publish"
          "--files" "**/*"
          "--no-open"
          "--no-ui"
          "--port" port))

;; -- Watch for changes ------------------------------------------------------------------------------------------------

(defn change-handler
  [event]
  (when (= :write (:type event))
    (println "Detected write event")
    (run! "bb" "build-example")
    (println "Generated new site")))

(defn start-watch!
  [site on-change]
  (fw/watch site on-change {:recursive true}))

(defn -main
  [& args]

  (let [parsed-opts (cli/parse-opts args cli-options)
        options (:options parsed-opts)
        site (if (:example options) "site-example" "site")]
    (run! "bb" "build-example")                             ; Initial build
    (start-watch! site change-handler)
    (start-browser-sync!)
    )

  @(promise))

(script/run -main *command-line-args*)