(ns bitacora.handlers.cargas_gasolina.controller
  (:require [bitacora.handlers.cargas_gasolina.model :as model]
            [bitacora.handlers.cargas_gasolina.view  :as view]
            [bitacora.layout :refer [application]]
            [bitacora.models.util :refer [get-session-id]]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [clojure.java.io :as io]))

;; ─────────────────────────────────────────
;; Helpers privados
;; ─────────────────────────────────────────

(defn- parse-int [x]
  (cond
    (integer? x) x
    (number? x)  (int x)
    (string? x)  (try (Integer/parseInt x) (catch Exception _ nil))
    :else nil))

(defn- parse-dbl [x]
  (cond
    (number? x) x
    (string? x) (try (Double/parseDouble x) (catch Exception _ nil))
    :else nil))

(defn- normalize-body [body]
  {:vehiculo_id      (parse-int (:vehiculo_id body))
   :conductor_id     (parse-int (:conductor_id body))
   :fecha            (:fecha body)
   :litros           (parse-dbl (:litros body))
   :precio_litro     (parse-dbl (:precio_litro body))
   :total            (parse-dbl (:total body))
   :odometro         (parse-int (:odometro body))
   :imagen           (:imagen body)
   :ticket_imagen    (:ticket_imagen body)
   :tipo_combustible (:tipo_combustible body)
   :observaciones    (:observaciones body)})

(defn- json-ok
  ([]      {:status 200 :headers {"Content-Type" "application/json"} :body (json/write-str {:ok true})})
  ([extra] {:status 200 :headers {"Content-Type" "application/json"} :body (json/write-str (merge {:ok true} extra))}))

(defn- json-err [status msg]
  {:status status
   :headers {"Content-Type" "application/json"}
   :body (json/write-str {:ok false :error msg})})

(defn- iso-fecha [f]
  (when f
    (subs (str f) 0 10)))

(defn- validar-odometro [vehiculo-id excluir-id odometro]
  (let [ultimo (when vehiculo-id
                 (model/get-ultimo-odometro vehiculo-id excluir-id))
        odo-ant (:odometro ultimo)]
    (cond
      (nil? vehiculo-id)
      {:ok false :status 400 :error "Debes seleccionar un vehículo."}

      (nil? odometro)
      {:ok false :status 400 :error "Debes ingresar un odómetro."}

      (and odo-ant (<= odometro odo-ant))
      {:ok false
       :status 400
       :error (str "El odómetro debe ser mayor a " odo-ant " km.")
       :odometro_anterior odo-ant}

      :else
      {:ok true})))

(defn subir-imagen [request]
  (try
    (let [file (or (get-in request [:params :foto])
                   (get-in request [:multipart-params :foto]))]

      (when-not file
        (throw (ex-info "No se recibió archivo." {})))

      (let [temp     (:tempfile file)
            original (:filename file)
            ext      (or (some-> original (re-find #"\.[A-Za-z0-9]+$")) ".jpg")
            nombre   (str (java.util.UUID/randomUUID) ext)
            dir      (io/file "resources/public/uploads/cargas-gasolina")
            _        (.mkdirs dir)
            destino  (io/file dir nombre)]

        (io/copy temp destino)

        (json-ok {:url (str "/uploads/cargas-gasolina/" nombre)})))

    (catch Exception e
      (json-err 500 (.getMessage e)))))

;; ─────────────────────────────────────────
;; Páginas HTML
;; ─────────────────────────────────────────

(defn index [request]
  (let [ok           (get-session-id request)
        fecha-inicio (get-in request [:params :fecha_inicio])
        fecha-fin    (get-in request [:params :fecha_fin])
        vehiculo-id  (get-in request [:params :vehiculo_id])
        vehiculos    (model/get-vehiculos)

        lista0       (if (str/blank? (or vehiculo-id ""))
                       (model/get-all)
                       (model/get-by-vehiculo vehiculo-id))

        lista        (if (and (not (str/blank? (or fecha-inicio "")))
                              (not (str/blank? (or fecha-fin ""))))
                       (filter (fn [c]
                                 (let [f (iso-fecha (:fecha c))]
                                   (and (<= (compare fecha-inicio f) 0)
                                        (>= (compare fecha-fin f) 0))))
                               lista0)
                       lista0)

        content      (view/index-view request lista fecha-inicio fecha-fin vehiculos vehiculo-id)]
    (application request "Cargas de Gasolina" ok nil content)))

(defn nuevo [request]
  (let [ok          (get-session-id request)
        vehiculos   (model/get-vehiculos)
        conductores (model/get-conductores)
        content     (view/edit-view request nil vehiculos conductores nil)]
    (application request "Nueva Carga de Gasolina" ok nil content)))

(defn editar [request]
  (let [id          (get-in request [:params :id])
        carga       (model/get-by-id id)
        vehiculos   (model/get-vehiculos)
        conductores (model/get-conductores)
        ok          (get-session-id request)
        ultimo-odo  (when (:vehiculo_id carga)
                      (model/get-ultimo-odometro (:vehiculo_id carga) id))
        content     (view/edit-view request carga vehiculos conductores ultimo-odo)]
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
    (let [body   (json/read-str (slurp (:body request)) :key-fn keyword)
          id     (parse-int (:id body))
          data   (normalize-body body)
          odo    (:odometro data)
          check  (validar-odometro (:vehiculo_id data) id odo)]
      (if-not (:ok check)
        (json-err (:status check) (:error check))
        (if id
          (do
            (model/update! id data)
            (json-ok {:id id}))
          (let [new-id (model/create! data)]
            {:status 201
             :headers {"Content-Type" "application/json"}
             :body (json/write-str {:ok true :id new-id})}))))
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

(defn ultimo-odometro-vehiculo [request]
  (try
    (let [vehiculo-id (get-in request [:params :vehiculo_id])
          excluir-id  (get-in request [:params :excluir_id])
          odo         (model/get-ultimo-odometro vehiculo-id excluir-id)]
      (if odo
        {:status 200
         :headers {"Content-Type" "application/json"}
         :body (json/write-str {:ok true
                                :odometro (:odometro odo)
                                :fecha    (str (:fecha odo))
                                :id       (:id odo)})}
        {:status 200
         :headers {"Content-Type" "application/json"}
         :body (json/write-str {:ok true :odometro nil})}))
    (catch Exception e
      (json-err 500 (.getMessage e)))))