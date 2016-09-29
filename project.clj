(defproject croque "0.2.0-SNAPSHOT"
  :description "Component wrapper for Chronical Queue"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[com.stuartsierra/component "0.3.1"]
                 [com.taoensso/nippy "2.12.1"]
                 ;; Logging
                 [com.taoensso/timbre "4.7.0"]
                 [com.fzakaria/slf4j-timbre "0.3.2"]
                 [org.slf4j/jul-to-slf4j "1.7.21"]
                 [org.slf4j/jcl-over-slf4j "1.7.21"]
                 [org.slf4j/log4j-over-slf4j "1.7.21"]
                 ;; Chronicle Queue
                 [net.openhft/chronicle-queue "4.5.15"]]
  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[org.clojure/clojure "1.9.0-alpha13"]
                                  [org.clojure/tools.namespace "0.2.11"]]}})
