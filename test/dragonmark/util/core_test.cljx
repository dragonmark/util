(ns dragonmark.util.core_test
  #+cljs (:require-macros [dragonmark.util.core :as du])
  (:require #+clj  [clojure.test :refer :all]
            #+cljs [cemerick.cljs.test :as t :refer-macros [is deftest testing]]
                   [dragonmark.util.core :as du]))


(deftest test-mapcatv
  (let [empt (du/mapcatv identity nil)
        one [1 2 3]
        one-mapped (du/mapcatv (fn [x] [x]) one)]
    (is (= [] empt))
    (is (= one one-mapped))))
        
(deftest test-someor
  (is (= false (du/some-or false 33)))
  (is (= nil (du/some-or)))
  (is (= 33 (du/some-or 33)))
  )

(deftest test-restv
  (is (= [1] (du/restv [0 1])))
  (is (= '(1) (du/restv '(0 1))))
  (is (= [2 3 4] (du/restv [1 2 3 4])))
  )

(deftest test-concatv
  (is (= [1 2 3] (du/concatv [1] '(2 3))))
  (is (= [1 2 3] (du/concatv [1] [2] [3])))
  (is (= [1 2 3] (du/concatv [1] [2 3])))
  )

(deftest test-tree-map
  (is (= {:count 1
          :children [{:count 4} {:count 8}]}

         (du/map-tree
          #(update-in % [:count] + 1)
          {:count 0
          :children [{:count 3} {:count 7}]})))

  (is (= {:count 1
          :children {:dog {:count 4} :cat {:count 8}}}

         (du/map-tree
          #(update-in % [:count] + 1)
          {:count 0
          :children {:dog {:count 3} :cat {:count 7}}})))
  )
