(ns play.core)

(enable-console-print!)
(println "Here we go, again!")

;;
;; =====================================================================================
;; Make ctx:
;; =====================================================================================
;;

(defn make-obj-old []
  (js/THREE.Line. (doto (js/THREE.Geometry.)
                    (-> .-vertices (.push (js/THREE.Vector3. -10 0 0)))
                    (-> .-vertices (.push (js/THREE.Vector3. 0 10 0)))
                    (-> .-vertices (.push (js/THREE.Vector3. 10 0 0))))
                  (js/THREE.LineBasicMaterial. #js {:color 0x0000ff})))

(def segments 60)

(defn make-verts []
  (let [d (-> Math/PI (* 2.0) (/ segments))]
    (->> (range segments)
         (reduce (fn [verts i]
                   (doto verts
                     (.push (Math/cos (* i d)))
                     (.push (Math/sin (* i d)))
                     (.push 0)))
                 (js/Array.))
         (js/Float32Array.))))

(defn make-geom []
  (doto (js/THREE.BufferGeometry.)
    (.addAttribute "position" (js/THREE.BufferAttribute. (make-verts) 3))))

(def geom (make-geom))

(defn make-material []
  (doto (js/THREE.LineBasicMaterial. #js {:color 0xff00ff})
    (-> .-transparent (set! true))
    (-> .-opacity (set! 0.9))))

(defn make-element [x y s]
  (doto (js/THREE.LineLoop. geom (make-material))
    (-> .-position (.copy (js/THREE.Vector3. x y 0)))
    (-> .-scale (.copy (js/THREE.Vector3. s s 1)))
    (-> .-visible (set! false))
    (-> .-age (set! 0.0))))

(def elements (let [group (js/THREE.Group.)]
                (dotimes [_ 1000]
                    (.add group (make-element 0 0 100)))
                ;(.add group (make-element 1 1 0.1))
                ;(.add group (make-element 1 -1 0.1))
                ;(.add group (make-element -1 -1 0.1))
                ;(.add group (make-element -1 1 0.1))
                ;(.add group (make-element 0 0 1))
                group))

(defn resize-canvas [{:keys [canvas] :as ctx}]
  (let [width (.-clientWidth canvas)
        height (.-clientHeight canvas)
        aspect (/ width height)]
    (doto canvas
      (-> .-width (set! width))
      (-> .-height (set! height)))
    (assoc ctx :width width
               :height height
               :aspect aspect)))

(defn resize-renderer [{:keys [renderer width height] :as ctx}]
  (doto renderer
    (-> (.setViewport 0 0 width height)))
  ctx)

(defn resize-camera [{:keys [camera width height aspect] :as ctx}]
  (doto camera
    (-> .-left (set! 0))
    (-> .-right (set! width))
    (-> .-top (set! 0))
    (-> .-bottom (set! height))
    (.updateProjectionMatrix))
  ctx)

(defn resize [ctx]
  (-> ctx
      (resize-canvas)
      (resize-renderer)
      (resize-camera)))

(defn make-scene []
  (doto (js/THREE.Scene.)
    (.add elements)))

(defn make-ctx []
  (let [canvas (js/document.getElementById "app")]
    (-> {:canvas canvas}
        (resize-canvas)
        (assoc :renderer (js/THREE.WebGLRenderer. #js {:canvas canvas}))
        (resize-renderer)
        (assoc :camera (doto (js/THREE.OrthographicCamera. -1 +1 +1 -1 1 20)
                         (-> .-position .-z (set! 1))
                         (.lookAt (js/THREE.Vector3. 0 0 0))))
        (resize-camera)
        (assoc :scene (make-scene)))))

(def ctx (atom (make-ctx)))

;;
;; =====================================================================================
;; Element manipulation:
;; =====================================================================================
;;

(defn retire-element [element]
  (-> element .-visible (set! false)))

(defn set-element-age [element age]
  (let [s (* age 100)]
    (doto element
      (-> .-age (set! age))
      (-> .-material .-opacity (set! (- 1.0 age)))
      (-> .-scale (doto
                    (-> .-x (set! s))
                    (-> .-y (set! s)))))))

(defn age-element [element]
  (let [age (.-age element)]
    (if (>= age 1.0)
      (retire-element element)
      (set-element-age element (+ age 0.002)))))

(defn revive-element [element x y]
  (doto element
    (set-element-age 0.0)
    (-> .-visible (set! true))
    (-> .-position (.copy (js/THREE.Vector3. x y 0)))))

(defn revive-one-element [x y]
  (when-let [element (->> elements
                          .-children
                          (remove (fn [element] (-> element .-visible)))
                          (first))]
    (revive-element element x y)))

;;
;; =====================================================================================
;; Rendering:
;; =====================================================================================
;;

(defn render [{:keys [renderer scene camera]}]
  (doseq [element (-> elements .-children)]
    (when (-> element .-visible)
      (age-element element)))
  (.render renderer scene camera))

(defn animate [_]
  (js/requestAnimationFrame animate)
  (render @ctx))

(defonce game (animate 0))

;;
;; =====================================================================================
;; Events:
;; =====================================================================================
;;

(def PI Math/PI)
(def PI2 (* Math/PI 2.0))

(defn on-mouse-move [e]
  (.preventDefault e)
  (revive-one-element (-> e .-offsetX) (-> e .-offsetY)))

(defn on-touch-move [e]
  (.preventDefault e)
  (let [touches (-> e .-touches)]
    (dotimes [n (.-length touches)]
      (let [touch (.item touches n)]
        (revive-one-element (->> touch .-clientX) (->> touch .-clientY))))))

(defn on-resize [_]
  (swap! ctx resize))

(defn init-event-listeners []
  (doto js/window
    (.addEventListener "resize" (fn [e] (on-resize e))))
  (doto (:canvas @ctx)
    (.addEventListener "mousemove" (fn [e] (on-mouse-move e)))
    ;(.addEventListener "touchstart" (fn [e] (on-touch-move e)))
    (.addEventListener "touchmove" (fn [e] (on-touch-move e)))))

(defonce _ (init-event-listeners))

(println "Page ready")
