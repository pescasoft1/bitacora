(ns bitacora.handlers.reports.model
  (:require
   [bitacora.models.crud :refer [Query]]))

(def get-control-kilometraje-sql
  "
  select
    ck.*,
    v.nombre as vehiculo_nombre,
    strftime('%d/%m/%Y', ck.fecha) as fecha_formateada,
    strftime('%d/%m/%Y %H:%M', ck.fecha_registro) as fecha_registro_formateada
  from control_kilometraje ck
  left join vehiculos v
         on ck.vehiculo_id = v.id
  order by
    date(ck.fecha) asc,
    vehiculo_nombre asc
  ")


(def get-conductores-sql
  "
   SELECT 
    nombre,
    telefono,
    licencia,
    CASE 
        WHEN activo = 1 THEN 'Sí'
        ELSE 'No'
    END AS activo,
    fecha_registro
FROM conductores
ORDER BY id DESC")


(defn get-control-kilometraje
  []
  (Query get-control-kilometraje-sql))


(defn get-conductores
  []
  (Query get-conductores-sql))
