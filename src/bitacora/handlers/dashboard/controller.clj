(ns bitacora.handlers.dashboard.controller
  (:require
   [bitacora.handlers.dashboard.model :as model]
   [bitacora.handlers.dashboard.view :as view]
   [bitacora.layout :refer [application]]
   [bitacora.models.util :refer [get-session-id]]))

(defn main [request]
  (let [title "DASHBOARD VEHICULAR"
        ok (get-session-id request)
        stats (model/get-stats)
        gastos-mes (model/gastos-por-mes)
        content (view/main title stats gastos-mes)]
    (application request title ok nil content)))