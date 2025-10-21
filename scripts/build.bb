#!/usr/bin/env bb

(ns build
  (:require [babashka.fs :as fs]
            [clojure.tools.cli :as cli]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [hiccup2.core :as h]
            [file-date :as fdt]
            [script]))

(def ^:const cli-options
  [[nil "--example" "Build site-example instead of site"]])

(def ^:const site-dir (fs/path "site"))
(def ^:const site-example-dir (fs/path "site-example"))
(def ^:const publish-dir (fs/path "publish"))

(defn- hiccup->html
  "Converts Hiccup file f into an html file at out-path. Returns metadata from the Hiccup file."
  ([f out-path] (hiccup->html f out-path nil))
  ([f out-path readers]
   (try
     (let [hiccup-data (edn/read-string {:readers readers} (slurp (str f))) ; edn/read-string is safer than read-string (avoids code evaluation).
           metadata (meta hiccup-data)
           html-output (str (h/html hiccup-data))]
       (fs/create-dirs (fs/parent out-path))                ; Make sure the output directory exists.
       (spit (str out-path) html-output)
       (or metadata {}))
     (catch Exception e
       (binding [*out* *err*]
         (println "Failed to build" (str f) ":" (.getMessage e)))))))

(defn- nice-title
  "Derive a human-ish title from a filename like:
   2025-10-07-hello-world.clj  ->  \"Hello World\"
   hello_world.clj             ->  \"Hello World\""
  [rel]
  (let [base (-> (fs/file-name rel)
                 (str/replace #"\.[^.]+(\.[^.]+)*$" "")     ; Match regular extensions (e.g. .clj) and multi-part extensions (e.g. .tar.gz).
                 (str/replace #"^\d{4}-?\d{2}-?\d{2}-?" "")
                 (str/replace #"[._-]+" " "))
        words (remove str/blank? (str/split base #"\s+"))]
    (->> words (map #(str (str/upper-case (subs % 0 1)) (subs % 1))) (str/join " "))))

(defn- process-post
  "Process post from file f in in-dir to out-dir, and returns metadata about the post."
  [in-dir out-dir f]
  (let [in-file (str (fs/relativize in-dir f))              ; file path and name relative to the posts directory (e.g. example/20251020-example.clj)
        out-file (str (fs/strip-ext in-file) ".html")       ; file path and name relative to the publish directory (e.g. example/20251020-example.html)
        metadata (hiccup->html f (fs/path out-dir out-file)) ; metadata from the hiccup file after converting to html
        published (:published metadata)
        title (or (:title metadata) (nice-title in-file))
        date (or (:date metadata) (fdt/extract in-file))
        url (str (fs/path "posts" out-file))]
    {:title title :date date :url url :published published}))

(defn- process-posts
  "Process all posts from in-dir to out-dir. Returns a vector of metadata for the posts, sorted by most to least recent date."
  [in-dir out-dir]
  (let [pattern "*.clj"
        files (fs/glob in-dir pattern)]                 ; Sort by file name to make the build more predictable (glob order isn't guaranteed).
    (->> files
         (map (partial process-post in-dir out-dir))
         (filter :published)
         (sort-by :date)
         (reverse)                                          ;; Newest filenames first if they're date prefixed
         (vec))))

(defn- create-posts-list-reader
  "Creates the reader function used by the #posts/list custom EDN tag to generate an unordered list of posts."
  [posts-metadata]
  (fn [{:keys [ul-attrs item-attrs]}]
    (into
      [:ul (or ul-attrs {})]
      (for [{:keys [title date url]} posts-metadata]
        [:li (or item-attrs {})
         [:a {:href url} (str date  " — " title)]]))))

(defn- process-index
  "Process index hiccup file from in-dir to out-dir."
  [in-dir out-dir posts-metadata]
  (let [index-clj (fs/path in-dir "index.clj")
        index-html (fs/path out-dir "index.html")
        ;; readers are used by custom EDN tags in the index.clj file (e.g. to create a list of posts).
        readers {'posts/list (create-posts-list-reader posts-metadata)}]
    (hiccup->html index-clj index-html readers)))

(defn- recreate-publish-dir!
  "Deletes the existing publish directory, and creates it (and subdirectories)."
  []
  (when (fs/exists? publish-dir)
    (fs/delete-tree publish-dir))
  (fs/create-dirs publish-dir)
  (fs/create-dirs (fs/path publish-dir "posts"))
  (fs/create-dirs (fs/path publish-dir "css")))

(defn -main [& args]
  (recreate-publish-dir!)
  (let [parsed-opts (cli/parse-opts args cli-options)
        options (:options parsed-opts)
        site (if (:example options) site-example-dir site-dir)
        site-posts-dir (fs/path site "posts")
        posts-metadata (process-posts site-posts-dir (fs/path publish-dir "posts"))]
    (process-index site publish-dir posts-metadata)
    (fs/copy-tree (fs/path site "css") (fs/path publish-dir "css"))))

(script/run -main *command-line-args*)