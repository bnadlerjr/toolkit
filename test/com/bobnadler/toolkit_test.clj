(ns com.bobnadler.toolkit-test
  (:require [clojure.test :as t]
            [com.bobnadler.toolkit :as bntk]))

(t/deftest redact-map-test
  (let [options {:redact-key? #{:secret :password} :redact-value "[FILTERED]"}]
    (t/is (= {:secret "[FILTERED]"}
             (bntk/redact-map {:secret "123"} options)))

    (t/is (= {:secret "[FILTERED]" :foo 42}
             (bntk/redact-map {:secret "123" :foo 42} options)))

    (t/is (= {"secret" "[FILTERED]" :foo 42}
             (bntk/redact-map {"secret" "123" :foo 42} options)))

    (t/is (= {:nested {"secret" "[FILTERED]"} :foo 42}
             (bntk/redact-map {:nested {"secret" "123"} :foo 42} options)))

    (t/is (= {:nested {"secret" "[FILTERED]"} :password "[FILTERED]"}
             (bntk/redact-map {:nested {"secret" "123"} :password "secret"} options)))))

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
        req {:request-method :get :uri "/foo" :query-string "bar=2"}
        handler (bntk/wrap-request-start-logger identity {:log-fn fake-logger})]

    (t/testing "message is logged"
      (handler req)
      (t/is (= {:level :info
                :msg "Started GET '/foo?bar=2'"
                :attrs {:method "GET" :path "/foo?bar=2"}}
               @state_)))

    (t/testing "request map is returned"
      (let [response (handler req)]
        (t/is (= :get (:request-method response)))
        (t/is (= "/foo" (:uri response)))
        (t/is (= "bar=2" (:query-string response)))
        (t/is (> (:start-ms response) 0))))))

(t/deftest wrap-request-finish-logger-test
  (let [state_ (atom nil)
        fake-logger (fn [log-info] (reset! state_ log-info))
        req {:request-method :get :uri "/foo" :query-string "bar=2"}
        handler (bntk/wrap-request-finish-logger
                 #(assoc % :status 200)
                 {:log-fn fake-logger})]

    (t/testing "message is logged with duration"
      ;; Might be brittle, but works for now
      (handler (assoc req :start-ms (System/currentTimeMillis)))
      (let [{:keys [level msg attrs]} @state_]
        (t/is (= :info level))
        (t/is (= "Completed GET '/foo?bar=2' 200 in 0ms" msg))
        (t/is (= "GET" (:method attrs)))
        (t/is (= "/foo?bar=2" (:path attrs)))
        (t/is (= 200 (:status attrs)))
        (t/is (= "0ms" (:duration attrs)))))

    (t/testing "message is logged with unknown duration"
      (handler req)
      (let [{:keys [level msg attrs]} @state_]
        (t/is (= :info level))
        (t/is (= "Completed GET '/foo?bar=2' 200 in ?ms" msg))
        (t/is (= "GET" (:method attrs)))
        (t/is (= "/foo?bar=2" (:path attrs)))
        (t/is (= 200 (:status attrs)))
        (t/is (= "?ms" (:duration attrs)))))

    (t/testing "request map is returned"
      (t/is (= (assoc req :status 200) (handler req))))))

(t/deftest wrap-request-params-logger
  (let [state_ (atom nil)
        fake-logger (fn [log-info] (reset! state_ log-info))
        handler (bntk/wrap-request-params-logger identity {:log-fn fake-logger})
        request {:params {:foo 42 :password "secret"}}]
    (t/testing "message is logged with filtered params"
      (handler request)
      (let [{:keys [level msg attrs]} @state_]
        (t/is (= :info level))
        (t/is (= "Request parameters" msg))
        (t/is (= {:foo 42 :password "[FILTERED]"} (:params attrs)))))

    (t/testing "anti-forgery token is removed from params"
      (handler {:params {:foo 42 :__anti-forgery-token "abc123"}})
      (let [{:keys [attrs]} @state_]
        (t/is (= {:foo 42} (:params attrs)))))

    (t/testing "no message emitted if params are empty"
      (reset! state_ nil)
      (handler {})
      (t/is (nil? @state_)))

    (t/testing "request map is returned"
      (t/is (= request (handler request))))))
