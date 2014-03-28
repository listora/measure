(ns listora.measure
  (:require [clojure.core.async :as a :refer [>!!]]))

(def measurements
  "A channel that receives measurements."
  (a/chan (a/buffer 255)))

(def ^:dynamic *measurement* {})

(defn measure
  "Add a measurement map to the measurements channel."
  [measurement]
  (>!! measurements (merge *measurement* measurement)))

(defmacro imply
  "Merges a map with any measurement taken in the body."
  [measurement & body]
  `(binding [*measurement* (merge *measurement* ~measurement)]
     ~@body))

(defn profile* [key func]
  (let [t0  (System/nanoTime)
        ret (func)
        t1  (System/nanoTime)]
    (measure {key (/ (- t1 t0) 1e9)})
    ret))

(defmacro profile
  "Evaluate the body and take a measurement of the time it takes to complete
  in seconds."
  [key & body]
  `(profile* ~key (^:once fn* [] ~@body)))

(defn add-profiling!
  "Add profiling to a function defined in a var."
  [var]
  (let [ns  (-> var .ns .name str)
        sym (-> var .sym str)
        key (keyword ns sym)]
    (alter-var-root var (fn [f] (fn [& args] (profile* key #(apply f args)))))))
