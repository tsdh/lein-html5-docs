(ns leiningen.html5-docs
  "Generate HTML5 API docs."
  (:use [leiningen.core.eval :only [eval-in-project]]))

(defn html5-docs [project]
  (eval-in-project
   (update-in project [:dependencies] conj [lein-html5-docs "1.2.0"])
   `(leiningen.html5-docs.core/html5-docs '~project)
   '(require 'leiningen.html5-docs.core)))

