(ns watermark.core-test
  (:require [clojure.test :refer :all]
            [watermark.core :refer :all]))

(deftest is-image
  (testing "Gifs" (is-image "gif")))
#{}
