(ns file-date
  (:require [babashka.fs :as fs])
  (:import [java.nio.file Files LinkOption]
           [java.nio.file.attribute BasicFileAttributes]
           [java.time Instant LocalDate ZoneId]))

(def ^:private dashed-re  #"(?<!\d)(\d{4})-(\d{2})-(\d{2})(?!\d)")
(def ^:private compact-re #"(?<!\d)(\d{4})(\d{2})(\d{2})(?!\d)")

(defn- match->local-date [m]
  (try
    (LocalDate/of (Integer/parseInt (nth m 1))
                  (Integer/parseInt (nth m 2))
                  (Integer/parseInt (nth m 3)))
    (catch Exception _ nil)))

(defn- date-in-string->local-date [s]
  (when s
    (or (some-> (re-find dashed-re s) match->local-date)
        (some-> (re-find compact-re s) match->local-date))))

(defn- file-birthdate->local-date [p]
  (let [path  (fs/path p)
        attrs (Files/readAttributes path BasicFileAttributes (into-array LinkOption []))
        file-time (or (.creationTime attrs) (.lastModifiedTime attrs))
        instant (Instant/ofEpochMilli (.toMillis file-time))]
    (.toLocalDate (.atZone instant (ZoneId/systemDefault)))))

(defn extract
  "Given a path like foo/bar/2025-10-20-example.clj or foo/bar/20251020-example.clj,
 returns YYYY-MM-DD from the file name, or the file's creation/modified date if a date is not part of the file name."
  [relative-path]
  (let [s (str relative-path)]
    (if-let [ld (date-in-string->local-date s)]
      (str ld)
      (when (fs/exists? relative-path)
        (str (file-birthdate->local-date relative-path))))))
