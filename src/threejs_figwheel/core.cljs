(ns ^:figwheel-always threejs-figwheel.core
    (:require))

(enable-console-print!)

(println "Edits to this text should show up in your developer console.")

;; define your app data so that it doesn't get over-written on reload

(defonce app-state (atom {:text "Hello world!"}))

(def scene (js/THREE.Scene.))

(def camera (js/THREE.PerspectiveCamera. 75
                                         (/ (.-innerWidth js/window) (.-innerHeight js/window))
                                         0.1
                                         1000))
(def renderer (js/THREE.WebGLRenderer.))
(def geometry (js/THREE.BoxGeometry. 1 1 1))
(def material (js/THREE.MeshBasicMaterial. (clj->js {:color 0x00FF00
                                                     :wireframe false})))
(def cube (js/THREE.Mesh. geometry material))
(def stats (js/Stats.))

(defn render []
  (.render renderer scene camera))

(defn animate []
  (js/requestAnimationFrame animate)
  (render)
  (.update stats))

(defn startup []
  (set! (.. stats -domElement -style -position) "absolute")
  (set! (.. stats -domElement -style -left) "0px")
  (set! (.. stats -domElement -style -top) "0px")
  (.appendChild (.-body js/document) (.-domElement stats))

  (.setSize renderer (.-innerWidth js/window) (.-innerHeight js/window))
  (.appendChild (.-body js/document) (.-domElement renderer))
  (.add scene cube)
  (set! (.. camera -position -z) 5)
  (animate))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)

(startup)
