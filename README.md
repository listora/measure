# Measure

A small library for recording measurements and profiling information
from running server applications.

## Installation

Add the following dependency to your project.clj file:

```clojure
[listora/measure "0.1.4"]
```

## Usage

Measure writes measurement data to a [core.async][1] channel. In order
to use Measure, you first need something to consume this channel:

[1]: https://github.com/clojure/core.async

```clojure
(require '[listora.measure :as m])
(require '[clojure.core.async :refer [go-loop <!]])

(go-loop []
  (when-let [m (<! m/measurements)]
    (prn m)
    (recur)))
```

This is a very simple example, but could easily be more complex. For
instance, measurements could be grouped up in batches every few
seconds, then sent off to a metrics service.

Measurements are recorded as maps of data:

```clojure
(m/measure {:example "some value"})
```

A common form of measurement is to time how long a block of code takes
to execute. A `profile` macro is provided to handle this:

```clojure
(m/profile :profile/add (+ 1 1))
```

This will time how long the expression takes to execute, then write a
measurement with the key `:profile/add` and the value of a map with an
`:elapsed` key, measured in seconds, like so:

```clojure
{:profile/add {:elapsed 0.0001}
```

It's also possible to add profiling to pre-defined functions:

```clojure
(defn foo [x] (+ x 1))

(m/add-profiling! #'foo)
```

This will profile the function `foo` whenever it is invoked. The
resulting measurement will be stored under the fully qualified name of
the function; in this case, `:user/foo`. An optional second argument
to `add-profiling!` can be used to name the measurement key
differently.

Finally, Measure provides a way of adding data to any measurements
taken within a block.

```clojure
(m/imply {:group :example}
  (m/measure {:foo 1})
  (m/measure {:foo 2}))
```

The inner maps will be merged with the outer. This is equivalent to:

```clojure
(m/measure {:group :example, :foo 1})
(m/measure {:group :example, :foo 2})
```

Sometimes it's useful to combine measurements. The collate macro will
combine all measurements taken in the body, then send a single
measurement at the end.

```clojure
(m/collate merge
  (m/measure {:foo 1})
  (m/measure {:bar 2}))
```

This is equivalent to:

```clojure
(m/measure (merge {:foo 1} {:bar 2}))
```

In this example we just use `merge` to combine the measurements, but
more sophisticated functions can be applied.


## License

Copyright © 2014 Listora

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
