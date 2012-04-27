(defproject lein-html5-docs "1.2.0"
  :description "A HTML5 API docs generator plugin for Leiningen."
  :dependencies [[hiccup "0.3.8"]]
  :jar-exclusions [#"(?:^|/).git/"]
  :warn-on-reflection true
  :url "https://github.com/tsdh/lein-html5-docs")