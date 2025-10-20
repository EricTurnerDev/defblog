#!/usr/bin/env bb

(ns build
  (:require [babashka.fs :as fs]
            [clojure.tools.cli :as cli]
            [clojure.edn :as edn]
            [hiccup2.core :as h]
            [script]))

(def ^:const cli-options
  [[nil "--example" "Build site-example instead of site"]])

(def ^:const site-dir (fs/path "site"))
(def ^:const site-example-dir (fs/path "site-example"))
(def ^:const publish-dir (fs/path "publish"))
(def ^:const publish-blog-dir (fs/path publish-dir "blog"))
(def ^:const publish-css-dir (fs/path publish-dir "css"))

(defn- hiccup->html! [f out-path]
  (try
    (let [hiccup-data (edn/read-string (slurp (str f)))     ; edn/read-string is safer than read-string (avoids code evaluation).
          html-output (str (h/html hiccup-data))]
      (fs/create-dirs (fs/parent out-path))                 ; Make sure the output directory exists.
      (spit (str out-path) html-output))
    (catch Exception e
      (binding [*out* *err*]
        (println "Failed to build" (str f) ":" (.getMessage e))))))

(defn- process-pages [in-dir out-dir & {:keys [recursive?] :or {recursive? false}}]
  (let [pattern (if recursive? "**.clj" "*.clj")
        files (->> (fs/glob in-dir pattern)
                   (sort-by fs/file-name))]                 ; Sort by file name to make the build more predictable (glob order isn't guaranteed).
    (doseq [f files]
      (let [rel (str (fs/relativize in-dir f))
            out-rel (str (fs/strip-ext rel) ".html")
            out-rel-path (fs/path out-dir out-rel)]
        (hiccup->html! f out-rel-path)))))

(defn- process-css! [in-dir out-dir]
  (fs/copy-tree in-dir out-dir))

(defn -main [& args]
  ;; Clean and recreate the directories.
  (when (fs/exists? publish-dir)
    (fs/delete-tree publish-dir))
  (fs/create-dirs publish-dir)
  (fs/create-dirs publish-blog-dir)
  (fs/create-dirs publish-css-dir)

  (let [parsed-opts (cli/parse-opts args cli-options)
        options (:options parsed-opts)
        site (if (:example options) site-example-dir site-dir)
        blog (fs/path site "blog")
        css (fs/path site "css")]
    ;; Convert the hiccup files into html.
    (process-pages blog publish-blog-dir :recursive? true)
    (process-pages site publish-dir)
    (process-css! css publish-css-dir)))

(script/run -main *command-line-args*)