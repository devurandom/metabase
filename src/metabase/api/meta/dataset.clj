(ns metabase.api.meta.dataset
  "/api/meta/dataset endpoints."
  (:require [clojure.data.csv :as csv]
            [compojure.core :refer [GET POST]]
            [metabase.api.common :refer :all]
            [metabase.driver :as driver]
            [metabase.util :as u]))

(defendpoint POST "/"
  "Execute an MQL query and retrieve the results as JSON."
  [:as {:keys [body]}]
  (let [{:keys [status] :as response} (driver/dataset-query body {:executed_by *current-user-id*})]
    {:status (if (= status :completed) 200 400)
     :body response}))

(defendpoint GET "/csv"
  "Execute an MQL query and download the result data as a CSV file."
  [query]
  {query [Required String->Dict]}
  (let [{{:keys [columns rows]} :data :keys [status] :as response} (driver/dataset-query query {:executed_by *current-user-id*})]
    (if (= status :completed)
      ;; successful query, send CSV file
      {:status 200
       :body (with-out-str
               (csv/write-csv *out* (into [columns] rows)))
       :headers {"Content-Type" "text/csv"
                 "Content-Disposition" (str "attachment; filename=\"query_result_" (u/now-iso8601) ".csv\"")}}
      ;; failed query, send error message
      {:status 500
       :body response})))

(define-routes)
