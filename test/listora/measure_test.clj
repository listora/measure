(ns listora.measure-test
  (:require [clojure.test :refer :all]
            [clojure.core.async :as a :refer [<!!]]
            [listora.measure :refer :all]))

(deftest test-measure
  (testing "measurements are recorded"
    (measure {:foo 1})
    (is (= (<!! measurements) {:foo 1})))

  (testing "measurements are buffered"
    (a/thread
     (measure {:bar 2})
     (measure {:baz 3}))
    (is (= (<!! measurements) {:bar 2}))
    (is (= (<!! measurements) {:baz 3}))))

(deftest test-imply
  (testing "single level"
    (imply {:foo 1}
      (measure {:bar 2})
      (is (= (<!! measurements) {:foo 1 :bar 2}))
      (measure {:baz 3})
      (is (= (<!! measurements) {:foo 1 :baz 3}))))

  (testing "nested imply"
    (imply {:foo 1}
      (imply {:bar 2}
        (measure {:baz 3})
        (is (= (<!! measurements) {:foo 1 :bar 2 :baz 3}))))))

(deftest test-profile
  (profile :foo (+ 1 1))
  (let [m (<!! measurements)]
    (is (contains? m :foo))
    (is (number? (:foo m)))
    (is (< (:foo m) 0.1))
    (is (> (:foo m) 0.0))))

(deftest test-add-profiling!
  (defn add-two [x] (+ x 2))
  (try
    (add-profiling! #'add-two)
    (is (= (add-two 3) 5))

    (let [m (<!! measurements)]
      (is (contains? m :listora.measure-test/add-two))
      (is (number? (:listora.measure-test/add-two m)))
      (is (< (:listora.measure-test/add-two m) 0.1))
      (is (> (:listora.measure-test/add-two m) 0.0)))
    
    (finally
      (ns-unmap *ns* 'add-two))))
