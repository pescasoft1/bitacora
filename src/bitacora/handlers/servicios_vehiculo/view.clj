(ns bitacora.handlers.servicios_vehiculo.view
  (:require [clojure.string :as str]
            [hiccup.util :refer [raw-string]]
            [ring.util.anti-forgery :refer [anti-forgery-field]]))

;; ─────────────────────────────────────────
;; Helpers
;; ─────────────────────────────────────────

(defn- fmt-fecha [f]
  (when f
    (try
      (.format
       (java.time.LocalDate/parse (subs (str f) 0 10))
       (java.time.format.DateTimeFormatter/ofPattern "dd/MM/yyyy"))
      (catch Exception _ f))))

(defn- fmt-num [n decimals]
  (when n
    (format (str "%." decimals "f") (double n))))

(defn- selected? [a b]
  (= (str a) (str b)))

;; ─────────────────────────────────────────
;; Modal Imagen
;; ─────────────────────────────────────────

(defn- image-modal []
  [:div#imgModal.modal.fade
   {:tabindex "-1"
    :aria-hidden "true"}

   [:div.modal-dialog.modal-dialog-centered
    {:style "max-width:90vw"}

    [:div.modal-content
     {:style "background:#111;border:none"}

     [:div.modal-header
      {:style "border:none;padding:8px 12px"}

      [:span#imgModal-label.text-white.small ""]

      [:button.btn-close.btn-close-white
       {:type "button"
        :data-bs-dismiss "modal"}]]

     [:div.modal-body.text-center.p-2

      [:img#imgModal-img
       {:src ""
        :style "max-width:100%;max-height:80vh;object-fit:contain;border-radius:4px"}]]]]])

;; ─────────────────────────────────────────
;; Campo Imagen
;; ─────────────────────────────────────────

(defn- image-field [field-id label current-val]

  [:div.col-md-4

   [:label.form-label.fw-semibold label]

   [:input
    {:type "hidden"
     :id field-id
     :value (or current-val "")}]

   [:input
    {:type "file"
     :id (str field-id "-file")
     :accept "image/*"
     :class "d-none"
     :onchange (str "ServiciosVehiculo.onFileSelected('" field-id "')")}]

   [:div.input-group.mb-2

    [:input.form-control.form-control-sm
     {:type "text"
      :id (str field-id "-display")
      :placeholder "Sin imagen"
      :readonly true
      :value (if current-val "Imagen cargada" "")
      :style "cursor:pointer;background:#fff"
      :onclick (str "document.getElementById('" field-id "-file').click()")}]

    [:button.btn.btn-sm.btn-outline-secondary
     {:type "button"
      :onclick (str "document.getElementById('" field-id "-file').click()")}
     "📁"]

    [:button.btn.btn-sm.btn-outline-primary
     {:type "button"
      :id (str field-id "-view")
      :disabled (not current-val)
      :onclick (str "ServiciosVehiculo.openModal('" field-id "')")}
     "👁"]

    [:button.btn.btn-sm.btn-outline-danger
     {:type "button"
      :id (str field-id "-clear")
      :style (str "display:" (if current-val "inline-block" "none"))
      :onclick (str "ServiciosVehiculo.clearImage('" field-id "')")}
     "✕"]]

   [:div {:id (str field-id "-preview")
          :class "mb-1"}

    (when current-val

      [:img
       {:id (str field-id "-thumb")
        :src current-val
        :class "img-thumbnail"
        :style "max-height:90px;max-width:100%;object-fit:cover;cursor:pointer;border-radius:4px"
        :onclick (str "ServiciosVehiculo.openModal('" field-id "')")}])]])

;; ─────────────────────────────────────────
;; Botones Grid
;; ─────────────────────────────────────────

(defn- btn-edit [id]
  [:a.btn.btn-sm.btn-outline-secondary
   {:href (str "/servicios-vehiculo/editar/" id)}
   "Editar"])

(defn- btn-ver [id]
  [:a.btn.btn-sm.btn-outline-primary
   {:href (str "/servicios-vehiculo/ver/" id)
    :target "_blank"}
   "Ver"])

(defn- btn-del [id]
  [:button.btn.btn-sm.btn-outline-danger
   {:onclick (str "ServiciosVehiculo.delete(" id ")")}
   "Eliminar"])

;; ─────────────────────────────────────────
;; INDEX
;; ─────────────────────────────────────────

(defn index-view [_req lista fecha-inicio fecha-fin]

  (let [csrf (anti-forgery-field)]

    [:div.container.mt-4

     [:div.d-flex.justify-content-between.align-items-center.mb-3.flex-wrap.gap-2

      [:h3.mb-0 "Servicios de Vehículos"]

      [:div.d-flex.gap-2.flex-wrap

       [:a.btn.btn-primary
        {:href "/servicios-vehiculo/nuevo"}
        "➕ Nuevo Servicio"]

       [:button.btn.btn-outline-secondary
        {:type "button"
         :onclick "ServiciosVehiculo.printList()"}
        "🖨 Imprimir listado"]

       [:button.btn.btn-outline-success
        {:type "button"
         :onclick "ServiciosVehiculo.exportExcel()"}
        "📊 Exportar Excel"]]]

     ;; FILTRO
     [:div.card.mb-3

      [:div.card-body

       [:form.row.g-2.align-items-end
        {:method "get"
         :action "/servicios-vehiculo"}

        [:div.col-md-3

         [:label.form-label.fw-semibold "Fecha inicial"]

         [:input.form-control
          {:type "date"
           :name "fecha_inicio"
           :value (or fecha-inicio "")}]]

        [:div.col-md-3

         [:label.form-label.fw-semibold "Fecha final"]

         [:input.form-control
          {:type "date"
           :name "fecha_fin"
           :value (or fecha-fin "")}]]

        [:div.col-md-6.d-flex.gap-2

         [:button.btn.btn-primary
          {:type "submit"}
          "Buscar"]

         [:a.btn.btn-outline-secondary
          {:href "/servicios-vehiculo"}
          "Limpiar"]]]]]

     (raw-string csrf)

     ;; GRID
     [:div.table-responsive

      [:table#servicios-grid.table.table-striped.table-hover.align-middle.small

       [:thead.table-dark

        [:tr
         [:th "ID"]
         [:th "Fecha"]
         [:th "Vehículo"]
         [:th "Conductor"]
         [:th "Tipo Servicio"]
         [:th "Reparación"]
         [:th "Monto"]
         [:th "Imagen"]
         [:th "Acciones"]]]

       [:tbody

        (if (empty? lista)

          [:tr
           [:td {:colSpan 9
                 :class "text-center text-muted"}
            "No hay servicios registrados."]]

          (for [s lista]

            [:tr

             [:td (:id s)]

             [:td (fmt-fecha (:fecha s))]

             [:td
              [:span.fw-semibold (or (:placa s) "—")]
              [:br]
              [:small.text-muted
               (or (:vehiculo_nombre s) "")]]

             [:td
              (or (:conductor_nombre s) "—")]

             [:td
              (or (:tipo_servicio_nombre s) "—")]

             [:td.small.text-wrap
              {:style "max-width:260px;white-space:normal"}
              (or (:reparacion s) "—")]

             [:td.text-end
              "$"
              (fmt-num (:monto s) 2)]

             [:td.text-center

              (if (:image s)

                [:img.img-thumbnail
                 {:src (:image s)
                  :style "height:42px;width:42px;object-fit:cover;cursor:pointer"
                  :onclick (str "ServiciosVehiculo.openModalSrc('" (:image s) "','Servicio #" (:id s) "')")}]

                [:span.text-muted "—"])]

             [:td.text-nowrap

              [:div.d-flex.gap-1.flex-wrap

               (btn-ver (:id s))
               (btn-edit (:id s))
               (btn-del (:id s))]]]))]]]

     (image-modal)

     [:script {:src "/js/servicios-vehiculo.js"}]]))

;; ─────────────────────────────────────────
;; EDITAR / NUEVO
;; ─────────────────────────────────────────

(defn edit-view [_req servicio vehiculos conductores tipos]

  (let [csrf (anti-forgery-field)
        nuevo? (nil? servicio)]

    [:div.container.mt-3

     [:h3.mb-3
      (if nuevo?
        "Nuevo Servicio de Vehículo"
        "Editar Servicio de Vehículo")]

     (raw-string csrf)

     [:input#servicio-id
      {:type "hidden"
       :value (or (:id servicio) "")}]

     [:div.mb-3.d-flex.gap-2.flex-wrap

      [:a.btn.btn-secondary
       {:href "/servicios-vehiculo"}
       "← Volver"]

      [:button.btn.btn-success
       {:onclick "ServiciosVehiculo.save()"}
       "💾 Guardar"]

      (when-not nuevo?

        [:button.btn.btn-outline-danger
         {:onclick (str "ServiciosVehiculo.delete(" (:id servicio) ")")}
         "Eliminar"])]

     [:div.card.mb-3

      [:div.card-header.fw-bold
       "Datos del Servicio"]

      [:div.card-body

       [:div.row.g-3

        ;; Vehículo
        [:div.col-md-4

         [:label.form-label.fw-semibold "Vehículo"]

         [:select#vehiculo_id.form-select

          [:option {:value ""}
           "-- Seleccionar --"]

          (for [v vehiculos]

            [:option
             {:value (:id v)
              :selected (selected? (:id v) (:vehiculo_id servicio))}
             (str (:placa v)
                  " — "
                  (:nombre v)
                  " "
                  (:modelo v))])]]

        ;; Conductor
        [:div.col-md-4

         [:label.form-label.fw-semibold "Conductor"]

         [:select#conductor_id.form-select

          [:option {:value ""}
           "-- Seleccionar --"]

          (for [d conductores]

            [:option
             {:value (:id d)
              :selected (selected? (:id d) (:conductor_id servicio))}
             (:nombre d)])]]

        ;; Tipo servicio
        [:div.col-md-4

         [:label.form-label.fw-semibold "Tipo de Servicio"]

         [:select#tipo_servicio_id.form-select

          [:option {:value ""}
           "-- Seleccionar --"]

          (for [t tipos]

            [:option
             {:value (:id t)
              :selected (selected? (:id t) (:tipo_servicio_id servicio))}
             (:nombre t)])]]

        ;; Fecha
        [:div.col-md-4

         [:label.form-label.fw-semibold "Fecha"]

         [:input#fecha.form-control
          {:type "date"
           :value (or
                   (when (:fecha servicio)
                     (subs (str (:fecha servicio)) 0 10))
                   "")}]]

        ;; Monto
        [:div.col-md-4

         [:label.form-label.fw-semibold "Monto"]

         [:input#monto.form-control
          {:type "number"
           :step "0.01"
           :min "0"
           :value (or (fmt-num (:monto servicio) 2) "")}]]

        ;; Imagen
        (image-field "image"
                     "Foto/Imagen Recibo del Servicio"
                     (:image servicio))

        ;; Observaciones
        [:div.col-12

         [:label.form-label.fw-semibold
          "Reparación / Observaciones"]

         [:textarea#reparacion.form-control
          {:rows 4}
          (or (:reparacion servicio) "")]]]]]

     (image-modal)

     [:script {:src "/js/servicios-vehiculo.js"}]]))

;; ─────────────────────────────────────────
;; PRINT
;; ─────────────────────────────────────────

(defn print-view [_req servicio]

  [:div.container

   [:div.text-center.mb-4

    [:h3 "SERVICIO DE VEHÍCULO"]

    [:p
     "Folio #"
     [:strong (:id servicio)]]]

   [:div.row.mb-3

    [:div.col-6

     [:table.table.table-sm.table-bordered

      [:tbody

       [:tr
        [:th "Vehículo"]
        [:td
         (str (:placa servicio)
              " — "
              (:vehiculo_nombre servicio))]]

       [:tr
        [:th "Modelo"]
        [:td (:modelo servicio)]]

       [:tr
        [:th "Conductor"]
        [:td (:conductor_nombre servicio)]]

       [:tr
        [:th "Tipo Servicio"]
        [:td (:tipo_servicio_nombre servicio)]]

       [:tr
        [:th "Fecha"]
        [:td (fmt-fecha (:fecha servicio))]]]]]

    [:div.col-6

     [:table.table.table-sm.table-bordered

      [:tbody

       [:tr
        [:th "Monto"]
        [:td.text-end
         "$"
         (fmt-num (:monto servicio) 2)]]

       [:tr
        [:th "Reparación"]
        [:td (:reparacion servicio)]]]]]]

   (when (:reparacion servicio)

     [:div.mb-3

      [:strong "Reparación / Observaciones: "]

      (:reparacion servicio)])

   ;; Imagen
   [:div.row

    (when (:image servicio)

      [:div.col-md-6.mb-3

       [:p.fw-semibold "Foto/Imagen Recibo del Servicio"]

       [:img.img-fluid
        {:src (:image servicio)
         :style "max-height:280px;object-fit:contain;border:1px solid #ccc;border-radius:4px;cursor:pointer"
         :onclick (str "ServiciosVehiculo.openModalSrc('" (:image servicio) "','Foto/Imagen Recibo del Servicio')")}]])]

   (image-modal)

   [:button.btn.btn-primary.mt-3.d-print-none
    {:onclick "window.print()"}
    "🖨 Imprimir"]

   [:script {:src "/js/servicios-vehiculo.js"}]])