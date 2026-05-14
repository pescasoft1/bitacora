(ns bitacora.handlers.servicios_vehiculo.model
  (:require [bitacora.models.crud :refer [db Query Insert Delete Update]]))

(defn get-vehiculos []
  (Query db ["SELECT id, nombre, placa, modelo
              FROM vehiculos
              WHERE activo = 1
              ORDER BY nombre"]))

(defn get-conductores []
  (Query db ["SELECT id, nombre, telefono
              FROM conductores
              WHERE activo = 1
              ORDER BY nombre"]))

(defn get-tipos-servicio []
  (Query db ["SELECT id, nombre
              FROM tipo_servicio
              ORDER BY nombre"]))

(defn get-all []
  (Query db [(str "SELECT s.*, "
                  "       v.nombre AS vehiculo_nombre, v.placa, v.modelo, "
                  "       d.nombre AS conductor_nombre, "
                  "       t.nombre AS tipo_servicio_nombre "
                  "  FROM servicios_vehiculo s "
                  "  LEFT JOIN vehiculos v ON v.id = s.vehiculo_id "
                  "  LEFT JOIN conductores d ON d.id = s.conductor_id "
                  "  LEFT JOIN tipo_servicio t ON t.id = s.tipo_servicio_id "
                  " ORDER BY s.fecha DESC, s.id DESC")]))

(defn get-by-id [id]
  (first
   (Query db [(str "SELECT s.*, "
                   "       v.nombre AS vehiculo_nombre, v.placa, v.modelo, "
                   "       d.nombre AS conductor_nombre, "
                   "       t.nombre AS tipo_servicio_nombre "
                   "  FROM servicios_vehiculo s "
                   "  LEFT JOIN vehiculos v ON v.id = s.vehiculo_id "
                   "  LEFT JOIN conductores d ON d.id = s.conductor_id "
                   "  LEFT JOIN tipo_servicio t ON t.id = s.tipo_servicio_id "
                   " WHERE s.id = ?")
              id])))

(defn create! [data]
  (let [raw    (Insert :servicios_vehiculo data)
        result (cond
                 (map? raw)        raw
                 (sequential? raw) (first raw)
                 (number? raw)     {:id raw}
                 :else             raw)]
    (or (:generated_key result)
        ((keyword "last_insert_rowid()") result)
        (:id result))))

(defn update! [id data]
  (Update :servicios_vehiculo data ["id = ?" id]))

(defn delete! [id]
  (Delete :servicios_vehiculo ["id = ?" id]))