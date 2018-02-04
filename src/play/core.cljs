(ns play.core
  (:require [cljsjs.three]))

(enable-console-print!)
(println "Here we go, again")

(def width (.-innerWidth js/window))
(def height (.-innerHeight js/window))
(def scene (js/THREE.Scene.))
(def camera (js/THREE.PerspectiveCamera. 75 (/  width height) 0.1 1000))
(def renderer (js/THREE.WebGLRenderer. #js {:canvas (js/document.getElementById "app")}))

(.setSize renderer width height)

(def geometry (js/THREE.BoxGeometry. 1 1 1))
(def material (js/THREE.MeshBasicMaterial. #js {:color 0x00ff00}))
(def cube (js/THREE.Mesh. geometry material))

(.add scene cube)

(-> camera .-position .-z (set! 2))

(defn render [_]
  (let [rotation (.-rotation cube)]
    (-> rotation .-x (set! (+ (.-x rotation) 0.001)))
    (-> rotation .-y (set! (+ (.-y rotation) 0.002))))
  (.render renderer scene camera))

(defn animate [ts]
  (js/requestAnimationFrame animate)
  (render ts))

(defonce game (animate 0))
