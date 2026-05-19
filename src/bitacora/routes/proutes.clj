(ns bitacora.routes.proutes
  (:require
   [compojure.core :refer [defroutes GET POST]]
   [bitacora.handlers.cargas_gasolina.controller :as cargas-gasolina]
   [bitacora.handlers.servicios_vehiculo.controller :as servicios-vehiculo]
   [bitacora.handlers.dashboard.controller :as dashboard]
   [bitacora.handlers.reports.controller :as reports]))

(defroutes proutes
  (GET "/dashboard" request (dashboard/main request))

  (GET  "/cargas-gasolina"              request (cargas-gasolina/index request))
  (GET  "/cargas-gasolina/nuevo"        request (cargas-gasolina/nuevo request))
  (GET  "/cargas-gasolina/editar/:id"   request (cargas-gasolina/editar request))
  (GET  "/cargas-gasolina/ver/:id"      request (cargas-gasolina/ver request))
  (GET  "/cargas-gasolina/ultimo-odometro" request (cargas-gasolina/ultimo-odometro-vehiculo request))
  (POST "/cargas-gasolina/guardar"      request (cargas-gasolina/guardar request))
  (POST "/cargas-gasolina/eliminar/:id" request (cargas-gasolina/eliminar request))
  (POST "/cargas-gasolina/subir-imagen" req (cargas-gasolina/subir-imagen req))

  (GET  "/servicios-vehiculo"            request (servicios-vehiculo/index request))
  (GET  "/servicios-vehiculo/nuevo"      request (servicios-vehiculo/nuevo request))
  (GET  "/servicios-vehiculo/editar/:id" request (servicios-vehiculo/editar request))
  (GET  "/servicios-vehiculo/ver/:id"    request (servicios-vehiculo/ver request))
  (POST "/servicios-vehiculo/guardar"    request (servicios-vehiculo/guardar request))
  (POST "/servicios-vehiculo/eliminar/:id" request (servicios-vehiculo/eliminar request))

  (GET "/reports/conductores" request (reports/conductores request))
  (GET "/reports/control-kilometraje" request (reports/control-kilometraje request)))