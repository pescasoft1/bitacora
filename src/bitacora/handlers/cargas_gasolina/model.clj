(ns bitacora.handlers.cargas_gasolina.model
  (:require [bitacora.models.crud :refer [db Query Insert Delete Update]]))

;; ─────────────────────────────────────────
;; Catálogos
;; ─────────────────────────────────────────

(defn get-vehiculos []
  (Query db ["SELECT id, nombre, placa, modelo FROM vehiculos WHERE activo = 1 ORDER BY nombre"]))

(defn get-conductores []
  (Query db ["SELECT id, nombre, telefono FROM conductores WHERE activo = 1 ORDER BY nombre"]))

;; ─────────────────────────────────────────
;; Cargas de Gasolina
;; ─────────────────────────────────────────

(defn get-all []
  (Query db [(str "SELECT c.*,"
                  "       v.nombre AS vehiculo_nombre, v.placa, v.modelo,"                  "       v.rendimientoxkm AS vehiculo_rendimientoxkm,"                  "       d.nombre AS conductor_nombre"
                  " FROM cargas_gasolina c"
                  " JOIN vehiculos   v ON v.id = c.vehiculo_id"
                  " JOIN conductores d ON d.id = c.conductor_id"
                  " ORDER BY c.fecha DESC, v.placa ASC, c.id DESC")]))

(defn get-by-id [id]
  (first
   (Query db [(str "SELECT c.*,"
                   "       v.nombre AS vehiculo_nombre, v.placa, v.modelo,"
                   "       v.rendimientoxkm AS vehiculo_rendimientoxkm,"
                   "       d.nombre AS conductor_nombre"
                   " FROM cargas_gasolina c"
                   " JOIN vehiculos   v ON v.id = c.vehiculo_id"
                   " JOIN conductores d ON d.id = c.conductor_id"
                   " WHERE c.id = ?")
              id])))

(defn get-by-vehiculo [vehiculo-id]
  (Query db [(str "SELECT c.*,"
                  "       v.nombre AS vehiculo_nombre, v.placa,"
                  "       v.rendimientoxkm AS vehiculo_rendimientoxkm,"
                  "       d.nombre AS conductor_nombre"
                  " FROM cargas_gasolina c"
                  " JOIN vehiculos   v ON v.id = c.vehiculo_id"
                  " JOIN conductores d ON d.id = c.conductor_id"
                  " WHERE c.vehiculo_id = ?"
                  " ORDER BY c.fecha DESC, c.id DESC")
             vehiculo-id]))

;; Devuelve el odómetro de la última carga del vehículo, excluyendo la carga actual (por id)
(defn get-ultimo-odometro [vehiculo-id & [excluir-id]]
  (let [sql (if excluir-id
              ["SELECT odometro, fecha, id FROM cargas_gasolina
                WHERE vehiculo_id = ? AND id != ? AND odometro IS NOT NULL
                ORDER BY fecha DESC, id DESC LIMIT 1"
               vehiculo-id excluir-id]
              ["SELECT odometro, fecha, id FROM cargas_gasolina
                WHERE vehiculo_id = ? AND odometro IS NOT NULL
                ORDER BY fecha DESC, id DESC LIMIT 1"
               vehiculo-id])]
    (first (Query db sql))))

(defn actualizar-odometro-vehiculo!
  [vehiculo-id odometro]

  (when (and vehiculo-id odometro)

    (Update :vehiculos
            {:odometro odometro}
            ["id = ?" vehiculo-id])))



(defn create! [data]
  (let [raw    (Insert :cargas_gasolina data)
        result (cond
                 (map? raw)        raw
                 (sequential? raw) (first raw)
                 (number? raw)     {:id raw}
                 :else             raw)
        new-id  (or (:generated_key result)
                    ((keyword "last_insert_rowid()") result)
                    (:id result))]
    (actualizar-odometro-vehiculo! (:vehiculo_id data) (:odometro data))
    new-id))



(defn update! [id data]
  (Update :cargas_gasolina data ["id = ?" id])
  (actualizar-odometro-vehiculo! (:vehiculo_id data) (:odometro data)))

(defn delete! [id]
  (Delete :cargas_gasolina ["id = ?" id]))
