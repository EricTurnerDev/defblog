#!/usr/bin/env bb

(ns build
  (:require [babashka.fs :as fs]
            [clojure.tools.cli :as cli]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [hiccup2.core :as h]
            [file-date :as fdt]
            [script])
  (:import (java.time LocalDate)))

(def ^:const cli-options
  [[nil "--example" "Build site-example instead of site"]])
(def ^:const website-name "defblog")
(def ^:const default-title "A minimalistic static site generator for people who love Clojure")
(def ^:const publish-dir (fs/path "publish"))

;; -- Directory to read the website files from -------------------------------------------------------------------------

(defonce site-dir (atom (fs/path "site")))

(defn- configure!
  "Changes site-dir to `site`. Typically used when building or running the site example."
  [{:keys [site]}]
  (when site (reset! site-dir (fs/path site))))

(defn- get-site-dir
  []
  @site-dir)

;; -- Layout / templating ----------------------------------------------------------------------------------------------

(defn- load-partial [path]
  (let [site (get-site-dir)
        p (fs/path site "partials" path)]
    (when (fs/exists? p)
      (edn/read-string (slurp (str p))))))

(defn- site-head
  "Standard <head>. Title falls back to sensible default."
  [{:keys [title]}]
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
   [:title (if title (str website-name " — " title) default-title)]
   [:link {:rel "stylesheet" :href "/css/style.css"}]])

(defn- fallback-site-header
  "Top of page header/nav to use if the partial file doesn't exist."
  []
  [:header {:class "site-header"}
   [:div {:class "wrap"}
    [:a {:href "/"} "Home"]]])

(defn site-header-from-file []
  (or (load-partial "header.clj")
      (fallback-site-header)))

(defn- fallback-site-footer
  "Bottom of page footer to use if the partial file doesn't exist."
  []
  [:footer {:class "site-footer"}
   [:div {:class "wrap"}
    "© " (.getYear (LocalDate/now)) " — Built with Babashka"]])

(defn site-footer-from-file []
  (or (load-partial "footer.clj")
      (fallback-site-footer)))

(defn- layout
  "Default page layout. Expects `content` has hiccup and a context map (title, etc.)."
  [content ctx]
  [:html {:lang "en"}
   (site-head ctx)
   [:body
    (site-header-from-file)
    [:main {:class "container"} content]
    (site-footer-from-file)]])

(defn- bare-layout
  "Minimal layout if you need one (per-page override)."
  [content ctx]
  [:html {:lang "en"}
   (site-head ctx)
   [:body content]])

(def layout-table
  "Per-page layout overrides via ^{:layout :key} metadata."
  {:default layout
   :bar     bare-layout})

(defn- top-level-html?
  "Returns true if the hiccup looks like a full [:html ...] tree."
  [x]
  (and (vector? x) (= :html (first x))))

(defn- pick-layout
  "Choose a layout function from metadata (e.g. ^{:layout :bare})."
  [m]
  (get layout-table (or (:layout m) :default) layout))

(defn- wrap-page
  "If `hiccup-data` is a fragment, wrap with site layout using metadata ctx.
  If it's already [:html ...], leave it as-is."
  [hiccup-data meta*]
  (if (top-level-html? hiccup-data)
    hiccup-data
    ((pick-layout meta*) hiccup-data meta*)))

