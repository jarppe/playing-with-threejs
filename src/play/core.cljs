(ns play.core)

(enable-console-print!)
(println "Here we go, again!")

(defn render [{:keys [cube renderer scene camera]}]
  (let [rotation (.-rotation cube)]
    (-> rotation .-x (set! (+ (.-x rotation) 0.02)))
    (-> rotation .-y (set! (+ (.-y rotation) 0.01))))
  (.render renderer scene camera))

(defn init []
  (let [width (.-innerWidth js/window)
        height (.-innerHeight js/window)
        scene (js/THREE.Scene.)
        camera (js/THREE.PerspectiveCamera. 75 (/ width height) 0.1 1000)
        renderer (js/THREE.WebGLRenderer. #js {:canvas (js/document.getElementById "app")})
        geometry (js/THREE.BoxGeometry. 1 1 1)
        material (js/THREE.MeshBasicMaterial. #js {:color 0x00ff00})
        cube (js/THREE.Mesh. geometry material)]
    (.setSize renderer width height)
    (.add scene cube)
    (-> camera .-position .-z (set! 2))
    {:scene scene
     :camera camera
     :renderer renderer
     :cube cube}))

(defonce ctx (init))

(defn animate [_]
  (js/requestAnimationFrame animate)
  (render ctx))

(defonce game (animate 0))
