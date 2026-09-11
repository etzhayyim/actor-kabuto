(ns kabuto.repository-contract-test
  (:require [clojure.edn :as edn] [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]))
(deftest repository-boundary
  (let [c (edn/read-string (slurp "repository-contracts.edn"))
        names ["publishIntelReport" "publishSupplyChainViz" "registerCompany"
               "registerSupplyEdge" "socialPost"]]
    (is (= :edn (get-in c [:canonical :format])))
    (doseq [n names]
      (is (.isFile (io/file "lex" (str n ".edn"))))
      (is (.isFile (io/file "wire/lexicons" (str n ".json")))))
    (doseq [p (:forbidden-root-paths c)] (is (not (.exists (io/file p))) p))))
