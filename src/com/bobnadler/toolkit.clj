(ns com.bobnadler.toolkit
  "Various utility functions for Clojure projects.")

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
