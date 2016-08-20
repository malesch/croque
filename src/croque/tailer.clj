(ns croque.tailer)

(defprotocol Tailer
  (peek [this]
    "Return the queue value from the current read position (sequence) and move to next position")
  (rewind! [this n]
    "Rewind the current read position by n steps")
  (seek! [this pos]
    "Set sequence position to a specific position")
  (state [this]
    "Return a map with state information on the tailer instance"))
