(defproject lein-html5-docs "1.0.4"
  :description "A HTML5 API docs generator plugin for Leiningen."
  :dependencies [[hiccup "0.3.7"]]
  :jar-exclusions [#"(?:^|/).git/"]
  :warn-on-reflection true
  :url "https://github.com/tsdh/lein-html5-docs")