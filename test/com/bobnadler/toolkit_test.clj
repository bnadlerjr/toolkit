(ns com.bobnadler.toolkit-test
  (:require [clojure.test :as t]
            [com.bobnadler.toolkit :as btk]))

(t/deftest wrap-request-id-test
  (let [handler (btk/wrap-request-id identity)]

    (t/testing "no header"
      (t/is (contains? (handler {}) :request-id)))

    (t/testing "header is present"
      (let [req-id "c9bd7379-3fe9-45e8-8f69-a8e20fa007a7"]
        (t/is (= req-id
                 (:request-id (handler {:headers {"x-request-id" req-id}}))))))))
