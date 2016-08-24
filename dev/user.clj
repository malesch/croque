(ns user
  (:use [croque.core])
  (:require [com.stuartsierra.component :as component]
            [clojure.java.javadoc :refer [javadoc]]
            [clojure.pprint :refer [pprint]]
            [clojure.reflect :refer [reflect]]
            [clojure.repl :refer [apropos dir doc find-doc pst source]]
            [clojure.tools.namespace.repl :refer [refresh refresh-all]]))

(def dev-config {:path "./data"
                 :roll-cycle :TEST_HOURLY
                 :retain-cycles 10})

(def croque (new-croque-queue dev-config))

(defn start
  "Starts the Croque queue component."
  []
  (alter-var-root #'croque component/start))

(defn stop
  "Stops the component."
  []
  (alter-var-root #'croque component/stop))

(defn go
  "Starts the component."
  []
  (start)
  :ready)

(defn reset
  "Stops the system, reloads modified source files, and restarts it."
  []
  (stop)
  (refresh :after `go))
