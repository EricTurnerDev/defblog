#!/usr/bin/env bb

(ns build
  (:require [babashka.fs :as fs]
            [clojure.edn :as edn]
            [hiccup2.core :as h]))

(def site-dir (fs/path "site"))
(def site-blog-dir (fs/path "site" "blog"))
(def publish-dir (fs/path "publish"))
(def publish-blog-dir (fs/path "publish" "blog"))

;; Clean and recreate publish/
(when (fs/exists? publish-dir)
  (fs/delete-tree publish-dir))
(fs/create-dirs publish-dir)
(fs/create-dirs publish-blog-dir)

(defn hiccup->html! [f out-path]
  (try
    (let [hiccup-data (edn/read-string (slurp (str f)))     ; edn/read-string is safer than read-string (avoids code evaluation).
          html-output (str (h/html hiccup-data))]
      (fs/create-dirs (fs/parent out-path))                 ; Make sure the output directory exists.
      (spit (str out-path) html-output))
    (catch Exception e
      (binding [*out* *err*]
        (println "Failed to build" (str f) ":" (.getMessage e))))))

(defn process-pages [in-dir out-dir & {:keys [recursive?] :or {recursive? false}}]
  (let [pattern (if recursive? "**.clj" "*.clj")
        files (->> (fs/glob in-dir pattern)
                   (sort-by fs/file-name))]                 ; Sort by file name to make the build more predictable (glob order isn't guaranteed).
    (doseq [f files]
      (let [rel (str (fs/relativize in-dir f))
            out-rel (str (fs/strip-ext rel) ".html")
            out-rel-path (fs/path out-dir out-rel)]
        (hiccup->html! f out-rel-path)))))

(process-pages site-blog-dir publish-blog-dir :recursive? true)
(process-pages site-dir publish-dir)
