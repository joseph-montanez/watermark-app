(ns webapp.core
  (:import (java.io ByteArrayOutputStream
                    ByteArrayInputStream))

  (:require [compojure.core :refer :all]
            [clojure.java.io :as io]
            [watermark.core :refer [get-image-info
                                    file-ext
                                    watermark-file
                                    calculate-watermark
                                    composite-watermark]]
            [compojure.route :as route]
            [ring.util.response :as r]
            [hiccup.core :as hiccup]
            [org.tobereplaced.nio.file :refer [read-all-bytes]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.util.anti-forgery :refer [anti-forgery-field]]))

(defn read-file [file] (ByteArrayInputStream. (read-all-bytes (io/file file))))

; Bootstrap helpers
(defn form-file [label name & [id]]
  (let
    [override-id (if id id name)]
    [:div.form-group
     [:label {:for override-id} label]
     [:input.form-control {:type "file", :name name, :id override-id}]]))




(defroutes app-routes

  (GET "/" []

    (hiccup/html
      [:html
       [:head

        ; Stylesheets
        [:link {:rel "stylesheet", :href "//maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css", :crossorigin "anonymous"}]
        [:link {:rel "stylesheet", :href "//maxcdn.bootstrapcdn.com/bootswatch/3.3.7/cerulean/bootstrap.min.css", :crossorigin "anonymous"}]
        [:link {:rel "stylesheet", :href "//maxcdn.bootstrapcdn.com/font-awesome/4.6.3/css/font-awesome.min.css", :crossorigin "anonymous"}]]

       [:body

        ; Header
        [:div.navbar.navbar-default.navbar-fixed-top
         [:div.container
          [:div.navbar-header
           [:a.navbar-brand {:href "/"} "Shabb"]
           [:button.navbar-tottle {:type "button" :data-toggle "collapse" :data-target "#navbar-main"}
            [:span.icon-bar]
            [:span.icon-bar]
            [:span.icon-bar]]]
          [:div#navbar-main.navbar-collapse.collapse]]]

        ; Body
        [:div.container
         [:div.bs-docs-section.clearfix

          [:div#banner.page-header
           [:div.row
            [:div.col-lg-8.col-md-7.col-sm-6
             [:h1 "Watermarker"]]]]

          [:form.inline-form {:method "Post", :enctype "multipart/form-data"}

           (anti-forgery-field)

           (form-file "Image" "image")
           (form-file "Watermark" "watermark")

           [:button.btn.btn-default {:type "submit"} "Upload"]]]]

        ; Scripts
        [:script {:src "//maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js", :crossorigin "anonymous"}]]]))



  (POST "/" [:as r]
    (let [tmpfileimg   (get-in r [:params :image :tempfile] false)
          tmpfilemrk   (get-in r [:params :watermark :tempfile] false)
          ; Get the full path of the tmp file
          tmpnameimg   (.getAbsolutePath tmpfileimg)
          tmpnamemrk   (.getAbsolutePath tmpfilemrk)
          ; Get the filename of the uploaded image
          imagename (get-in r [:params :image :filename] false)
          ; Generate a random output filename
          outfile   (str (java.util.UUID/randomUUID) "." (file-ext imagename))
          ; Get the image details of the file
          image     (get-image-info tmpnameimg)
          ; Get the watermark path
          mark      tmpnamemrk
          ; Get the composition details of the image and watermark
          composite (calculate-watermark image mark)
          ; Convert the compsite hashmap into paramaters and append the outfile
          composite-params (concat (vals composite) [outfile])
          ; Apply the watermark to the image
          composed  (apply composite-watermark composite-params)
          ; Read out the file into a bytes array
          img-bytes (read-file (str "resources/processed/" outfile))]
      ; Read out the file to the browser
      (-> (r/response img-bytes)
          (r/header "Content-Type" "image/png"))))

  (GET "/test" [:as req]
       (-> (r/response "Testing")
           (r/header "Content-Type" "text/css")))

  (route/resources "/")
  (route/not-found "<h1>Page not found</h1>"))

(def app
  (wrap-defaults app-routes site-defaults))
