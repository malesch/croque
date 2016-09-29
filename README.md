# croque

An experimental implementation of a durable queue [component] (https://github.com/stuartsierra/component), i.e. a queue where entries can be replayed from a
previous point in time (if not intentionally expunged). This is simply a wrapper around the [Chronical Queue] (http://chronicle.software/products/chronicle-queue/) and only
exposes some basic functionality required for evaluating its usage as an [Event Storage] (http://martinfowler.com/eaaDev/EventSourcing.html) replacement for [Kafka]
(http://kafka.apache.org/) in an embedded application scenario.


[![croque version](http://clojars.org/croque/latest-version.svg)](http://clojars.org/croque)


## Configuration

The queue is configured by starting the component with a configuration map.

Sample configuration with all available options:
```clj
{
 ;; Path on the file system where the queue information is persisted.
 ;; This is a mandatory parameter.
 :path "./data"

 ;; Optional parameter to control the rolling period i.e. when new data files are created.
 ;; The default value is :DAILY what results in a new data files created every day.
 ;; Other possible values are amongst others: :TEST_SECONDLY, :MINUTELY, :HOURLY.
 ;; See also:
 ;; https://github.com/OpenHFT/Chronicle-Queue/blob/HEAD/src/main/java/net/openhft/chronicle/queue/RollCycles.java
 :roll-cycle :DAILY

 ;; Optional parameter to specify the number data files to be retained. Older cycle files are deleted.
 ;; If this option is not specified, no persisted data is deleted and the history is kept forever
 ;; (as long there is disk space). Thus, the duration how long back in time the queue can replayed  
 ;; depends on the number cycles retained and what the roll-cycle period is configured.
 ;; Example: If the roll cycle is set to :DAILY and :retain-cycles has the value 10, the queue will keep
 ;; entries from the last 10 days.
 :retain-cycles 10
}
```

## Usage

```clj
;; Get some dependencies
> (require '[com.stuartsierra.component :as component])
> (require '[croque.core :as croque])

;; Create and start a CroqueQueue component, persisting the queue entries
;; under the local directory './data'
> (def queue (component/start (croque/new-croque-queue {:path "./data"})))

;; Add some example entries to the queue
;; Every call will return the index position of the inserted entry
> (croque/append-entry! queue [:example {:entry 0}])
> (croque/append-entry! queue [:example {:entry 1}])
> (croque/append-entry! queue [:example {:entry 2}])

;; Read entries from the queue
> (croque/next-entry queue)
;;=> [:example {:entry 0}]
> (croque/next-entry queue)
;;=> [:example {:entry 1}]
> (croque/next-entry queue)
;;=> [:example {:entry 2}]

;; Get the current read position (index)
;; The returned index number will be different when you execute this
;; command. The index number is built by combining the cycle number
;; (normally representing days) and the sequence number, the "number
;; of added entry", so to speak
> (:last-index-appended (croque/appender-state queue))
;;=> 73164767887365

;; Add an additional entry (returns the insert index position)
> (croque/append-entry! queue [:example {:entry 3}])

;; Rewind tailer two entries
> (croque/rewind queue 2)
> (croque/next-entry queue)
;;=> [:example {:entry 1}]

;; Check current read position (sequence number)
> (:sequence (croque/tailer-state queue))
;;=> 2

;; Set the new read position by sequence number
> (croque/seek-sequence-position queue 3)
> (croque/next-entry queue)
;;=> [:example {:entry 3}]

;; Set the new read position by the index obtained before
> (croque/seek-index-position queue 73164767887365)
> (croque/next-entry queue)
;;=> [:example {:entry 2}]

```


## License

Copyright © 2016 Marcus Spiegel

Permission to use, copy, modify, and distribute this software for any purpose with or without fee is hereby granted,
provided that the above notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE INCLUDING ALL
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT,
INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER
IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
PERFORMANCE OF THIS SOFTWARE.
