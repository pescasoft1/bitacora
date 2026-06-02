(ns bitacora.handlers.servicios_vehiculo.controller
  (:require [bitacora.handlers.servicios_vehiculo.model :as model]
            [bitacora.handlers.servicios_vehiculo.view  :as view]
            [bitacora.layout :refer [application]]
            [bitacora.models.crud :refer [config]]
            [bitacora.models.util :refer [get-session-id]]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [clojure.java.io :as io])
  (:import [java.awt Color]
           [java.awt.image BufferedImage]
           [java.util Base64]
           [javax.imageio ImageIO]))

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
   :imagen          (:imagen body)})

(defn- uploads-dir []
  (doto (io/file (:uploads config) "servicios")
    (.mkdirs)))

(defn- upload-url [filename]
  (str (:path config) filename))

(defn- data-url? [s]
  (and (string? s) (str/starts-with? s "data:image/")))

(defn- draw-jpg! [image target]
  (when-not image
    (throw (ex-info "No se pudo leer la imagen." {})))
  (let [rgb (BufferedImage. (.getWidth image) (.getHeight image) BufferedImage/TYPE_INT_RGB)
        g   (.createGraphics rgb)]
    (try
      (.setColor g Color/WHITE)
      (.fillRect g 0 0 (.getWidth rgb) (.getHeight rgb))
      (.drawImage g image 0 0 nil)
      (ImageIO/write rgb "jpg" target)
      (finally
        (.dispose g)))))

(defn- write-data-url-jpg! [src target]
  (let [[_ payload] (str/split src #"," 2)
        bytes (.decode (Base64/getDecoder) payload)]
    (with-open [in (io/input-stream bytes)]
      (draw-jpg! (ImageIO/read in) target))))

(defn- upload-source-file [src]
  (let [filename (cond
                   (str/starts-with? src (:path config))
                   (subs src (count (:path config)))

                   (str/starts-with? src "/uploads/")
                   (subs src (count "/uploads/"))

                   (not (str/includes? src "/"))
                   src

                   :else nil)
        candidates (when filename
                     [(io/file (uploads-dir) filename)])]
    (first (filter #(.exists %) candidates))))

(defn- finalize-image! [src servicio-id]
  (let [src (str/trim (or src ""))
        filename (str "servicio" servicio-id ".jpg")
        target (io/file (uploads-dir) filename)]
    (cond
      (str/blank? src)
      nil

      (= src (upload-url filename))
      src

      (data-url? src)
      (do
        (write-data-url-jpg! src target)
        (upload-url filename))

      :else
      (if-let [source (upload-source-file src)]
        (do
          (draw-jpg! (ImageIO/read source) target)
          (upload-url filename))
        src))))

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
        (let [servicio-id (parse-int id)
              final-data (assoc data :imagen (finalize-image! (:imagen data) servicio-id))]
          (model/update! id final-data)
          (json-ok {:id servicio-id}))
        (let [new-id (model/create! (assoc data :imagen nil))]
          (model/update! new-id (assoc data :imagen (finalize-image! (:imagen data) new-id)))
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
