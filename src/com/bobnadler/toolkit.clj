(ns com.bobnadler.toolkit
  "Various utility functions for Clojure projects."
  (:require
   [clojure.string :as s]))

(defn wrap-request-id
  "Ring middleware that wraps a request with an identifier.

  Checks the request for a `x-request-id` header, if one is present it is
  added to the request map. If not present one is generated and added to the
  request map."
  [handler]
  (fn [request]
    (let [request-id (get-in request [:headers "x-request-id"] (java.util.UUID/randomUUID))]
      (handler (assoc request :request-id (.toString request-id))))))

(comment
  (def handler (wrap-request-id identity))

  (handler {}) ; {:request-id "2c8d4796-dc7a-47db-8182-f172b1f06521"}

  (handler {:headers {"x-request-id" "c9bd7379-3fe9-45e8-8f69-a8e20fa007a7"}})
; {:headers {"x-request-id" "c9bd7379-3fe9-45e8-8f69-a8e20fa007a7"},
;  :request-id "c9bd7379-3fe9-45e8-8f69-a8e20fa007a7"}
  )

(defn- default-log-fn [{:keys [level msg attrs]}]
  (.println System/out (format "%s | %s -- %s" (s/upper-case (name level)) msg attrs)))

(defn wrap-request-start-logger
  "Ring middleware that logs information about the start of the request."
  [handler & {:keys [log-fn] :or {log-fn default-log-fn}}]
  (fn [request]
    (let [method (s/upper-case (name (:request-method request)))
          path (str (:uri request)
                    (when-let [query-string (:query-string request)]
                      (str "?" query-string)))]
      (log-fn {:level :info
               :msg (format "Started %s '%s'" method path)
               :attrs {:method method :path path}})
      (handler request))))

(comment
  (def handler (wrap-request-start-logger identity))

  (handler {:request-method :get
            :uri "/foo"
            :query-string "bar=2"}))
