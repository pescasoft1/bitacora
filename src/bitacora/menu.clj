(ns bitacora.menu
  "Menu configuration - auto-generated from entity configs with manual overrides"
  (:require
   [bitacora.engine.menu :as auto-menu]
   [clojure.pprint :as pp]
   [clojure.string :as str]))

;; ============================================================================
;; CUSTOM NAVIGATION LINKS
;; ============================================================================

(def custom-nav-links
  "Custom navigation links (non-dropdown, not entity-based)"
  [["/" "HOME" "bi bi-house" nil 0]
   ["/dashboard" "Dashboard" "bi bi-speedometer2" "U" 10]])






;; ============================================================================
;; CUSTOM DROPDOWNS
;; ============================================================================

(def custom-dropdowns

  {:Gasolina
   {:id      "navdrop-gas"
    :data-id "Gasolina"
    :label   "Gasolina"
    :order   60
    :icon    "bi bi-fuel-pump"

    :items   [["/cargas-gasolina"
               "Cargas de Gasolina"
               "bi bi-fuel-pump"
               "U"
               10]

              ["/cargas-gasolina/nuevo"
               "Nueva Carga"
               "bi bi-plus-circle"
               "U"
               20]]}

   :Servicios
   {:id      "navdrop-servicios"
    :data-id "Servicios"
    :label   "Servicios"
    :order   70
    :icon    "bi bi-tools"

    :items   [["/servicios-vehiculo"
               "Servicios de Vehículos"
               "bi bi-tools"
               "U"
               10]

              ["/servicios-vehiculo/nuevo"
               "Nuevo Servicio"
               "bi bi-plus-circle"
               "U"
               20]]}

   :Reports
   {:id "navdrop3"
    :data-id "Reports"
    :label "Reportes"
    :order 100
    :icon "bi bi-printer"
    :items [["/reports/conductores" "Conductores" "bi bi-people" "U" 10]
           ["/reports/control-kilometraje" "Control de Kilometraje" "bi bi-speedometer2" "U" 20]]

    
    }})

;; ============================================================================
;; PARSE CUSTOM MENU ITEM
;; ============================================================================

(defn ^:private parse-custom-menu-item
  "Parses a custom nav link or dropdown item vector."
  [[href label & rest]]

  (let [[rights order icon] rest
        [rights order icon]

        (cond
          (and rights
               (string? rights)
               (str/starts-with? rights "bi "))
          [nil 0 rights]

          (and order
               (string? order)
               (str/starts-with? order "bi "))
          [rights 0 order]

          :else
          [rights order icon])]

    {:href href
     :label label
     :rights (when rights (vector rights))
     :order (or order 0)
     :icon icon}))

;; ============================================================================
;; FORMAT CUSTOM NAV LINKS
;; ============================================================================

(defn ^:private format-custom-nav-link
  [link]
  (parse-custom-menu-item link))

(defn ^:private format-custom-nav-links
  [links]
  (map format-custom-nav-link links))

(defn ^:private combine-and-sort-nav-links
  [auto-links custom-links]
  (sort-by :order
           (concat auto-links custom-links)))

;; ============================================================================
;; FORMAT CUSTOM DROPDOWNS
;; ============================================================================

(defn ^:private format-custom-dropdown-item
  [item]
  (parse-custom-menu-item item))

(defn ^:private format-custom-dropdown-items
  [items]
  (map format-custom-dropdown-item items))

(defn ^:private format-custom-dropdown
  [dropdown]
  (update dropdown :items
          format-custom-dropdown-items))

(defn ^:private format-custom-dropdowns
  [dropdowns-map]
  (into {}
        (map (fn [[k v]]
               [k (format-custom-dropdown v)])
             dropdowns-map)))

;; ============================================================================
;; PARSE META ARGS
;; ============================================================================

(defn ^:private parse-meta-args
  "Parse optional rights/icon metadata from a menu vector."
  [[href label & rest]]

  (let [[rights icon] rest
        [rights icon]

        (if (and rights
                 (string? rights)
                 (str/starts-with? rights "bi "))
          [nil rights]
          [rights icon])]

    {:href href
     :label label
     :rights (when rights (vector rights))
     :icon (when (string? icon) icon)
     :order 999}))

;; ============================================================================
;; MAP -> VECTOR
;; ============================================================================

(defn ^:private map->vec
  [{:keys [href label rights icon]}]

  (cond
    (and (seq rights) icon)
    [href label (first rights) icon]

    icon
    [href label icon]

    (seq rights)
    [href label (first rights)]

    :else
    [href label]))

;; ============================================================================
;; MAIN MENU CONFIG
;; ============================================================================

(defn get-menu-config
  "Returns the complete menu configuration with custom overrides"
  []

  (let [auto-config (auto-menu/get-menu-config)

        ;; Custom nav links
        formatted-custom-nav-links
        (format-custom-nav-links custom-nav-links)

        ;; Auto nav links
        auto-nav-links-as-maps
        (map parse-meta-args
             (:nav-links auto-config))

        ;; Merge nav links
        sorted-nav-links
        (combine-and-sort-nav-links
         auto-nav-links-as-maps
         formatted-custom-nav-links)

        ;; Custom dropdowns
        formatted-custom-dropdowns
        (format-custom-dropdowns custom-dropdowns)

        ;; Auto dropdown items
        auto-dropdown-items-as-maps
        (fn [dropdown-config]
          (update dropdown-config
                  :items
                  (fn [items]
                    (map parse-meta-args items))))

        ;; Auto dropdowns
        auto-dropdowns-as-maps
        (into {}
              (map
               (fn [[category-key dropdown-config]]
                 [category-key
                  (auto-dropdown-items-as-maps
                   dropdown-config)])
               (:dropdowns auto-config)))

        ;; Merge dropdowns
        combined-dropdowns
        (merge auto-dropdowns-as-maps
               formatted-custom-dropdowns)

        ;; Sort dropdowns
        sorted-dropdowns
        (->> combined-dropdowns
             (sort-by
              (fn [[_ cfg]]
                (or (:order cfg) 999)))

             (map
              (fn [[category dropdown-config]]
                [category
                 (update dropdown-config
                         :items
                         (fn [items]
                           (map map->vec
                                (sort-by :order items))))])))]

    {:nav-links (map map->vec sorted-nav-links)
     :dropdowns sorted-dropdowns}))

;; ============================================================================
;; DEVELOPMENT
;; ============================================================================

(comment

  ;; Test menu generation
  (pp/pprint (get-menu-config))

  ;; Force menu refresh
  (auto-menu/refresh-menu!))
