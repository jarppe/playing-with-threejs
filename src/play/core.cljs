(ns play.core)

(enable-console-print!)

;;
;; =====================================================================================
;; Make ctx:
;; =====================================================================================
;;

(def segments 10)

(defn make-verts []
  (let [d (-> Math/PI (* 2.0) (/ segments))]
    (->> (range segments)
         (reduce (fn [verts i]
                   (let [r (if (odd? i) 1.0 0.5)]
                     (doto verts
                       (.push (-> i (* d) (Math/cos) (* r)))
                       (.push (-> i (* d) (Math/sin) (* r)))
                       (.push 0))))
                 (js/Array.))
         (js/Float32Array.))))

(def geom (doto (js/THREE.BufferGeometry.)
            (.addAttribute "position" (js/THREE.BufferAttribute. (make-verts) 3))))

(defn make-material []
  (doto (js/THREE.LineBasicMaterial. #js {:color 0x00ff00})
    (-> .-transparent (set! true))
    (-> .-opacity (set! 0.0))))

(defn make-element [x y s]
  (doto (js/THREE.LineLoop. geom (make-material))
    (-> .-position (.copy (js/THREE.Vector3. x y 0)))
    (-> .-scale (.copy (js/THREE.Vector3. s s 1)))
    (-> .-material .-opacity (set! 1.0))
    (-> .-visible (set! false))
    (-> .-age (set! 0.0))))

(defn resize-canvas [{:keys [canvas] :as ctx}]
  (let [width (.-clientWidth canvas)
        height (.-clientHeight canvas)
        aspect (/ width height)]
    (js/console.log "width:" width "height:" height)
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

(defn resize-camera [{:keys [camera width height] :as ctx}]
  (doto camera
    (-> .-top (set! 0))
    (-> .-right (set! width))
    (-> .-bottom (set! height))
    (-> .-left (set! 0))
    (.updateProjectionMatrix))
  ctx)

(defn resize [ctx]
  (-> ctx
      (resize-canvas)
      (resize-renderer)
      (resize-camera)))

(defn make-ctx []
  (let [canvas (js/document.getElementById "app")
        elements (js/THREE.Group.)]
    (dotimes [_ 1000]
      (.add elements (make-element 0 0 100)))
    (-> {:canvas canvas
         :elements elements}
        (resize-canvas)
        (assoc :renderer (js/THREE.WebGLRenderer. #js {:canvas canvas}))
        (resize-renderer)
        (assoc :camera (doto (js/THREE.OrthographicCamera. -1 +1 +1 -1 1 20)
                         (-> .-position .-z (set! 1))
                         (.lookAt (js/THREE.Vector3. 0 0 0))))
        (resize-camera)
        (assoc :scene (doto (js/THREE.Scene.)
                        (.add elements))))))

(defonce ctx (atom (make-ctx)))

;;
;; =====================================================================================
;; Element manipulation:
;; =====================================================================================
;;

(defn retire-element [element]
  (doto element
    (-> .-visible (set! false))))

(defn set-element-age [element age]
  (let [s (* age 100)
        y-speed (-> element .-y-speed (+ 0.01))
        y (-> element .-position .-y (+ y-speed))
        r-speed (-> element .-r-speed)
        r (-> element .-rotation .-z (+ r-speed))]
    (doto element
      (-> .-age (set! age))
      (-> .-material .-opacity (set! (- 1.0 age)))
      (-> .-rotation .-z (set! r))
      (-> .-r (set! r))
      (-> .-scale (doto
                    (-> .-x (set! s))
                    (-> .-y (set! s))))
      (-> .-position .-y (set! y))
      (-> .-y-speed (set! y-speed)))))

(defn age-element [element]
  (let [age (.-age element)]
    (if (>= age 1.0)
      (retire-element element)
      (set-element-age element (+ age 0.002)))))

(defn revive-element [element x y]
  (doto element
    (set-element-age 0.0)
    (-> .-visible (set! true))
    (-> .-position (.copy (js/THREE.Vector3. x y 0)))
    (-> .-rotation .-z (set! 0.0))
    (-> .-r-speed (set! (-> (rand) (- 0.5) (* 0.1))))
    (-> .-y-speed (set! 0.0))))

(defn revive-one-element [ctx x y]
  (when-let [element (->> ctx
                          :elements
                          .-children
                          (remove (fn [element] (-> element .-visible)))
                          (first))]
    (revive-element element x y)))

;;
;; =====================================================================================
;; Rendering:
;; =====================================================================================
;;

(defn render [{:keys [renderer scene camera elements]}]
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

(defn on-mouse-move [e]
  (.preventDefault e)
  (when (-> e .-buttons zero? not)
    (revive-one-element @ctx (-> e .-offsetX) (-> e .-offsetY))))

(defn on-touch-move [e]
  (.preventDefault e)
  (let [touches (-> e .-touches)]
    (dotimes [n (.-length touches)]
      (let [touch (.item touches n)]
        (revive-one-element @ctx (-> touch .-clientX) (-> touch .-clientY))))))

(defn on-resize [_]
  (swap! ctx resize))

(defn init-event-listeners []
  (doto js/window
    (.addEventListener "resize" (fn [e] (on-resize e))))
  (doto (:canvas @ctx)
    (.addEventListener "mousemove" (fn [e] (on-mouse-move e)))
    (.addEventListener "touchmove" (fn [e] (on-touch-move e)))))

(defonce _ (init-event-listeners))

(println "Page ready")
