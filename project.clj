;; TODO: When bumping the version number here, be sure to bump it also in
;; src/leiningen/html5_docs/core.clj (var lein-html5-docs-version).
(defproject lein-html5-docs "2.0.1"
  :description "A HTML5 API docs generator plugin for Leiningen.
Versions below 1.2.0 are for Leiningen 1, starting with 1.2.0 Leiningen 2 is
required."
  :dependencies [;;[org.clojure/clojure "1.5.1"]
                 [hiccup "1.0.3"]]
  :jar-exclusions [#"(?:^|/).git/"]
  :warn-on-reflection true
  :url "https://github.com/tsdh/lein-html5-docs")
