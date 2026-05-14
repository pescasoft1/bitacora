(ns bitacora.models.routes
  (:require
   [clojure.string :as str]))

(defn- get-base-ns
  "Gets the base namespace (project name) from the current namespace"
  []
  (-> (str *ns*)
      (str/split #"\.")
      first))

(defn- proutes-path
  "Returns the path to proutes.clj for this project"
  []
  (str "src/" (get-base-ns) "/routes/proutes.clj"))

(defn build-grid-defroutes
  "Genera rutas especificas para el grid para una tabla"
  [table]
  (let [data (str
              "  (GET \"/admin/" table "\" params [] (" table "-controller/" table " params))\n"
              "  (GET \"/admin/" table "/add-form\" params [] (" table "-controller/" table "-add-form params))\n"
              "  (GET \"/admin/" table "/edit-form/:id\" [id :as request] (" table "-controller/" table "-edit-form request id))\n"
              "  (POST \"/admin/" table "/save\" params [] (" table "-controller/" table "-save params))\n"
              "  (GET \"/admin/" table "/delete/:id\" [id :as request] (" table "-controller/" table "-delete request id))")]
    data))

(defn build-defroutes
  "Genera rutas especificas para un dashboard para una tabla"
  [table]
  (let [data (str
              "  (GET \"/" table "\" params [] (" table "-dashboard/" table " params))")]
    data))

(defn build-defroutes-reporte
  "Genera rutas especificas para reportes"
  [table]
  (let [data (str
              "  (GET \"/reports/" table "\" params [] (" table "-report/" table " params))")]
    data))

(defn build-grid-require
  "Genera archivos requeridos para un grid para una tabla"
  [table]
  (let [data (str
              "   [bitacora.handlers.admin."
              table
              ".controller :as "
              table
              "-controller]")]
    data))

(defn build-reporte-require
  "Genera archivos requeridos para un reporte"
  [table]
  (let [data (str
              "   [bitacora.handlers.reports."
              table
              ".controller :as "
              table
              "-report]")]
    data))

(defn build-require
  "Genera archivos requeridos para un dashboard para una tabla"
  [table]
  (let [data (str
              "   [bitacora.handlers."
              table
              ".controller :as "
              table
              "-dashboard]")]
    data))

(defn insert-lines-after-search [file-path lines-to-insert search-str]
  (let [file-contents (slurp file-path)
        lines (str/split-lines file-contents)
        found-index (first
                     (keep-indexed
                      (fn [idx line]
                        (when (str/includes? line search-str)
                          idx))
                      lines))
        insert-at (if found-index (inc found-index) (count lines))
        lines-before (take insert-at lines)
        lines-after (drop insert-at lines)
        existing-lines-set (set lines)
        new-lines-to-insert
        (filter #(not (contains? existing-lines-set %))
                lines-to-insert)
        new-lines
        (concat lines-before new-lines-to-insert lines-after)
        new-file-contents (str/join "\n" new-lines)]
    (when (seq new-lines-to-insert)
      (spit file-path new-file-contents))))

(defn process-grid
  "Actualiza proutes.clj"
  [table]
  (insert-lines-after-search
   (proutes-path)
   [(build-grid-require table)]
   "[compojure.core :refer [defroutes GET POST")

  (insert-lines-after-search
   (proutes-path)
   [(build-grid-defroutes table)]
   "(defroutes proutes"))

(defn process-dashboard
  "Actualiza proutes.clj"
  [table]
  (insert-lines-after-search
   (proutes-path)
   [(build-require table)]
   "[compojure.core :refer [defroutes GET POST")

  (insert-lines-after-search
   (proutes-path)
   [(build-defroutes table)]
   "(defroutes proutes"))

(defn process-report
  "Actualiza proutes.clj"
  [table]
  (insert-lines-after-search
   (proutes-path)
   [(build-reporte-require table)]
   "[compojure.core :refer [defroutes GET POST")

  (insert-lines-after-search
   (proutes-path)
   [(build-defroutes-reporte table)]
   "(defroutes proutes"))