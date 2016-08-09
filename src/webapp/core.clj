(ns webapp.core
  (:require [compojure.core :refer :all]
            [watermark.core :as watermark]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.util.anti-forgery :refer [anti-forgery-field]]))

(defroutes app-routes
  (GET "/" []
    (str "<h1>Hello World</h1>"
         "<form method=POST enctype='multipart/form-data'>"
         (anti-forgery-field)
         "<input type=file name=image>"
         "<input type=submit value=Upload>"
         "</form>"))
  (POST "/" [:as r]
    (let [tmpfile   (get-in r [:params :image :tempfile] false)
          ; Get the full path of the tmp file
          tmpname   (.getAbsolutePath tmpfile)
          ; Get the filename of the uploaded image
          imagename (get-in r [:params :image :filename] false)
          ; Generate a random output filename
          outfile   (str (java.util.UUID/randomUUID) "." (watermark/file-ext imagename))
          ; Get the image details of the file
          image     (watermark/get-image-info tmpname)
          ; Get the watermark path
          mark      (.getAbsolutePath watermark/watermark-file)
          ; Get the composition details of the image and watermark
          composite (watermark/calculate-watermark image mark)
          ; Convert the compsite hashmap into paramaters and append the outfile
          composite-params (concat (vals composite) [outfile])
          ; Apply the watermark to the image
          composed  (apply watermark/composite-watermark composite-params)]
      (str "Uploaded: " (get-in r [:params :image :filename] false))))
  (route/not-found "<h1>Page not found</h1>"))

(def app
  (wrap-defaults app-routes site-defaults))
