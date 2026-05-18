(ns bitacora.handlers.cargas_gasolina.view
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

(defn- fmt-odo [n]
  (when n
    (format "%,d" (long n))))

(defn- selected? [a b]
  (= (str a) (str b)))

(defn- opt-combustible [val current]
  [:option {:value val :selected (selected? val current)} val])

;; ─────────────────────────────────────────
;; Modal de imagen
;; ─────────────────────────────────────────

(defn- image-modal []
  [:div#imgModal.modal.fade
   {:tabindex "-1" :aria-hidden "true"}
   [:div.modal-dialog.modal-dialog-centered
    {:style "max-width:90vw"}
    [:div.modal-content
     {:style "background:#111;border:none"}
     [:div.modal-header
      {:style "border:none;padding:8px 12px"}
      [:span#imgModal-label.text-white.small ""]
      [:button.btn-close.btn-close-white
       {:type "button" :data-bs-dismiss "modal"}]]
     [:div.modal-body.text-center.p-2
      [:img#imgModal-img
       {:src ""
        :style "max-width:100%;max-height:80vh;object-fit:contain;border-radius:4px"}]]]]])

;; ─────────────────────────────────────────
;; Campo de imagen
;; ─────────────────────────────────────────
(defn- image-field [field-id label current-val]
  [:div.col-md-4
   [:label.form-label.fw-semibold label]

   [:input {:type "hidden" :id field-id :value (or current-val "")}]

   [:input {:type     "file"
            :id       (str field-id "-file")
            :accept   "image/*"
            :capture  "environment"
            :class    "d-none"
            :onchange (str "CargasGasolina.onFileSelected('" field-id "')")}]

   [:div.input-group.mb-2
    [:input.form-control.form-control-sm
     {:type        "text"
      :id          (str field-id "-display")
      :placeholder "Sin imagen"
      :readonly    true
      :value       (if current-val "Imagen cargada" "")
      :style       "cursor:pointer;background:#fff"
      :onclick     (str "document.getElementById('" field-id "-file').click()")}]

    [:button.btn.btn-sm.btn-outline-secondary
     {:type    "button"
      :title   "Buscar imagen"
      :onclick (str "document.getElementById('" field-id "-file').click()")}
     "📁"]

    [:button.btn.btn-sm.btn-outline-primary
     {:type    "button"
      :title   "Tomar foto"
      :onclick (str "document.getElementById('" field-id "-file').click()")}
     "📷"]

    [:button.btn.btn-sm.btn-outline-primary
     {:type     "button"
      :id       (str field-id "-view")
      :title    "Ver imagen"
      :disabled (not current-val)
      :onclick  (str "CargasGasolina.openModal('" field-id "')")}
     "👁"]

    [:button.btn.btn-sm.btn-outline-danger
     {:type    "button"
      :id      (str field-id "-clear")
      :title   "Quitar imagen"
      :style   (str "display:" (if current-val "inline-block" "none"))
      :onclick (str "CargasGasolina.clearImage('" field-id "')")}
     "✕"]]

   [:div {:id (str field-id "-preview") :class "mb-1"}
    (when current-val
      [:img
       {:id      (str field-id "-thumb")
        :src     current-val
        :class   "img-thumbnail"
        :style   "max-height:90px;max-width:100%;object-fit:cover;cursor:zoom-in;border-radius:4px"
        :onclick (str "CargasGasolina.openModal('" field-id "')")
        :title   "Click para ver en grande"}])]])
;; ─────────────────────────────────────────
;; Botones de tabla
;; ─────────────────────────────────────────

(defn- btn-edit [id]
  [:a.btn.btn-sm.btn-outline-secondary {:href (str "/cargas-gasolina/editar/" id)} "Editar"])

(defn- btn-ver [id]
  [:a.btn.btn-sm.btn-outline-primary {:href (str "/cargas-gasolina/ver/" id) :target "_blank"} "Ver"])

(defn- btn-del [id]
  [:button.btn.btn-sm.btn-outline-danger {:onclick (str "CargasGasolina.delete(" id ")")} "Eliminar"])

(defn- sortable-th [label key]
  [:th {:style "cursor:pointer;user-select:none"
        :title (str "Ordenar por " label)
        :onclick (str "CargasGasolina.sortGrid('" key "')")}
   label])

;; ─────────────────────────────────────────
;; Index
;; ─────────────────────────────────────────
(defn index-view [_req lista fecha-inicio fecha-fin vehiculos vehiculo-id]
  (let [csrf (anti-forgery-field)]
    [:div.container.mt-4

     [:div.d-flex.justify-content-between.align-items-center.mb-3.flex-wrap.gap-2
      [:h3.mb-0 "Cargas de Gasolina"]
      [:div.d-flex.gap-2.flex-wrap
       [:a.btn.btn-primary {:href "/cargas-gasolina/nuevo"} "⛽ Nueva Carga"]
       [:button.btn.btn-outline-secondary {:type "button" :onclick "CargasGasolina.printList()"} "🖨 Imprimir listado"]
       [:button.btn.btn-outline-success {:type "button" :onclick "CargasGasolina.exportExcel()"} "📊 Exportar Excel"]]]

     [:div.card.mb-3
      [:div.card-body
       [:form.row.g-2.align-items-end {:method "get" :action "/cargas-gasolina"}
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
        
        [:div.col-md-4
         [:label.form-label.fw-semibold "Vehículo"]
         [:select.form-select
          {:name "vehiculo_id"}
          [:option {:value ""} "Todos los vehículos"]
          (for [v vehiculos]
            [:option {:value (:id v)
                      :selected (= (str (:id v)) (str vehiculo-id))}
             (str (:placa v) " — " (:nombre v) " " (:modelo v))])]]





        [:div.col-md-6.d-flex.gap-2
         [:button.btn.btn-primary {:type "submit"} "Buscar"]
         [:a.btn.btn-outline-secondary {:href "/cargas-gasolina"} "Limpiar"]]]]]

     (raw-string csrf)

     [:div.table-responsive
      [:table#cargas-grid.table.table-striped.table-hover.align-middle.small
       [:thead.table-dark
        [:tr
         [:th "ID"]
         [:th "Fecha"]
         [:th "Vehículo"]
         [:th "Conductor"]
         [:th "Litros"]
         [:th "$/Litro"]
         [:th "Total"]
         [:th "Odómetro"]
         [:th "Combustible"]
         [:th "Observaciones"]
         [:th "Imagen"]
         [:th "Ticket"]
         [:th "Acciones"]]]
       [:tbody
        (if (empty? lista)
          [:tr [:td {:colSpan 13 :class "text-center text-muted"} "No hay cargas registradas."]]
          (for [c lista]
            [:tr {:data-id (str (:id c))
                  :data-fecha (when (:fecha c) (subs (str (:fecha c)) 0 10))
                  :data-vehiculo (str/lower-case (str (or (:placa c) "") " " (or (:vehiculo_nombre c) "")))
                  :data-conductor (str/lower-case (str (or (:conductor_nombre c) "")))
                  :data-combustible (str/lower-case (str (or (:tipo_combustible c) "")))
                  :data-observaciones (str/lower-case (str (or (:observaciones c) "")))
                  :data-total (str (or (:total c) ""))
                  :data-litros (str (or (:litros c) ""))
                  :data-precio (str (or (:precio_litro c) ""))
                  :data-odometro (str (or (:odometro c) ""))}
             [:td (:id c)]
             [:td (fmt-fecha (:fecha c))]
             [:td [:span.fw-semibold (or (:placa c) "—")] [:br] [:small.text-muted (or (:vehiculo_nombre c) "")]]
             [:td (or (:conductor_nombre c) "—")]
             [:td.text-end (fmt-num (:litros c) 2)]
             [:td.text-end "$" (fmt-num (:precio_litro c) 2)]
             [:td.text-end.fw-bold "$" (fmt-num (:total c) 2)]
             [:td.text-end (when (:odometro c) (str (:odometro c) " km"))]
             [:td [:span.badge.bg-secondary (or (:tipo_combustible c) "—")]]
             [:td.small.text-wrap {:style "max-width:260px;white-space:normal"}
              (or (:observaciones c) "—")]
             [:td.text-center
              (if (:imagen c)
                [:img.img-thumbnail
                 {:src     (:imagen c)
                  :style   "height:42px;width:42px;object-fit:cover;cursor:zoom-in"
                  :onclick (str "CargasGasolina.openModalSrc('" (:imagen c) "','Imagen #" (:id c) "')")
                  :title   "Ver imagen"}]
                [:span.text-muted "—"])]
             [:td.text-center
              (if (:ticket_imagen c)
                [:img.img-thumbnail
                 {:src     (:ticket_imagen c)
                  :style   "height:42px;width:42px;object-fit:cover;cursor:zoom-in"
                  :onclick (str "CargasGasolina.openModalSrc('" (:ticket_imagen c) "','Ticket #" (:id c) "')")
                  :title   "Ver ticket"}]
                [:span.text-muted "—"])]
             [:td.text-nowrap
              [:div.d-flex.gap-1.flex-wrap
               (btn-ver (:id c))
               (btn-edit (:id c))
               (btn-del (:id c))]]]))]]]

     (image-modal)
     [:script {:src "/js/cargas-gasolina.js"}]]))

;; ─────────────────────────────────────────
;; Edit / Nuevo
;; ─────────────────────────────────────────

(defn edit-view [_req carga vehiculos conductores ultimo-odo]
  (let [csrf     (anti-forgery-field)
        nuevo?   (nil? carga)
        odo-ant  (when ultimo-odo (:odometro ultimo-odo))
        odo-act  (:odometro carga)
        diff     (when (and odo-ant odo-act) (- odo-act odo-ant))
        ref-text (if odo-ant
                   (str "Folio #" (:id ultimo-odo)
                        " · " (fmt-fecha (:fecha ultimo-odo))
                        " · " (fmt-odo odo-ant) " km")
                   "Sin carga anterior")]

    [:div.container.mt-3

     [:h3.mb-3 (if nuevo? "Nueva Carga de Gasolina" "Editar Carga de Gasolina")]

     (raw-string csrf)
     [:input#carga-id {:type "hidden" :value (or (:id carga) "")}]

     [:div.mb-3.d-flex.gap-2.flex-wrap
      [:a.btn.btn-secondary {:href "/cargas-gasolina"} "← Volver"]
      [:button.btn.btn-success {:onclick "CargasGasolina.save()"} "💾 Guardar"]
      (when-not nuevo?
        (list
         [:a.btn.btn-outline-primary {:href (str "/cargas-gasolina/ver/" (:id carga)) :target "_blank"} "🖨 Imprimir"]
         [:button.btn.btn-outline-danger {:onclick (str "CargasGasolina.delete(" (:id carga) ")")} "Eliminar"]))]

     [:div.card.mb-3
      [:div.card-header.fw-bold "Datos de la Carga"]
      [:div.card-body
       [:div.row.g-3

        [:div.col-md-4
         [:label.form-label.fw-semibold "Vehículo"]
        [:select#vehiculo_id.form-select
         {:onchange "CargasGasolina.onVehiculoChange(this.value);CargasGasolina.calcDiffOdo();"}
          [:option {:value ""} "-- Seleccionar --"]
          (for [v vehiculos]
            [:option {:value (:id v) :selected (selected? (:id v) (:vehiculo_id carga))}
             (str (:placa v) " — " (:nombre v) " " (:modelo v))])]]

        [:div.col-md-4
         [:label.form-label.fw-semibold "Conductor"]
         [:select#conductor_id.form-select
          [:option {:value ""} "-- Seleccionar --"]
          (for [d conductores]
            [:option {:value (:id d) :selected (selected? (:id d) (:conductor_id carga))}
             (:nombre d)])]]

        [:div.col-md-4
         [:label.form-label.fw-semibold "Fecha"]
         [:input#fecha.form-control
          {:type "date"
           :value (or (when (:fecha carga) (subs (str (:fecha carga)) 0 10)) "")}]]

        [:div.col-md-3
         [:label.form-label.fw-semibold "Tipo de Combustible"]
         [:select#tipo_combustible.form-select
          (opt-combustible "Magna"   (:tipo_combustible carga))
          (opt-combustible "Premium" (:tipo_combustible carga))
          (opt-combustible "Diesel"  (:tipo_combustible carga))]]

        [:div.col-md-3
         [:label.form-label.fw-semibold "Litros"]
         [:input#litros.form-control
          {:type "number"
           :step "0.01"
           :min "0"
           :value (or (fmt-num (:litros carga) 2) "")
           :oninput "CargasGasolina.calcTotal()"}]]

        [:div.col-md-3
         [:label.form-label.fw-semibold "Precio por Litro ($)"]
         [:input#precio_litro.form-control
          {:type "number"
           :step "0.01"
           :min "0"
           :value (or (fmt-num (:precio_litro carga) 2) "")
           :oninput "CargasGasolina.calcTotal()"}]]

        [:div.col-md-3
         [:label.form-label.fw-semibold "Total ($)"]
         [:input#total.form-control.fw-bold
          {:type "number"
           :step "0.01"
           :min "0"
           :value (or (fmt-num (:total carga) 2) "")
           :style "background:#f0fff4"}]]

        [:div.col-md-4
         [:label.form-label.fw-semibold "Odómetro (km)"]
        [:input#odometro.form-control
         {:type "number"
          :min "0"
          :value (or (:odometro carga) "")
          :oninput "CargasGasolina.calcDiffOdo()"
          :onkeyup "CargasGasolina.calcDiffOdo()"
          :onchange "CargasGasolina.calcDiffOdo(true)"
          :onblur "CargasGasolina.calcDiffOdo(true)"}]

         [:div#odo-error-msg
          {:style "display:none;color:#dc3545;font-size:.82rem;font-weight:600;margin-top:4px"}]

         [:div#odo-ref-box.mt-2.p-2.rounded
          {:style "background:#f8f9fa;border:1px solid #dee2e6;font-size:.85rem;display:block"}

          [:label.form-label.fw-semibold.mb-1 "Última carga registrada"]

          [:input.form-control.form-control-sm.mb-2
           {:type "text"
            :id "odo-ant-input"
            :readonly true
            :value ref-text
            :style "background:#fff"}]

          [:input {:type "hidden"
                   :id "odo-ant-val"
                   :value (str (or odo-ant ""))
                   :data-odo-ant (str (or odo-ant ""))}]

          [:div#odo-diff-row.mt-1
           {:style (cond
                     (nil? odo-ant)        "color:#6c757d"
                     (and diff (> diff 0)) "color:#198754"
                     :else                 "color:#dc3545")}
           "Diferencia: "
           [:strong#odo-diff-val
            (cond
              (nil? odo-ant) "—"
              (nil? odo-act) "—"
              (> diff 0)     (str "+" diff " km")
              :else          (str diff " km"))]]]]

        (image-field "imagen" "Foto/Imagen del Odometro" (:imagen carga))
        (image-field "ticket_imagen" "Foto/Imagen del Ticket" (:ticket_imagen carga))

        [:div.col-12
         [:label.form-label.fw-semibold "Observaciones"]
         [:textarea#observaciones.form-control
          {:rows 4}
          (or (:observaciones carga) "")]]]]]

     (image-modal)

     [:script {:src "/js/cargas-gasolina.js"}]]))

;; ─────────────────────────────────────────
;; Print / Ver
;; ─────────────────────────────────────────

(defn print-view [_req carga]
  [:div.container

   [:div.text-center.mb-4
    [:h3 "CARGA DE GASOLINA"]
    [:p "Folio #" [:strong (:id carga)]]]

   [:div.row.mb-3
    [:div.col-6
     [:table.table.table-sm.table-bordered
      [:tbody
       [:tr [:th "Vehículo"]  [:td (str (:placa carga) " — " (:vehiculo_nombre carga))]]
       [:tr [:th "Modelo"]    [:td (:modelo carga)]]
       [:tr [:th "Conductor"] [:td (:conductor_nombre carga)]]
       [:tr [:th "Fecha"]     [:td (fmt-fecha (:fecha carga))]]]]]
    [:div.col-6
     [:table.table.table-sm.table-bordered
      [:tbody
       [:tr [:th "Combustible"] [:td (:tipo_combustible carga)]]
       [:tr [:th "Litros"]      [:td.text-end (fmt-num (:litros carga) 2)]]
       [:tr [:th "$/Litro"]     [:td.text-end "$" (fmt-num (:precio_litro carga) 2)]]
       [:tr [:th.fw-bold "Total"] [:td.text-end.fw-bold "$" (fmt-num (:total carga) 2)]]
       [:tr [:th "Odómetro"]   [:td.text-end (when (:odometro carga) (str (:odometro carga) " km"))]]]]]]

   (when (:observaciones carga)
     [:div.mb-3 [:strong "Observaciones: "] (:observaciones carga)])

   [:div.row
    (when (:imagen carga)
      [:div.col-md-6.mb-3
       [:p.fw-semibold "Foto/Imagen del Odometro:"]
       [:img.img-fluid
        {:src     (:imagen carga)
         :style   "max-height:280px;object-fit:contain;border:1px solid #ccc;border-radius:4px;cursor:zoom-in"
         :onclick (str "CargasGasolina.openModalSrc('" (:imagen carga) "','Imagen de la Carga')")
         :title   "Click para ver en grande"}]])

    (when (:ticket_imagen carga)
      [:div.col-md-6.mb-3
       [:p.fw-semibold "Foto/Imagen del Ticket:"]
       [:img.img-fluid
        {:src     (:ticket_imagen carga)
         :style   "max-height:280px;object-fit:contain;border:1px solid #ccc;border-radius:4px;cursor:zoom-in"
         :onclick (str "CargasGasolina.openModalSrc('" (:ticket_imagen carga) "','Ticket')")
         :title   "Click para ver en grande"}]])]

   (image-modal)

   [:button.btn.btn-primary.mt-3.d-print-none {:onclick "window.print()"} "🖨 Imprimir"]
   [:script {:src "/js/cargas-gasolina.js"}]])