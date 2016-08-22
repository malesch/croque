# croque

An experimental implementation of a persistent historical queue [component] (https://github.com/stuartsierra/component), i.e. a queue where entries can be replayed from a 
previous point in time (if not intentionally expunged). This is simply a wrapper around the [Chronical Queue] (http://chronicle.software/products/chronicle-queue/) and only
exposes some basic functionality required for evaluating its usage as an [Event Storage] (http://martinfowler.com/eaaDev/EventSourcing.html) replacement for [Kafka]
(http://kafka.apache.org/) in a simplified local embedded scenario. 


## Usage

```clj
;; Get some dependencies
> (require '[com.stuartsierra.component :as component])
> (require '[croque.core :as croque])

;; Create and start a CroqueQueue component, persisting the queue entries
;; under the local directory './data'
> (def queue (component/start (croque/new-croque-queue {:path "./data"})))

;; Add some example entries to the queue
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

;; Add an additional entry
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