# croque

An experimental implementation of a persistent historical queue [component] (https://github.com/stuartsierra/component), i.e. a queue where entries can be replayed from a 
previous point in time (if not intentionally expunged). This is simply a wrapper around [Chronical Queue] (http://chronicle.software/products/chronicle-queue/) and only
exposes some basic functionality required for evaluating its usage as an [Event Storage] (http://martinfowler.com/eaaDev/EventSourcing.html) replacement for [Kafka]
(http://kafka.apache.org/) in a simplified local embedded scenario. 


## Usage


## License

Copyright © 2016 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
