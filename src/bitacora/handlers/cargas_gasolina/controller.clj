(ns bitacora.handlers.cargas_gasolina.controller
  (:require [bitacora.handlers.cargas_gasolina.model :as model]
            [bitacora.handlers.cargas_gasolina.view  :as view]
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

(defn- format-kpl [km litros]
  (when (and (number? km)
             (number? litros)
             (pos? km)
             (pos? litros))
    (format "Kilómetros por litro: %.2f" (/ km litros))))

(defn- format-rendimiento-porcentaje [actual target]
  (when (number? actual)
    (if (and (number? target) (pos? target))
      (let [pct (* 100 (/ (- actual target) target))]
        (format "Porcentaje rendimiento: %s%.2f%%"
                (if (pos? pct) "+" "")
                pct))
      "Porcentaje rendimiento: N/D")))

(defn- append-kpl-observaciones [observaciones texto]
  (let [observaciones (str/trim (or observaciones ""))
        texto-lower    (str/lower-case texto)
        base-lower     (str/lower-case observaciones)]
    (cond
      (str/blank? observaciones)
      texto

      (and (str/includes? texto-lower "kilómetros por litro")
           (str/includes? base-lower "kilómetros por litro"))
      observaciones

      (and (str/includes? texto-lower "porcentaje rendimiento")
           (str/includes? base-lower "porcentaje rendimiento"))
      observaciones

      :else
      (str observaciones " · " texto))))

(defn- compute-kpl [current previous]
  (if (and previous
           (number? (:odometro current))
           (number? (:odometro previous))
           (number? (:litros current))
           (pos? (:litros current))
           (> (:odometro current) (:odometro previous)))
    (let [km        (- (:odometro current) (:odometro previous))
          actual    (/ km (:litros current))
          pct       (when (and (number? (:vehiculo_rendimientoxkm current))
                               (pos? (:vehiculo_rendimientoxkm current)))
                      (* 100 (/ (- actual (:vehiculo_rendimientoxkm current))
                                (:vehiculo_rendimientoxkm current))))
          kpl-text  (format-kpl km (:litros current))
          perf-text (format-rendimiento-porcentaje actual (:vehiculo_rendimientoxkm current))
          textos    (remove nil? [kpl-text perf-text])]
      (cond-> current
        true (assoc :rendimiento_p_km actual
                    :porcentaje_rendimiento pct)
        (seq textos) (assoc :observaciones
                             (reduce append-kpl-observaciones (:observaciones current) textos))))
    current))

(defn- with-kpl-list [rows]
  (let [rows-by-vehiculo (group-by :vehiculo_id rows)
        computed (reduce
                   (fn [acc [_ vehiculo-rows]]
                     (let [sorted (sort #(let [date-a (or (:fecha %1) "")
                                               date-b (or (:fecha %2) "")
                                               cmp (compare date-b date-a)]
                                           (if (zero? cmp)
                                             (compare (:id %2) (:id %1))
                                             cmp))
                                         vehiculo-rows)
                           annotated (map (fn [current previous]
                                            (or (compute-kpl current previous) current))
                                          sorted
                                          (concat (rest sorted) [nil]))]
                       (into acc annotated)))
                   []
                   rows-by-vehiculo)]
    (let [rows-by-id (into {} (map (fn [row] [(:id row) row]) computed))]
      (mapv #(get rows-by-id (:id %) %) rows))))

(defn- with-kpl-single [carga]
  (if (and carga (:vehiculo_id carga))
    (let [ultimo (model/get-ultimo-odometro (:vehiculo_id carga) (:id carga))]
      (or (compute-kpl carga ultimo) carga))
    carga))

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

(defn- uploads-dir []
  (doto (io/file (:uploads config))
    (.mkdirs)))

(defn- upload-url [filename]
  (str (:path config) filename))

(defn- data-url? [s]
  (and (string? s) (str/starts-with? s "data:image/")))

(defn- final-image-url? [s filename]
  (= s (upload-url filename)))

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
                     [(io/file (uploads-dir) filename)
                      (io/file "resources/public/uploads" filename)])]
    (first (filter #(.exists %) candidates))))

(defn- finalize-image! [src carga-id slot]
  (let [src (str/trim (or src ""))
        filename (str "cargas" carga-id slot ".jpg")
        target (io/file (uploads-dir) filename)]
    (cond
      (str/blank? src)
      nil

      (final-image-url? src filename)
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

(defn- req-param [request k]
  (or (get-in request [:params k])
      (get-in request [:params (name k)])
      (get-in request [:multipart-params k])
      (get-in request [:multipart-params (name k)])))

(defn subir-imagen [request]
  (try
    (let [file (req-param request :foto)]

      (when-not file
        (throw (ex-info "No se recibió archivo." {})))

      (let [temp     (or (:tempfile file) (get file "tempfile"))
            original (or (:filename file) (get file "filename"))
            ext      (or (when original (re-find #"\.[A-Za-z0-9]+$" original)) ".jpg")
            nombre   (str "tmp-cargas-" (java.util.UUID/randomUUID) ext)
            dir      (uploads-dir)
            destino  (io/file dir nombre)]

        (when-not temp
          (throw (ex-info "No se recibió archivo temporal." {})))

        (io/copy temp destino)

        (json-ok {:url (upload-url nombre)})))

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

        lista0       (with-kpl-list lista0)

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
        carga       (with-kpl-single (model/get-by-id id))
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
          (let [final-data (assoc data
                                  :imagen (finalize-image! (:imagen data) id 1)
                                  :ticket_imagen (finalize-image! (:ticket_imagen data) id 2))]
            (model/update! id final-data)
            (json-ok {:id id}))
          (let [new-id (model/create! (assoc data :imagen nil :ticket_imagen nil))
                final-data (assoc data
                                  :imagen (finalize-image! (:imagen data) new-id 1)
                                  :ticket_imagen (finalize-image! (:ticket_imagen data) new-id 2))]
            (model/update! new-id final-data)
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
