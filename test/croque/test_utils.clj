(ns croque.test-utils
  (:import [java.util UUID]))

(defmacro ^{:private true} assert-args
  [& pairs]
  `(do (when-not ~(first pairs)
         (throw (IllegalArgumentException.
                  (str (first ~'&form) " requires " ~(second pairs) " in " ~'*ns* ":" (:line (meta ~'&form))))))
       ~(let [more (nnext pairs)]
          (when more
            (list* `assert-args more)))))

(defmacro with-components
  "Create component bindings which are started and finally stopped again.
  In order to use this macro, the namespace of Stuarts components library
  must of course be present (mainly copied from the `with-open` macro)."
  [bindings & body]
  (assert-args
    (vector? bindings) "a vector for its binding"
    (even? (count bindings)) "an even number of forms in binding vector")
  (cond
    (= (count bindings) 0) `(do ~@body)
    (symbol? (bindings 0)) `(let [~(bindings 0)
                                  (com.stuartsierra.component/start ~(bindings 1))]
                              (try
                                (with-components ~(subvec bindings 2) ~@body)
                                (finally
                                  (com.stuartsierra.component/stop ~(bindings 0)))))
    :else (throw (IllegalArgumentException.
                   "with-components only allows Symbols in bindings"))))


(defn random-path
  "Create a random path under `target` for the test queue"
  []
  (str "./target/test-" (UUID/randomUUID)))