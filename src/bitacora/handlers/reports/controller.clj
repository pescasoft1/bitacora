(ns bitacora.handlers.reports.controller
  (:require
   [bitacora.handlers.reports.model :as model]
   [bitacora.handlers.reports.view :as view]
   [bitacora.layout :refer [application]]
   [bitacora.models.util :refer [get-session-id]]))

(defn conductores
  [request]
  (let [title "Reporte de Conductores"
        ok (get-session-id request)
        js nil
        rows (model/get-conductores)
        content (view/conductores request title rows)]
    (application request title ok js content)))

(defn control-kilometraje
  [request]
  (let [title "Reporte de Control de Kilometraje"
        ok (get-session-id request)
        js nil
        rows (model/get-control-kilometraje)
        content (view/control-kilometraje request title rows)]
    (application request title ok js content)))

(defn main
  [request]
  (conductores request))

 