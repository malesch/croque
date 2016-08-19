(defproject croque "0.1.0-SNAPSHOT"
  :description "Component wrapper for Chronical Queue"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha10"]
                 [com.stuartsierra/component "0.3.1"]
                 [com.taoensso/timbre "4.7.0"]
                 [net.openhft/chronicle-queue "4.5.12"]]
  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[org.clojure/tools.namespace "0.2.11"]]}})
