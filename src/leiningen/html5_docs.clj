(ns leiningen.html5-docs
  "Generate HTML5 API docs."
  (:use [leiningen.core.eval :only [eval-in-project]]))

(defn html5-docs [project]
  (eval-in-project
   project
   `(leiningen.html5-docs.core/html5-docs '~project)
   '(require 'leiningen.html5-docs.core)))

