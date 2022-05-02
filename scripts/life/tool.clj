(ns life.tool
  (:require [hiccup.core :as h]
            [babashka.fs :as fs]))

(def root (System/getProperty "user.dir"))
(def index-file (fs/file root "target/public/index.html"))

(def html-template
  (str "<!doctype html>"
       (h/html
        [:html
         [:head
          [:meta {:charset "utf-8"}]
          [:meta {:http-equiv :x-ua-compatible :content :ie=edge}]
          [:title :life]
          [:meta {:name :description :content "The Game of Life"}]
          [:meta {:name :viewport :content "width=device-width, initial-scale=1"}]
          [:link {:rel :stylesheet :href "/css/core.css" :type "text/css" :media :screen}]]
         [:body
          [:div#root]
          [:script {:src "/js/core.js"}]]])))

(defn generate-html []
  (fs/create-file index-file)
  (spit index-file html-template))
