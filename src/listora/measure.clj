(ns listora.measure
  (:require [clojure.core.async :as a :refer [>!!]]))

(def measurements
  "A channel that receives measurements."
  (a/chan (a/buffer 255)))

(defn measure
  "Add a measurement to the measurements channel."
  [key value]
  {:pre [(keyword? key)]}
  (>!! measurements [key value]))

(defn profile* [key func]
  (let [t0  (System/nanoTime)
        ret (func)
        t1  (System/nanoTime)]
    (measure key (/ (- t1 t0) 1e9))
    ret))

(defmacro profile
  "Evaluate the body and take a measurement of the time it takes to complete."
  [key & body]
  `(profile* ~key (^:once fn* [] ~@body)))
