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

(deftest test-collate
  (testing "single level"
    (collate merge
      (measure {:foo 1})
      (measure {:bar 2}))
    (is (= (<!! measurements) {:foo 1 :bar 2})))

  (testing "nested collate"
    (collate merge
      (collate merge
        (measure {:foo 1})
        (measure {:bar 2}))
      (measure {:baz 3}))
    (is (= (<!! measurements) {:foo 1 :bar 2 :baz 3}))))

(deftest test-profile
  (profile :foo (+ 1 1))
  (let [m (<!! measurements)]
    (is (contains? m :foo))
    (is (map? (:foo m)))
    (is (contains? (:foo m) :elapsed))
    (is (number? (-> m :foo :elapsed)))
    (is (< (-> m :foo :elapsed) 0.1))
    (is (> (-> m :foo :elapsed) 0.0))))

(deftest test-add-profiling-of-time-elapsed
  (defn add-two [x] (+ x 2))
  (try
    (add-profiling! #'add-two)
    (is (= (add-two 3) 5))

    (let [m (<!! measurements)]
      (is (contains? m :listora.measure-test/add-two))
      (is (map? (:listora.measure-test/add-two m)))
      (is (contains? (:listora.measure-test/add-two m) :elapsed))
      (is (number? (-> m :listora.measure-test/add-two :elapsed)))
      (is (< (-> m :listora.measure-test/add-two :elapsed) 0.1))
      (is (> (-> m :listora.measure-test/add-two :elapsed) 0.0)))

    (finally
      (ns-unmap *ns* 'add-two))))

(deftest test-add-profiling-measurement-keys
  (testing "with no explicit key"
    (try
      (defn add-two [x] (+ x 2))
      (add-profiling! #'add-two)
      (is (= (add-two 3) 5))
      (is (contains? (<!! measurements) :listora.measure-test/add-two))
      (finally
        (ns-unmap *ns* 'add-three))))

  (testing "with a key of :my-metric"
    (try
      (defn add-two [x] (+ x 2))
      (add-profiling! #'add-two :custom)
      (is (= (add-two 3) 5))
      (is (contains? (<!! measurements) :custom))
      (finally
        (ns-unmap *ns* 'add-three)))))
