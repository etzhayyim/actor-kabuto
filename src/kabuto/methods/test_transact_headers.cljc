(ns kabuto.methods.test-transact-headers
  "kabuto 兜 — transact request headers (ADR-2608124000, \"clients first\").

  kotoba-server's require_internal_trust gate returns success while
  KOTOBA_INTERNAL_SECRET is unset, and it is unset across the fleet — so sending
  the x-internal-trust header today changes nothing on the wire. These tests pin
  the shape NOW so that arming the server later is a one-variable decision
  rather than a fleet-wide outage.

  This namespace is NEW: kabuto.methods.transact had no header coverage before.
  `request-headers` is pure, so none of this needs the network.

  NOTE: kabuto has NO host allowlist, so there is no allowlist positive control
  to assert here. The KOTOBA_TOKEN bearer is the only pre-existing header and it
  IS asserted. Secret values below are obviously synthetic."
  (:require [clojure.test :refer [deftest is run-tests]]
            [kabuto.methods.transact :as transact]))

(def ^:private synthetic-trust "synthetic-internal-trust-not-a-real-secret")
(def ^:private synthetic-token "synthetic-operator-token")

(deftest internal-trust-header-present-when-configured
  (let [h (transact/request-headers synthetic-token synthetic-trust)]
    (is (= synthetic-trust (get h "x-internal-trust"))
        "the configured value is sent verbatim")
    ;; positive control — pre-existing headers untouched
    (is (= (str "Bearer " synthetic-token) (get h "Authorization")))
    (is (= "application/json" (get h "Content-Type")))))

(deftest internal-trust-header-absent-when-unconfigured
  (doseq [trust [nil "" "   "]]
    (let [h (transact/request-headers synthetic-token trust)]
      (is (not (contains? h "x-internal-trust"))
          (str "omitted entirely for " (pr-str trust) " — never an empty string"))
      (is (= (str "Bearer " synthetic-token) (get h "Authorization")))))
  ;; the no-op property: unconfigured yields exactly the historical header map
  (is (= {"Content-Type" "application/json"
          "Authorization" (str "Bearer " synthetic-token)}
         (transact/request-headers synthetic-token nil))))

(deftest bearer-keeps-its-own-rules
  ;; positive control in the other direction — a trust secret is NOT a bearer
  (is (= {"Content-Type" "application/json"} (transact/request-headers nil nil)))
  (let [h (transact/request-headers nil synthetic-trust)]
    (is (not (contains? h "Authorization")))
    (is (= synthetic-trust (get h "x-internal-trust")))))

(deftest internal-trust-absence-is-reported-not-silent
  (is (= "x-internal-trust" transact/internal-trust-header))
  (is (= "KOTOBA_INTERNAL_SECRET" transact/internal-trust-env)
      "the same variable the server and the Cloudflare gateway read")
  (with-redefs [transact/internal-trust (constantly nil)]
    (is (= :unconfigured (transact/internal-trust-status))))
  (with-redefs [transact/internal-trust (constantly synthetic-trust)]
    (is (= :configured (transact/internal-trust-status)))))

#?(:clj
   (when (= *file* (System/getProperty "babashka.file"))
     (let [{:keys [fail error]} (run-tests 'kabuto.methods.test-transact-headers)]
       (System/exit (if (zero? (+ fail error)) 0 1)))))
