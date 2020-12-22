(ns terraform-graph.core-test
  (:require [clojure.test :refer :all]
            [terraform-graph.core :refer :all]))

(def expected_state {:nodes
                     {0 {:id 0
                         :name "aws_s3_bucket.bucket"
                         :short_name "bucket"
                         :module ""
                         :type "aws_s3_bucket"
                         :dependencies #{"random_pet.name"}}
                      1 {:id 1
                         :name "random_pet.name"
                         :short_name "name"
                         :module ""
                         :type "random_pet"
                         :dependencies #{}}}
                     :ids {"aws_s3_bucket.bucket" 0
                           "random_pet.name" 1}})

(deftest parse-test
  (testing "Parsing 'doc/small_state.json'"
    (is (= (parse "doc/small_state.json") expected_state))))

(deftest short-module-name-test
  (testing "short-module-name ..."
    (is (= "lorem.ipsum" (short-module-name "module.lorem.module.ipsum"))                                 "... supports pure modules")
    (is (= "lorem.ipsum.random_pet.name" (short-module-name "module.lorem.module.ipsum.random_pet.name")) "... supports long names")
    (is (= "" (short-module-name ""))                                                                     "... supports empty modules 1")
    (is (= "" (short-module-name nil))                                                                    "... supports empty modules 2")))
