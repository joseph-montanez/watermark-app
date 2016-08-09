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
  (string/lower-case (last (string/split file #"\."))))

(defn is-img
  "Check if the extension of the file matches that of an image"
  [ext]
  (not= -1 (.indexOf ["jpg" "jpeg" "png" "gif"] ext)))

(defn list-files-from-folder
  "List all files recursively in a folder"
  [folder]
  (file-seq (io/file folder)))

;
(defn calculate-watermark
  "Calculates the position and size of the watermark in relation to the image"
  [image watermark watermark-ratio]
  (let [image-path   (get image :path)
        image-width  (get image :width)
        image-height (get image :height)
        width        (* image-width watermark-ratio)
        height       (* image-height watermark-ratio)
        left         (- image-width (/ width 1.5))
        top          (- image-height (- image-height height))]
    {:watermark (get watermark :path)
     :image image-path
     :width width
     :height height
     :left left
     :top top}))

(defn identify-width-and-height
  "Get the width and height of an image"
  [file-path]
  (let [params ["identify" "-format" "%[fx:w]x%[fx:h]" file-path]]
    (parse-dimensions (:out (apply shell/sh params)))))

(defn composite-watermark
  "Put a watermark on the bottom right of the image"
  [watermark-path image-path width height left top & [outfile]]
  (prn watermark-path image-path width height left top outfile)
  (let [geometry-format (cons "%dx%d+%d+%d" (map int [width height left top]))
        geometry-params (apply format geometry-format)
        filename        (if outfile outfile (filename-from-path image-path))
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
  "Get the details of an image like width and height"
  (let [dimensions (identify-width-and-height filename)]
    {:path filename
     :width (first dimensions)
     :height (second dimensions)}))

(defn list-images-from-folder
  "Returns a list of files and dimensions ((filepath width height) &)"
  [folder]
  (->> (list-files-from-folder folder)
       ; Make sure all items are files and the extension are of images
       (filter #(and (.isFile %) (is-img (file-ext (.getName %)))))
       ; Change output to (filepath:string width:int height:int)
       (map #(get-image-info (.getAbsolutePath %)))))

; ENTRY
(defn -main
  "Main application"
  [& args]
  (let [watermark-path (.getAbsolutePath watermark-file)
        watermark-info (get-image-info watermark-path)
        image-list (list-images-from-folder unprocessed-folder)
        images (map #(calculate-watermark % watermark-info 1/2) image-list)]
    (doall (pmap #(apply composite-watermark (vals %)) images)))
  (shutdown-agents))

