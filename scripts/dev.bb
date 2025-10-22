#!/usr/bin/env bb

(ns dev
  (:require [babashka.pods :as pods]
            [babashka.process :refer [process]]
            [clojure.tools.cli :as cli]
            [script]))

;; -- Configuration ----------------------------------------------------------------------------------------------------

(def ^:const port 4000)
(def ^:const cli-options
  [["-e" "--example" "Monitor and build site-example instead of site"]])

(pods/load-pod 'org.babashka/fswatcher "0.0.5")
(require '[pod.babashka.fswatcher :as fw])

;; -- Utility functions ------------------------------------------------------------------------------------------------

(defn run!
  "Run a command, wait for it to finish, and return the exit code. Blocking, one-shot."
  [& args]
  (-> (process args {:inherit true})
      deref                                                 ; Wait for the process to exit.
      :exit                                                 ; Extract the exit code.
      ))

(defn spawn!
  "Launch a long-running command (e.g. server) and keep it alive in parallel. Non-blocking, long-running."
  [& args]
  ;; Returns immediately with a handle to the subprocess.
  (process args {:inherit true}))

;; -- Browser sync functions -------------------------------------------------------------------------------------------

(defn start-browser-sync!
  []
  ;; Requires Node installed; uses npx so you don’t need to install globally
  (spawn! "npx" "browser-sync" "start"
          "--server" "publish"
          "--files" "**/*"
          "--no-open"
          "--no-ui"
          "--port" port))

;; -- Functions for watching for changes -------------------------------------------------------------------------------

(defn create-change-handler
  "Creates an event handler that calls on-change."
  [on-change]
  (fn
    [event]
    (when (= :write (:type event))
      (println "A change was detected in: " (:path event))
      (on-change)
      (println "Update complete"))))

(defn start-watch!
  [site on-change]
  (fw/watch site on-change {:recursive true}))

(defn -main
  [& args]
  (let [parsed-opts (cli/parse-opts args cli-options)
        options (:options parsed-opts)
        site (if (:example options) "site-example" "site")
        build-fn (if (:example options) #(run! "bb" "build-example") #(run! "bb" "build"))
        on-change (create-change-handler build-fn)]
    (build-fn)                                              ; Initial build
    (start-watch! site on-change)
    (start-browser-sync!))
  ;; Prevent -main from exiting.
  @(promise))

(script/run -main *command-line-args*)