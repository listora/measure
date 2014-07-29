(ns listora.measure
  (:require [clojure.core.async :as a :refer [>!!]]))

(def measurements
  "A channel that receives measurements."
  (a/chan (a/buffer 255)))

(def ^:dynamic *measurement* {})

(def ^:dynamic *buffer* nil)

(defn measure
  "Add a measurement map to the measurements channel."
  [measurement]
  (let [m (merge *measurement* measurement)]
    (if *buffer*
      (swap! *buffer* conj m)
      (>!! measurements m))))

(defn collate* [combinef func]
  (let [buffer (atom [])]
    (try (binding [*buffer* buffer] (func))
         (finally (measure (reduce combinef @buffer))))))

(defmacro collate
  "Any measurement taken in the body will be buffered then combined with the
  supplied function."
  [combinef & body]
  `(collate* ~combinef (^:once fn* [] ~@body)))

(defmacro imply
  "Merges a map with any measurement taken in the body."
  [measurement & body]
  `(binding [*measurement* (merge *measurement* ~measurement)]
     ~@body))

(defn profile* [key func]
  (let [t0  (System/nanoTime)
        ret (func)
        t1  (System/nanoTime)]
    (measure {key {:elapsed (/ (- t1 t0) 1e9)}})
    ret))

(defmacro profile
  "Evaluate the body and take a measurement of the time it takes to complete
  in seconds."
  [key & body]
  `(profile* ~key (^:once fn* [] ~@body)))

(defn- default-key [var]
  (keyword (-> var .ns .name str)
           (-> var .sym str)))

(defn add-profiling!
  "Add profiling to a function defined in a var."
  ([var]
     (add-profiling! var (default-key var)))
  ([var key]
     (alter-var-root
      var (fn [f] (fn [& args] (profile* key #(apply f args)))))))
