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
                  "       v.nombre AS vehiculo_nombre, v.placa, v.modelo,"
                  "       d.nombre AS conductor_nombre"
                  " FROM cargas_gasolina c"
                  " JOIN vehiculos   v ON v.id = c.vehiculo_id"
                  " JOIN conductores d ON d.id = c.conductor_id"
                  " ORDER BY c.fecha DESC, c.id DESC")]))

(defn get-by-id [id]
  (first
   (Query db [(str "SELECT c.*,"
                   "       v.nombre AS vehiculo_nombre, v.placa, v.modelo,"
                   "       d.nombre AS conductor_nombre"
                   " FROM cargas_gasolina c"
                   " JOIN vehiculos   v ON v.id = c.vehiculo_id"
                   " JOIN conductores d ON d.id = c.conductor_id"
                   " WHERE c.id = ?")
              id])))

(defn get-by-vehiculo [vehiculo-id]
  (Query db [(str "SELECT c.*,"
                  "       v.nombre AS vehiculo_nombre, v.placa,"
                  "       d.nombre AS conductor_nombre"
                  " FROM cargas_gasolina c"
                  " JOIN vehiculos   v ON v.id = c.vehiculo_id"
                  " JOIN conductores d ON d.id = c.conductor_id"
                  " WHERE c.vehiculo_id = ?"
                  " ORDER BY c.fecha DESC")
             vehiculo-id]))

(defn create! [data]
  (let [raw    (Insert :cargas_gasolina data)
        result (cond
                 (map? raw)        raw
                 (sequential? raw) (first raw)
                 (number? raw)     {:id raw}
                 :else             raw)]
    (or (:generated_key result)
        ((keyword "last_insert_rowid()") result)
        (:id result))))

(defn update! [id data]
  (Update :cargas_gasolina data ["id = ?" id]))

(defn delete! [id]
  (Delete :cargas_gasolina ["id = ?" id]))
