(ns bitacora.handlers.reports.view
  (:require
   [bitacora.models.grid :refer [build-dashboard]]))

(defn conductores
  [request title rows]
  (let [table-id "conductores-report"
        fields (array-map
                :nombre "Nombre"
                :telefono "Teléfono"
                :licencia "Licencia"
                :activo "Activo"
                :fecha_registro "Fecha Registro")]
    (build-dashboard request title rows table-id fields)))

(defn control-kilometraje
  [request title rows]
  (let [table-id "control-kilometraje-report"
        fields (array-map
                :vehiculo_nombre "Vehículo"
                :fecha_formateada "Fecha"
                :kilometraje "Kilometraje"
                :observaciones "Observaciones"
                :fecha_registro_formateada "Fecha Registro")]
    (build-dashboard request title rows table-id fields)))

(defn main
  [request title rows]
  (conductores request title rows))