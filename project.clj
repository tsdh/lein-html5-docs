;; IMPORTANT: When bumping the version number here, be sure to bump it also in
;; src/leiningen/html5_docs.clj!
(defproject lein-html5-docs "1.2.2"
  :description "A HTML5 API docs generator plugin for Leiningen.  Versions
below 1.2.0 are for Leiningen 1, starting with 1.2.0 Leiningen 2 is required."
  :dependencies [[hiccup "1.0.0"]]
  :jar-exclusions [#"(?:^|/).git/"]
  :warn-on-reflection true
  :url "https://github.com/tsdh/lein-html5-docs")