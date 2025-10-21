#!/usr/bin/env bb

(ns build
  (:require [babashka.fs :as fs]
            [clojure.tools.cli :as cli]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [hiccup2.core :as h]
            [script]))

(def ^:const cli-options
  [[nil "--example" "Build site-example instead of site"]])

(def ^:const site-dir (fs/path "site"))
(def ^:const site-example-dir (fs/path "site-example"))
(def ^:const publish-dir (fs/path "publish"))

(defn- hiccup->html!
  ([f out-path] (hiccup->html! f out-path nil))
  ([f out-path readers]
   (try
     (let [hiccup-data (edn/read-string {:readers readers} (slurp (str f))) ; edn/read-string is safer than read-string (avoids code evaluation).
           html-output (str (h/html hiccup-data))]
       (fs/create-dirs (fs/parent out-path))                ; Make sure the output directory exists.
       (spit (str out-path) html-output))
     (catch Exception e
       (binding [*out* *err*]
         (println "Failed to build" (str f) ":" (.getMessage e)))))))

(defn- nice-title
  "Derive a human-ish title from a filename like:
   2025-10-07-hello-world.clj  ->  \"Hello World\"
   hello_world.clj             ->  \"Hello World\""
  [rel]
  (let [base (-> (fs/file-name rel)
                 (str/replace #"\.clj$" "")
                 (str/replace #"^\d{4}-?\d{2}-?\d{2}-?" "")
                 (str/replace #"[._-]+" " "))
        words (remove str/blank? (str/split base #"\s+"))]
    (->> words (map #(str (str/upper-case (subs % 0 1)) (subs % 1))) (str/join " "))))

(defn- process-posts [in-dir out-dir]
  (let [pattern "*.clj"
        files (->> (fs/glob in-dir pattern)
                   (sort-by fs/file-name))]                 ; Sort by file name to make the build more predictable (glob order isn't guaranteed).
    (doseq [f files]
      (let [rel (str (fs/relativize in-dir f))
            out-rel (str (fs/strip-ext rel) ".html")
            out-rel-path (fs/path out-dir out-rel)]
        (hiccup->html! f out-rel-path)))
    (->> files
         (map (fn [f]
                (let [rel (str (fs/relativize in-dir f))
                      out-rel (str (fs/strip-ext rel) ".html")]
                  {:title (nice-title rel)
                   :url (str (fs/path "posts" out-rel))})))
         (reverse)                                          ;; Newest filenames first if they're date prefixed
         (vec))
    ))

(defn- process-css! [in-dir out-dir]
  (fs/copy-tree in-dir out-dir))

(defn- process-index! [in-dir out-dir posts]
  (let [index-clj (fs/path in-dir "index.clj")
        index-html (fs/path out-dir "index.html")
        readers {'posts/list
                 (fn [{:keys [ul-attrs item-attrs]}]
                   (into
                     [:ul (or ul-attrs {})]
                     (for [{:keys [title url]} posts]
                       [:li (or item-attrs {})
                        [:a {:href url} title]])))}]
    (hiccup->html! index-clj index-html readers)))

(defn- recreate-publish-dir! []
  ;; Delete and recreate the directories.
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
        site-css-dir (fs/path site "css")
        posts (process-posts site-posts-dir (fs/path publish-dir "posts"))]
    ;; Convert the hiccup files into html.
    (process-index! site publish-dir posts)
    (process-css! site-css-dir (fs/path publish-dir "css"))))

(script/run -main *command-line-args*)