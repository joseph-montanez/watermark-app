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
        (prn r)
        (let [file      (get-in r [:params :image :tempfile] false)
              filename  (.getAbsolutePath file)
              image     (watermark/get-image-info filename)
              mark      (.getAbsolutePath watermark/watermark-file)
              tmp       (prn [mark image])
              composite (watermark/calculate-watermark image mark)
              tmp2      (prn composite)
              composed  (apply watermark/composite-watermark (vals composite))
              ]
        (str "Uploaded: " (get-in r [:params :image :filename] "No image uploaded"))))
  (route/not-found "<h1>Page not found</h1>"))


(def app
  (wrap-defaults app-routes site-defaults))
