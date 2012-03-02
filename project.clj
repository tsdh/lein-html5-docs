(defproject lein-html5-docs "1.0.0"
  :description "A HTML5 API docs generator plugin for Leiningen."
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [hiccup "0.3.7"]]
  :jar-exclusions [#"(?:^|/).git/"]
  :warn-on-reflection true
  :url "https://github.com/tsdh/lein-html5-docs")