(ns bitacora.handlers.cargas_gasolina.controller
  (:require [bitacora.handlers.cargas_gasolina.model :as model]
            [bitacora.handlers.cargas_gasolina.view  :as view]
            [bitacora.layout :refer [application]]
            [bitacora.models.util :refer [get-session-id]]
            [clojure.data.json :as json]
            [clojure.string :as str]))

;; ─────────────────────────────────────────
;; Helpers privados
;; ─────────────────────────────────────────

(defn- parse-int [x]
  (cond
    (integer? x) x
    (string? x)  (try (Integer/parseInt x) (catch Exception _ nil))
    :else nil))

(defn- parse-dbl [x]
  (cond
    (number? x) (double x)
    (string? x)  (try (Double/parseDouble x) (catch Exception _ nil))
    :else nil))

(defn- normalize-body [body]
  {:vehiculo_id      (parse-int    (:vehiculo_id body))
   :conductor_id     (parse-int    (:conductor_id body))
   :fecha            (:fecha       body)
   :litros           (parse-dbl    (:litros body))
   :precio_litro     (parse-dbl    (:precio_litro body))
   :total            (parse-dbl    (:total body))
   :odometro         (parse-int    (:odometro body))
   :imagen           (:imagen      body)
   :ticket_imagen    (:ticket_imagen body)
   :tipo_combustible (:tipo_combustible body)
   :observaciones    (:observaciones body)})

(defn- json-ok
  ([]      {:status 200 :headers {"Content-Type" "application/json"} :body (json/write-str {:ok true})})
  ([extra] {:status 200 :headers {"Content-Type" "application/json"} :body (json/write-str (merge {:ok true} extra))}))

(defn- json-err [status msg]
  {:status status :headers {"Content-Type" "application/json"} :body (json/write-str {:ok false :error msg})})

(defn- iso-fecha [f]
  (when f
    (subs (str f) 0 10)))

;; ─────────────────────────────────────────
;; Páginas HTML
;; ─────────────────────────────────────────

(defn index [request]
  (let [ok           (get-session-id request)
        fecha-inicio (get-in request [:params :fecha_inicio])
        fecha-fin    (get-in request [:params :fecha_fin])
        lista0       (model/get-all)
        lista        (if (and (not (str/blank? (or fecha-inicio "")))
                              (not (str/blank? (or fecha-fin ""))))
                       (filter (fn [c]
                                 (let [f (iso-fecha (:fecha c))]
                                   (and (<= (compare fecha-inicio f) 0)
                                        (>= (compare fecha-fin f) 0))))
                               lista0)
                       lista0)
        content      (view/index-view request lista fecha-inicio fecha-fin)]
    (application request "Cargas de Gasolina" ok nil content)))

(defn nuevo [request]
  (let [ok          (get-session-id request)
        vehiculos   (model/get-vehiculos)
        conductores (model/get-conductores)
        content     (view/edit-view request nil vehiculos conductores)]
    (application request "Nueva Carga de Gasolina" ok nil content)))

(defn editar [request]
  (let [id          (get-in request [:params :id])
        carga       (model/get-by-id id)
        vehiculos   (model/get-vehiculos)
        conductores (model/get-conductores)
        ok          (get-session-id request)
        content     (view/edit-view request carga vehiculos conductores)]
    (application request "Editar Carga de Gasolina" ok nil content)))

(defn ver [request]
  (let [id      (get-in request [:params :id])
        carga   (model/get-by-id id)
        ok      (get-session-id request)
        content (view/print-view request carga)]
    (application request (str "Carga #" id) ok nil content)))

;; ─────────────────────────────────────────
;; API JSON
;; ─────────────────────────────────────────

(defn guardar [request]
  (try
    (let [body  (json/read-str (slurp (:body request)) :key-fn keyword)
          id    (:id body)
          data  (normalize-body body)]
      (if id
        (do (model/update! id data)
            (json-ok {:id (parse-int id)}))
        (let [new-id (model/create! data)]
          {:status 201
           :headers {"Content-Type" "application/json"}
           :body (json/write-str {:ok true :id new-id})})))
    (catch Exception e
      (println "[ERROR] cargas-gasolina guardar:" (.getMessage e))
      (json-err 500 (.getMessage e)))))

(defn eliminar [request]
  (try
    (let [id (get-in request [:params :id])]
      (model/delete! id)
      (json-ok))
    (catch Exception e
      (json-err 500 (.getMessage e)))))