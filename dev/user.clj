(ns user
  (:require [com.stuartsierra.component :as component]
            [clojure.java.javadoc :refer [javadoc]]
            [clojure.pprint :refer [pprint]]
            [clojure.reflect :refer [reflect]]
            [clojure.repl :refer [apropos dir doc find-doc pst source]]
            [clojure.tools.namespace.repl :refer [refresh refresh-all]]
            [croque.core :as core]))

(def system (core/create-croque-system {}))

(defn start
  "Starts the system."
  []
  (alter-var-root #'system component/start))

(defn stop
  "Stops the system."
  []
  (alter-var-root #'system component/stop))

(defn go
  "Starts the system."
  []
  (start)
  :ready)

(defn reset
  "Stops the system, reloads modified source files, and restarts it."
  []
  (stop)
  (refresh :after `go))
