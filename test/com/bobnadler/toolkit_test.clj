(ns com.bobnadler.toolkit-test
  (:require [clojure.test :as t]
            [com.bobnadler.toolkit :as bntk]))

(t/deftest wrap-request-id-test
  (let [handler (bntk/wrap-request-id identity)]

    (t/testing "no header"
      (t/is (contains? (handler {}) :request-id)))

    (t/testing "header is present"
      (let [req-id "c9bd7379-3fe9-45e8-8f69-a8e20fa007a7"]
        (t/is (= req-id
                 (:request-id (handler {:headers {"x-request-id" req-id}}))))))))

(t/deftest wrap-request-start-logger-test
  (let [state_ (atom nil)
        fake-logger (fn [log-info] (reset! state_ log-info))
        handler (bntk/wrap-request-start-logger identity {:log-fn fake-logger})]
    (handler {:request-method :get :uri "/foo" :query-string "bar=2"})
    (t/is (= {:level :info
             :msg "Started GET '/foo?bar=2'"
             :attrs {:method "GET" :path "/foo?bar=2"}}
             @state_))))
