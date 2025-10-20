#!/usr/bin/env bb

(ns gen
  (:require [hiccup2.core :as h]))

(def hiccup-data (read-string (slurp "site/index.clj")))

(def html-output (str (h/html hiccup-data)))

(spit "publish/index.html" html-output)
