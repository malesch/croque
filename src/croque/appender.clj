(ns croque.appender)

(defprotocol Appender
  (append! [this x]
    "Append data to the queue")
  (state [this]
    "Return a map with state information on the appender instance"))

