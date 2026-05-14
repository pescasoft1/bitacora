(ns bitacora.handlers.dashboard.view
  (:require [cheshire.core :as json]))

;; CARD KPI
(defn card [{:keys [title count subtitle icon color size]}]
  [:div {:class (str size " mb-4")}
   [:div {:style (str
                  "background: linear-gradient(145deg,#121821,#0B0F14);
      border-radius:16px;
      padding:20px;
      color:white;
      border-left:4px solid " (or color "#00D1FF") ";")}
    [:div {:style "display:flex; justify-content:space-between;"}
     [:div
      [:div {:style "font-size:13px; color:#8B949E;"} title]
      [:div {:style "font-size:26px; font-weight:bold;"} count]
      (when subtitle [:div {:style "font-size:11px; color:#8B949E;"} subtitle])]
     [:i {:class icon :style (str "font-size:28px; color:" color ";")}]]]])

;; CARD IMG
(defn card-img [{:keys [image size]}]
  [:div {:class (str size " mb-4")}
   [:div {:style "background:#121821; border-radius:16px; overflow:hidden;"}
    [:img {:src image :style "width:100%; height:180px; object-fit:cover;"}]]])

(defn money [n]
  (str "$" (format "%.2f" (double (or n 0)))))

(defn main [title stats gastos-mes]

  (let [litros (:total-litros stats)
        gasto (:total-gastado stats)
        costo (if (> litros 0) (/ gasto litros) 0)

        meses (->> gastos-mes (map :mes) distinct sort)
        tipos (->> gastos-mes (map :tipo) distinct)

        datasets
        (for [t tipos]
          {:label t
           :data (for [m meses]
                   (let [item (first (filter #(and (= (:mes %) m)
                                                   (= (:tipo %) t)) gastos-mes))]
                     (double (or (:total item) 0))))})

        labels-json (json/generate-string meses)
        datasets-json (json/generate-string datasets)
        data-json (json/generate-string (map #(double (:total %)) gastos-mes))

        max-item (apply max-key :total (or gastos-mes [{:mes "00" :total 0 :tipo "N/A"}]))]

    [:div {:style "background:#0B0F14; min-height:100vh; padding:30px;"}

     [:h2 {:style "color:#E6EDF3;"} "Bitácora Fleet"]
     [:span {:style "color:#8B949E;"} "Control total de tu flotilla"]

     ;; HERO
     [:div.row
      (card-img {:image "/uploads/bitacorafleet.png" :size "col-md-6"})
      (card-img {:image "/uploads/bitacorafleet2.png" :size "col-md-6"})]

     ;; KPIs
     [:div.row
      (card {:title "Cargas" :count (:total-cargas stats) :icon "bi bi-fuel-pump" :color "#00D1FF" :size "col-md-2"})
      (card {:title "Vehículos" :count (:total-vehiculos stats) :icon "bi bi-car-front" :color "#4ADE80" :size "col-md-2"})
      (card {:title "Conductores" :count (:total-conductores stats) :icon "bi bi-person" :color "#FFC857" :size "col-md-2"})
      (card {:title "Gastos" :count (money gasto) :icon "bi bi-cash" :color "#FF5C5C" :size "col-md-2"})
      (card {:title "Litros" :count (format "%.2f" litros) :icon "bi bi-droplet" :color "#00D1FF" :size "col-md-2"})
      (card {:title "Costo/L" :count (money costo) :icon "bi bi-graph-up" :color "#4ADE80" :size "col-md-2"})]

     ;; DASHBOARD PRO
     [:div.row.mt-4

      ;; GRAFICA
      [:div.col-md-8
       [:div {:style "background:#121821; padding:20px; border-radius:16px;"}

        [:select {:id "filtro-tipo"}
         [:option {:value "all"} "Todos"]
         (for [t tipos] [:option {:value t} t])]

        [:div {:style "height:320px;"}
         [:canvas {:id "chart"}]]]]

      ;; PANEL
      [:div.col-md-4
       [:div {:style "background:#121821; padding:20px; border-radius:16px; color:white;"}
        [:h5 "Insight"]
        [:p (str "Mes top: " (:mes max-item))]
        [:h3 (money (:total max-item))]
        [:p (str "Tipo: " (:tipo max-item))]]]]

     ;; MINI CARD
     [:div.row.mt-4
      [:div.col-md-4
       [:div {:style "background:#121821; padding:15px; border-radius:12px; cursor:pointer;"
              :onclick "document.getElementById('modal').style.display='flex'"}
        [:h6 "Tendencia"]
        [:canvas {:id "mini"}]]]]

     ;; MODAL
     [:div {:id "modal"
            :style "display:none; position:fixed; top:0; left:0; width:100%; height:100%; background:black; justify-content:center; align-items:center;"
            :onclick "this.style.display='none'"}
      [:canvas {:id "full"}]]

     ;; SCRIPT
     [:script
      (str "
document.addEventListener('DOMContentLoaded', function () {

  const labels = " labels-json ";
  const datasets = " datasets-json ";
  const rawData = " data-json ";

  function build(ctx,data) {
    return new Chart(ctx,{type:'bar',data:{labels:labels,datasets:data}});
  }

  let chart = build(document.getElementById('chart'), datasets);

  document.getElementById('filtro-tipo').addEventListener('change',function(){
    chart.destroy();
    chart = build(document.getElementById('chart'),
      this.value==='all'?datasets:datasets.filter(d=>d.label===this.value));
  });

  new Chart(document.getElementById('mini'),{
    type:'line',
    data:{labels:labels,datasets:[{data:rawData,borderColor:'#00D1FF'}]},
    options:{plugins:{legend:{display:false}},scales:{x:{display:false},y:{display:false}}}
  });

  new Chart(document.getElementById('full'),{
    type:'line',
    data:{labels:labels,datasets:[{data:rawData,borderColor:'#00D1FF'}]}
  });

});
")]]))