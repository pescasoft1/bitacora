(ns bitacora.hooks.cargas_gasolina
  "Business logic hooks for cargas_gasolina entity.
   
   SENIOR DEVELOPER: Implement custom business logic here.
   
   See: HOOKS_GUIDE.md for detailed documentation and examples.
   Example: src/bitacora/hooks/alquileres.clj
   
   Uncomment the hooks you need and implement the logic."
  (:require [clojure.string :as str]
            [bitacora.models.util :refer [image-link]]))

;; =============================================================================
;; Validators
;; =============================================================================

;; Example validator function:
;; (defn validate-dates
;;   "Validates that end date is after start date"
;;   [params]
;;   (let [start (:start_date params)
;;         end (:end_date params)]
;;     (when (and start end)
;;       ;; Add your validation logic here
;;       nil)))  ; Return nil if valid, or {:field "error message"}

;; =============================================================================
;; Computed Fields
;; =============================================================================

;; Example computed field:
;; (defn compute-total
;;   "Computes total from quantity and price"
;;   [row]
;;   (* (or (:quantity row) 0)
;;      (or (:price row) 0)))

;; =============================================================================
;; Lifecycle Hooks
;; =============================================================================

(defn before-load
  "Hook executed before loading records.
   
   Use cases:
   - Filter by user permissions
   - Add default filters
   - Log access
   
   Args: [params] - Query parameters
   Returns: Modified params map"
  [params]
  ;; TODO: Add your logic here
  (println "[INFO] Loading cargas_gasolina with params:" params)
  params)

(defn- format-kilometros-por-litro
  [km litros]
  (when (and (number? km)
             (number? litros)
             (pos? km)
             (pos? litros))
    (format "Kilómetros por litro: %.2f" (/ km litros))))

(defn- append-observaciones
  [observaciones texto]
  (let [observaciones (str/trim (or observaciones ""))]
    (if (str/blank? observaciones)
      texto
      (if (str/includes? (str/lower-case observaciones) "kilómetros por litro")
        observaciones
        (str observaciones " · " texto)))))

(defn- add-kpl-observaciones
  "Adds km/l observation to the latest record of a vehicle when a prior record exists."
  [rows]
  (let [by-vehiculo (group-by :vehiculo_id rows)
        computed (reduce
                   (fn [acc [vehiculo-id vehiculo-rows]]
                     (let [sorted-rows (sort #(let [date-a (or (:fecha %1) "")
                                                   date-b (or (:fecha %2) "")
                                                   cmp (compare date-b date-a)]
                                               (if (zero? cmp)
                                                 (compare (:id %2) (:id %1))
                                                 cmp))
                                             vehiculo-rows)
                           annotated (map (fn [current previous]
                                            (if (and previous
                                                     (number? (:odometro current))
                                                     (number? (:odometro previous))
                                                     (number? (:litros current))
                                                     (pos? (:litros current))
                                                     (> (:odometro current) (:odometro previous)))
                                              (let [km (- (:odometro current) (:odometro previous))
                                                    texto (format-kilometros-por-litro km (:litros current))]
                                                (if texto
                                                  (assoc current :observaciones
                                                         (append-observaciones (:observaciones current) texto))
                                                  current))
                                              current))
                                          sorted-rows
                                          (concat (rest sorted-rows) [nil]))]
                       (into acc annotated)))
                   []
                   by-vehiculo)]
    (let [rows-by-id (into {} (map (fn [row] [(:id row) row]) computed))]
      (mapv #(get rows-by-id (:id %) %) rows))))

(defn after-load
  "Hook executed after loading records.
   
   Use cases:
   - Add computed fields
   - Format data
   - Enrich with lookups
   
   Args: [rows params] - Loaded rows and query params
   Returns: Modified rows vector"
  [rows params]
  (println "[INFO] Loaded" (count rows) "cargas_gasolina record(s)")
  ;; Transform file fields to image links and add computed km/l observations
  (->> rows
       (map #(-> %
                 (assoc :imagen (image-link (:imagen %)))))
       (add-kpl-observaciones)))

(defn before-save
  "Hook executed before saving a record.
   
   Use cases:
   - Validate data
   - Set defaults
   - Transform values
   - Check permissions
   
   Args: [params] - Form data to be saved
   Returns: Modified params map OR {:errors {...}} if validation fails"
  [params]
  (println "[INFO] Saving cargas_gasolina...")

  ;; Handle file upload for imagen field
  ;; The system expects :file key, but our field is named :imagen
  (if-let [file-data (:imagen params)]
    (if (and (map? file-data) (:tempfile file-data))
      ;; It's a file upload - move it to :file key so build-form-save finds it
      (-> params
          (assoc :file file-data)
          (dissoc :imagen))
      ;; It's already a string (existing filename) - keep as is
      params)
    params))

(defn after-save
  "Hook executed after successfully saving a record.
   
   Use cases:
   - Send notifications
   - Update related records
   - Create audit logs
   - Trigger workflows
   
   Args: [entity-id params] - Saved record ID and data
   Returns: {:success true}"
  [entity-id params]
  ;; TODO: Add post-save logic
  (println "[INFO] Cargas_gasolina saved successfully. ID:" entity-id)
  {:success true})

(defn before-delete
  "Hook executed before deleting a record.
   
   Use cases:
   - Check for related records
   - Verify permissions
   - Prevent deletion if constraints
   
   Args: [entity-id] - ID of record to delete
   Returns: {:success true} to allow, or {:errors {...}} to prevent"
  [entity-id]
  ;; TODO: Add pre-delete checks
  (println "[INFO] Checking if cargas_gasolina can be deleted. ID:" entity-id)
  {:success true})

(defn after-delete
  "Hook executed after successfully deleting a record.
   
   Use cases:
   - Delete related files
   - Update related records
   - Send notifications
   - Archive data
   
   Args: [entity-id] - ID of deleted record
   Returns: {:success true}"
  [entity-id]
  ;; TODO: Add post-delete logic
  (println "[INFO] Cargas_gasolina deleted successfully. ID:" entity-id)
  {:success true})
