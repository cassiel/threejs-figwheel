(ns ^:figwheel-always threejs-figwheel.core
    (:require [figwheel.client :as fw]
              three
              stats))

(enable-console-print!)

;; There's a little debugging counter in here. This state variable
;; tracks a couple of components: the three.js renderer and the stats object.
(defonce APP-STATE (atom {:c 0}))

;; The startup and teardown routines need to basically be idempotent; in
;; particular, when a page loads we might see two startup calls.

(defn startup-stats
  "Install the stats into the page, and into the app state."
  []
  (when-not (:stats @APP-STATE)
    (let [stats (js/Stats.)]
      (set! (.. stats -domElement -style -position) "absolute")
      (set! (.. stats -domElement -style -left) "0px")
      (set! (.. stats -domElement -style -top) "0px")
      (.appendChild (.-body js/document) (.-domElement stats))
      (swap! APP-STATE assoc :stats stats))))

(defn teardown-stats
  "Remove the stats from the page and the app state."
  []
  (when-let [stats (:stats @APP-STATE)]
    (.removeChild (.-body js/document) (.-domElement stats))
    (swap! APP-STATE dissoc :stats)))

(defn startup-app
  "Swap into the app state the renderer, and a function to stop the current animation loop."
  []
  (when-not (:renderer @APP-STATE)
    (let [scene (js/THREE.Scene.)
          camera (js/THREE.PerspectiveCamera. 75
                                              (/ (.-innerWidth js/window) (.-innerHeight js/window))
                                              0.1
                                              1000)
          renderer (js/THREE.WebGLRenderer.)
          geometry (js/THREE.BoxGeometry. 1 1 1)
          material (js/THREE.MeshBasicMaterial. (clj->js {:color 0x00FF00
                                                          :wireframe false}))
          cube (js/THREE.Mesh. geometry material)
          ;; An "alive" flag to let us kill the animation refresh when we tear down:
          RUNNING (atom true)]
      (set! (.-xxid renderer)
            (:c (swap! APP-STATE update :c inc)))
      (.setSize renderer (.-innerWidth js/window) (.-innerHeight js/window))
      (.log js/console "Adding: " (.-xxid renderer))
      (.appendChild (.-body js/document) (.-domElement renderer))
      (.add scene cube)
      (set! (.. camera -position -z) 3)

      (letfn [(animate []
                (when @RUNNING (js/requestAnimationFrame animate))
                (set! (.. cube -rotation -x)
                      (+ 0.01 (.. cube -rotation -x)))
                (set! (.. cube -rotation -y)
                      (+ 0.01 (.. cube -rotation -y)))
                (.render renderer scene camera)
                (when-let [stats (:stats @APP-STATE)] (.update stats)))]
        (animate)
        (swap! APP-STATE #(assoc %
                                 :renderer renderer
                                 :stopper (fn [] (reset! RUNNING false))))))))

(defn teardown-app
  "Stop animation cycle, tear out renderer."
  []
  (when-let [stopper (:stopper @APP-STATE)] (stopper))
  (when-let [renderer (:renderer @APP-STATE)]
    (.log js/console "Removing: " (.-xxid renderer))
    (.removeChild (.-body js/document) (.-domElement renderer)))
  (swap! APP-STATE dissoc :stopper :renderer))

;; This confuses me: since we so a ^:figwheel-always, I don't see what purpose
;; fw/start serves here; perhaps these machinery should be decoupled a bit.

(fw/start {
  :on-jsload (fn []
               (teardown-app)
               (teardown-stats)

               (startup-app)
               (startup-stats))})

(startup-app)
(startup-stats)