(defn- hiccup->html
  "Converts Hiccup (or EDN) file `f` into an html file at `out-path`.
  Supports custom readers for tag functions (e.g. #posts/list).
  Returns metadata from the Hiccup file."
  ([f out-path] (hiccup->html f out-path nil))
  ([f out-path readers]
   (try
     (let [hiccup-data (edn/read-string {:readers readers} (slurp (str f))) ; edn/read-string is safer than read-string (avoids code evaluation).
           meta* (or (meta hiccup-data) {})
           page (wrap-page hiccup-data meta*)
           html-output (str (h/html page))]
       (fs/create-dirs (fs/parent out-path))                ; Make sure the output directory exists.
       (spit (str out-path) html-output)
       meta*)
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
  "Process post from file f in the site directory to out-dir, and returns metadata about the post."
  [out-dir f]
  (let [in-dir (get-site-dir)
        in-file (str (fs/relativize in-dir f))              ; file path and name relative to the posts directory (e.g. example/20251020-example.clj)
        out-file (str (fs/strip-ext in-file) ".html")       ; file path and name relative to the publish directory (e.g. example/20251020-example.html)
        metadata (hiccup->html f (fs/path out-dir out-file)) ; metadata from the hiccup file after converting to html
        published (:published metadata)
        title (or (:title metadata) (nice-title in-file))
        date (or (:date metadata) (fdt/extract in-file))
        url (str (fs/path "posts" out-file))]
    {:title title :date date :url url :published published}))

(defn- process-posts
  "Process all posts from the site directory to out-dir. Returns a vector of metadata for the posts, sorted by most to least recent date."
  [out-dir]
  (let [in-dir (fs/path (get-site-dir) "posts")
        pattern "*.clj"
        files (fs/glob in-dir pattern)]                     ; Sort by file name to make the build more predictable (glob order isn't guaranteed).
    (->> files
         (map (partial process-post out-dir))
         (filter :published)
         (sort-by :date)
         (reverse)                                          ;; Newest filenames first if they're date prefixed
         (vec))))

(defn- friendly-date
  "Convert a YYYY-M-D string to a friendly 'Month D, YYYY' string."
  [date-str]
  (let [months ["January" "February" "March" "April" "May" "June"
                "July" "August" "September" "October" "November" "December"]
        [y m d] (map #(Integer/parseInt %) (str/split date-str #"-"))
        month-name (nth months (dec m))]
    (format "%s %d, %d" month-name d y)))

(defn- create-posts-list-reader
  "Creates the reader function used by the #posts/list custom EDN tag to generate an unordered list of posts."
  [posts-metadata]
  (fn [{:keys [ul-attrs item-attrs]}]
    (into
      [:ul (or ul-attrs {})]
      (for [{:keys [title date url]} posts-metadata]
        [:li (or item-attrs {})
         [:a {:href url} (str title " — " (friendly-date date))]]))))

(defn- process-index
  "Process index hiccup file from the site directory to out-dir."
  [out-dir posts-metadata]
  (let [index-clj (fs/path (get-site-dir) "index.clj")
        index-html (fs/path out-dir "index.html")
        ;; readers are used by custom EDN tags in the index.clj file (e.g. to create a list of posts).
        readers {'posts/list (create-posts-list-reader posts-metadata)}]
    (when (fs/exists? index-clj)
      (hiccup->html index-clj index-html readers))))

(defn- copy-dir
  [dir out-dir]
  (let [site-dir (get-site-dir)
        src-dir (fs/path site-dir dir)
        dest-dir (fs/path out-dir dir)]
    (when (fs/exists? src-dir)
      (fs/copy-tree src-dir dest-dir))))

(defn- process-css
  "Copies CSS files to out-dir"
  [out-dir]
  (copy-dir "css" out-dir))

(defn- process-images
  "Copies image files files to out-dir"
  [out-dir]
  (copy-dir "images" out-dir))

(defn- recreate-publish-dir!
  "Deletes the existing publish directory, and creates it (and subdirectories)."
  []
  (when (fs/exists? publish-dir)
    (fs/delete-tree publish-dir))
  (fs/create-dirs publish-dir)
  (fs/create-dirs (fs/path publish-dir "posts"))
  (fs/create-dirs (fs/path publish-dir "css"))
  (fs/create-dirs (fs/path publish-dir "images")))

(defn -main [& args]
  (recreate-publish-dir!)
  (let [parsed-opts (cli/parse-opts args cli-options)
        options (:options parsed-opts)]
    (if (:example options) (configure! {:site "site-example"}))
    (let [posts-metadata (process-posts (fs/path publish-dir "posts"))]
      (process-index publish-dir posts-metadata)
      (process-css publish-dir)
      (process-images publish-dir))))

(script/run -main *command-line-args*)