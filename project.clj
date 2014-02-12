;; TODO: When bumping the version number here, be sure to bump it also in
;; src/leiningen/html5_docs/core.clj (var lein-html5-docs-version).
(defproject lein-html5-docs "2.1.0"
  :description "A HTML5 API docs generator plugin for Leiningen.
Versions below 1.2.0 are for Leiningen 1, starting with 1.2.0 Leiningen 2 is
required."
  :dependencies [;;[org.clojure/clojure "1.5.1"]
                 [hiccup "1.0.5"]]
  :jar-exclusions [#"(?:^|/).git/"]
  :global-vars {*warn-on-reflection* true}
  :eval-in-leiningen true
  :url "https://github.com/tsdh/lein-html5-docs")
