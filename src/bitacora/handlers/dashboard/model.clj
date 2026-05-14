(ns bitacora.handlers.dashboard.model
  (:require
   [bitacora.models.crud :refer [Query]]))

(defn- count-table [table]
  (->> (Query (str "select count(*) as count from " table))
       first
       :count))

(defn- sum-field [table field]
  (->> (Query (str "select coalesce(sum(" field "),0) as total from " table))
       first
       :total))

(defn get-stats []
  {:total-cargas      (count-table "cargas_gasolina")
   :total-vehiculos   (count-table "vehiculos")
   :total-conductores (count-table "conductores")
   :total-gastos      (count-table "servicios_vehiculo")
   :total-gastado     (sum-field "cargas_gasolina" "total")
   :total-litros      (sum-field "cargas_gasolina" "litros")})

;; 🔥 GASTOS POR MES (CORREGIDO)
(defn gastos-por-mes []
  (Query "
    SELECT 
      substr(fecha,6,2) as mes,
      SUM(total) as total,
      tipo
    FROM (
      SELECT fecha, total,tipo_combustible as tipo FROM cargas_gasolina
      UNION ALL
      SELECT fecha, monto as total, tipo_servicio.nombre  as tipo
       FROM servicios_vehiculo, tipo_servicio 
       where servicios_vehiculo.tipo_servicio_id = tipo_servicio.id 
    )
    WHERE fecha IS NOT NULL
    GROUP BY mes , tipo
    ORDER BY mes, tipo
  "))