;; TODO: When bumping the version number here, be sure to bump it also in
;; src/leiningen/html5_docs/core.clj (var lein-html5-docs-version).
(defproject lein-html5-docs "3.0.2"
  :description "A HTML5 API docs generator plugin for Leiningen.  Also
generates docsets for use with Dash (http://kapeli.com/dash) and
Zeal (http://zealdocs.org/)."
  :dependencies [[hiccup "1.0.5"]
                 [org.clojure/java.jdbc "0.4.2"]
                 [org.xerial/sqlite-jdbc "3.8.11.2"]]
  :jar-exclusions [#"(?:^|/).git/"]
  :global-vars {*warn-on-reflection* true}
  :eval-in-leiningen true
  :url "https://github.com/tsdh/lein-html5-docs")
