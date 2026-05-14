(ns bitacora.handlers.servicios_vehiculo.controller
  (:require [bitacora.handlers.servicios_vehiculo.model :as model]
            [bitacora.handlers.servicios_vehiculo.view  :as view]
            [bitacora.layout :refer [application]]
            [bitacora.models.util :refer [get-session-id]]
            [clojure.data.json :as json]
            [clojure.string :as str]))

(defn- parse-int [x]
  (cond
    (integer? x) x
    (string? x)  (try (Integer/parseInt x) (catch Exception _ nil))
    :else nil))

(defn- parse-dbl [x]
  (cond
    (number? x) (double x)
    (string? x) (try (Double/parseDouble x) (catch Exception _ nil))
    :else nil))

(defn- normalize-body [body]
  {:vehiculo_id      (parse-int (:vehiculo_id body))
   :conductor_id     (parse-int (:conductor_id body))
   :tipo_servicio_id (parse-int (:tipo_servicio_id body))
   :reparacion       (:reparacion body)
   :monto            (parse-dbl (:monto body))
   :fecha            (:fecha body)
   :imagen            (:imagen body)})

(defn- json-ok
  ([]      {:status 200 :headers {"Content-Type" "application/json"} :body (json/write-str {:ok true})})
  ([extra] {:status 200 :headers {"Content-Type" "application/json"} :body (json/write-str (merge {:ok true} extra))}))

(defn- json-err [status msg]
  {:status status :headers {"Content-Type" "application/json"} :body (json/write-str {:ok false :error msg})})

(defn- iso-fecha [f]
  (when f
    (subs (str f) 0 10)))

(defn index [request]
  (let [ok           (get-session-id request)
        fecha-inicio (get-in request [:params :fecha_inicio])
        fecha-fin    (get-in request [:params :fecha_fin])
        lista0       (model/get-all)
        lista        (if (and (not (str/blank? (or fecha-inicio "")))
                              (not (str/blank? (or fecha-fin ""))))
                       (filter (fn [s]
                                 (let [f (iso-fecha (:fecha s))]
                                   (and (<= (compare fecha-inicio f) 0)
                                        (>= (compare fecha-fin f) 0))))
                               lista0)
                       lista0)
        content      (view/index-view request lista fecha-inicio fecha-fin)]
    (application request "Servicios de Vehículos" ok nil content)))

(defn nuevo [request]
  (let [ok          (get-session-id request)
        vehiculos   (model/get-vehiculos)
        conductores (model/get-conductores)
        tipos       (model/get-tipos-servicio)
        content     (view/edit-view request nil vehiculos conductores tipos)]
    (application request "Nuevo Servicio de Vehículo" ok nil content)))

(defn editar [request]
  (let [id          (get-in request [:params :id])
        servicio    (model/get-by-id id)
        vehiculos   (model/get-vehiculos)
        conductores (model/get-conductores)
        tipos       (model/get-tipos-servicio)
        ok          (get-session-id request)
        content     (view/edit-view request servicio vehiculos conductores tipos)]
    (application request "Editar Servicio de Vehículo" ok nil content)))

(defn ver [request]
  (let [id      (get-in request [:params :id])
        servicio (model/get-by-id id)
        ok      (get-session-id request)
        content (view/print-view request servicio)]
    (application request (str "Servicio #" id) ok nil content)))

(defn guardar [request]
  (try
    (let [body (json/read-str (slurp (:body request)) :key-fn keyword)
          id   (:id body)
          data (normalize-body body)]
      (if id
        (do
          (model/update! id data)
          (json-ok {:id (parse-int id)}))
        (let [new-id (model/create! data)]
          {:status 201
           :headers {"Content-Type" "application/json"}
           :body (json/write-str {:ok true :id new-id})})))
    (catch Exception e
      (println "[ERROR] servicios_vehiculo guardar:" (.getMessage e))
      (json-err 500 (.getMessage e)))))

(defn eliminar [request]
  (try
    (let [id (get-in request [:params :id])]
      (model/delete! id)
      (json-ok))
    (catch Exception e
      (json-err 500 (.getMessage e)))))