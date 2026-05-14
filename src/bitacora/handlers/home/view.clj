(ns bitacora.handlers.home.view
  (:require
   [bitacora.models.form :refer [login-form password-form]]))

(defn home-view
  []
  (list
   [:div.container.mt-5
    [:div.text-center
     [:h1.text-info "Bitacora Fleet"]
     [:p.text-muted "Sistema de Gestion Vehicular"]
     [:p.text-muted "Serviendo a la comunidad desde 1998"]
     [:p "Free Code City, Mexicali Baja California Mexico, 21050"]
     [:p "Phone: (686) 123-4567 | Email: marcopescador@hotmail.com"]]]))

(defn main-view
  "This creates the login form and we are passing the title from the controller"
  [title]
  (let [href "/home/login"]
    (login-form title href)))

(defn change-password-view
  [title]
  (password-form title))
