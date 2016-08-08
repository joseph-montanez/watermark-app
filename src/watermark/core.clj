(ns watermark.core
  "This is an app that takes images from a folder and applies a watermark"
  (:require [clojure.java.io :as io])
  (:require [clojure.java.shell :as shell])
  (:require [clojure.string :as string])
  (:gen-class))

; GLOBALS
(def base-dir
  "The location of the watermark and unprocessed images"
  "resources")

(def watermark-file
  "The watermark image itself"
  (io/file (str base-dir "/watermark.png")))

(def unprocessed-folder
  "The location of the unprocessed images to be watermarked"
  (str base-dir "/unprocessed"))

; FUNCTIONS
(defn parse-dimensions
  "Parse '123x2345' type of strings into a pair of ints"
  [point]
  (map read-string (string/split point #"x")))

(defn filename-from-path
  "Get the file name out of a path"
  [path]
  (last (string/split path #"/")))
(defn file-ext
  "Get the extension of a file"
  [file]
  (string/lower-case (last (string/split (.getName file) #"\."))))

(defn is-img
  "Check if the extension of the file matches that of an image"
  [ext]
  (not= -1 (.indexOf ["jpg" "jpeg" "png" "gif"] ext)))

(defn list-files-from-folder
  "List all files recursively in a folder"
  [folder]
  (file-seq (io/file folder)))


(defn calculate-watermark
  "Calculates the position and size of the watermark in relation to the image"
  [image watermark]
  (let [image-path (get image :path)
        x          (get image :width)
        y          (get image :height)
        width      (/ x 2)
        height     (/ y 2)
        left       (- x (/ width 1.5))
        top        -75]
    {:watermark watermark,
     :image image-path,
     :width width,
     :height height,
     :left left,
     :top top}))

(defn identify-width-and-height
  "Get the width and height of an image"
  [file-path]
  (let [params ["identify" "-format" "%[fx:w]x%[fx:h]" file-path]]
    (parse-dimensions (:out (apply shell/sh params)))))

(defn composite-watermark
  "Put a watermark on the bottom right of the image"
  [watermark-path image-path width height left top]
  (let [geometry-format (cons "%dx%d+%d+%d" (map int [width height left top]))
        geometry-params (apply format geometry-format)
        filename        (filename-from-path image-path)
        output-path (str "resources/processed/" filename)]
    (prn (string/join " " ["composite"
                    "-compose" "multiply"
                    "-gravity" "SouthWest"
                    "-geometry" "+5+5"
                    watermark-path
                    image-path
                    "-geometry" geometry-params
                    output-path]))
    (:out (shell/sh "composite"
                    "-compose" "multiply"
                    "-gravity" "SouthWest"
                    "-geometry" "+5+5"
                    watermark-path
                    image-path
                    "-geometry" geometry-params
                    output-path))))

(defn get-image-info [filename]
  (let [dimensions (identify-width-and-height filename)]
    {:path filename,
     :width (first dimensions),
     :height (second dimensions)}))

(defn list-images-from-folder
  "Returns a list of files and dimensions ((filepath width height) &)"
  [folder]
  (->> (list-files-from-folder folder)
       ; Make sure all items are files and the extension are of images
       (filter #(and (.isFile %) (is-img (file-ext %))))
       ; Change output to (filepath:string width:int height:int)
       (map #(get-image-info (.getAbsolutePath %)))))


; ENTRY
(defn -main
  "Main application"
  [& args]
  (let [watermark-path (.getAbsolutePath watermark-file)
        watermark-x-y (identify-width-and-height watermark-path)
        image-list (list-images-from-folder unprocessed-folder)
        images (map #(calculate-watermark % watermark-path) image-list)]
    (doall (map #(apply composite-watermark (vals %)) images)))
  (shutdown-agents))


