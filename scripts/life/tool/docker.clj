(ns life.tool.docker
  (:require [babashka.process :as p]
            [clojure.string :as str]))

(defn build-number
  []
  (-> (p/process '[git rev-list HEAD --count]) :out slurp str/trim))

(defn build [name]
  (-> ['docker 'build '-t (format "reg.cork.lan/zehcnas34/%s:%s" name (build-number)) "."]
      (p/process {:inherit true :shutdown p/destroy-tree})
      (deref)))

(defn push [name]
  (-> ['docker 'push (format "reg.cork.lan/zehcnas34/%s:%s" name (build-number))]
      (p/process {:inherit true :shutdown p/destroy-tree})
      (deref)))
