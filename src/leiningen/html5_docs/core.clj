(ns leiningen.html5-docs.core
  (:use hiccup.core)
  (:use clojure.java.io)
  (:require [clojure.string :as str])
  (:require clojure.stacktrace)
  (:require [clojure.pprint :as pp])
  (:import [java.io File]))

(defn files-in [^String dirpath pattern]
  (for [^java.io.File file (-> dirpath File. file-seq)
        :when (re-matches pattern (.getName file))]
    (.getPath file)))

(def html-header
  "<!DOCTYPE html>")

(defn page-footer []
  [:footer
   [:p
    "These docs were generated by the "
    [:a {:href "https://github.com/tsdh/lein-html5-docs"}
     "Leiningen HTML5 Docs Plugin"]
    "."]
   [:a {:href "http://www.w3.org/html/logo/"}
    [:img {:src "http://www.w3.org/html/logo/badge/html5-badge-h-solo.png"
           :width "63" :height "64" :alt "HTML5 Powered"
           :title "HTML5 Powered"}]]])

(def css
  "body { margin: 10px;
          padding: 10px;
          background-color: #F7F7F7;
          font-family: serif; }

  h1, h2, h3, h4 { color:#116275; }

  code { font-size:12px;
         font-family: monospace; }

  pre { padding: 5px;
        border: 2px dashed DarkGray;
        background-color: #F7F7F7;
        font-size:12px;
        font-family: monospace; }

  a { color: #116275; }
  a:hover { background-color: #A8DFE6; }
  a:visited { color: #276B86; }

  td { padding-left: 5px; }

  section, footer, header { float: left; }

  #top { width: 800px;
         padding: 10px;
         margin-left: auto;
         margin-right: auto;
         overflow: hidden;
         background-color: #FFFFFF;
         border: 3px solid DarkGray; }")

(defn make-id
  [x]
  (str "ID"
       (-> x
           (clojure.string/replace "<" "SMALLER")
           (clojure.string/replace ">" "GREATER")
           (clojure.string/replace "*" "MUL")
           (clojure.string/replace "+" "PLUS")
           (clojure.string/replace "?" "QMARK"))))

(defn shorten [s max]
  (if (and s (> (count s) max))
    (let [short (str (first (str/split s #"\.\p{Space}")) ".")]
      (if (seq short)
        (if (< (count short) max)
          short
          (str (subs short 0 max) "..."))
        (str (subs s 0 max) "...")))
    s))

(defn gen-index-page
  "Generates an index page."
  [project nsps]
  (let [pname (or (:html5-docs-name project) (:name project))]
    (html
     html-header
     [:html
      [:head
       [:meta {:charset "utf-8"}]
       [:title (or (:html5-docs-page-title project)
                   (str pname " API Documentation"))]
       [:style {:type "text/css"} css]]
      [:body
       [:div {:id "top"}
        [:header
         [:h1 pname [:small " (version " (:version project) ")"]]
         [:h4 (:description project)]]
        (when-let [url (:url project)]
          [:section
           "For more information, visit the "
           [:a {:href url} pname " Homepage"]
           "."])
        (when-let [lic (:license project)]
          [:section
           pname " is licensed under the "
           [:a {:href (:url lic)} (:name lic)]
           ". " [:small (:comments lic)]])
        [:section {:id "ns-toc"}
         [:h2 "Namespaces"]
         [:table
          (for [nsp nsps]
            [:tr
             [:td [:a {:href (str (name nsp) ".html")}
                   (name nsp)]]
             [:td [:div
                   (shorten (:doc (meta (find-ns nsp))) 100)]]])]]
        (page-footer)]]])))

(defn gen-ns-toc
  "Generate a TOC of the other namespaces.
  nsp is the current namespace, nsps all namespaces."
  [nsp nsps]
  [:section {:id "ns-toc"}
   [:h2 "Other Namespaces"]
   [:table
    (for [onsp nsps
          :when (not= nsp onsp)]
      [:tr
       [:td [:a {:href (str (name onsp) ".html")}
             (name onsp)]]
       [:td [:div
             (shorten (:doc (meta (find-ns onsp))) 100)]]])
    [:tr
     [:td [:a {:href "index.html"} "Back to Index"]]
     [:td ""]]]])

(defn gen-public-vars-toc
  "Generates a TOC for all public vars in pubs."
  [pubs]
  [:section {:id "pubvars-toc"}
   [:h2 "Public Vars"]
   [:nav
    (interpose ", "
               (for [[s _] pubs]
                 [:a {:href (str "#" (make-id s))} (escape-html s)]))]])

(defn indent
  [s]
  (reduce str
          (for [line (str/split-lines s)]
            (str "  " line "\n"))))

(defn source-link
  [project v]
  (when v
    (if-let [f (:file (meta v))]
      (str (:html5-docs-repository-url project)
           (str/replace (or (:html5-docs-source-path project)
                            (first (:source-paths project)))
                        (:root project)
                        "")
           (.replaceFirst ^String f
                          (or (:html5-docs-source-path project)
                              (first (:source-paths project)))
                          "")
           "#L" (:line (meta v)))
      (source-link project (:protocol (meta v))))))

(defn protocol? [v]
  (and (map? v)
       (:on-interface v)
       (:on v)
       (:sigs v)
       (:var v)
       (:method-map v)
       (:method-builders v)))

(defn constructor?
  "Is Var v the constructor fn of a deftype or defrecord?"
  [v]
  (and (fn? @v)
       (:arglists (meta v))
       (:name (meta v))
       (.startsWith ^String (name (:name (meta v))) "->")))

(defn gen-fn-details [v s es]
  [:div
   [:h3 (cond
         (constructor? v)     "Type/Record Constructor: "
         (:macro (meta v))    "Macro: "
         (:protocol (meta v)) "Protocol Method: "
         :else                "Function: ")
    es]
   [:pre
    (when-let [prot (:name (meta (:protocol (meta v))))]
      (str "Specified by protocol " (name prot) ".\n\n"))
    "Arglists:\n=========\n\n"
    (escape-html
     (html
      (binding [pp/*print-miser-width*  60
                pp/*print-right-margin* 80]
        (map #(let [sig `(~s ~@%)]
                (indent (with-out-str
                          (pp/pprint sig))))
             (:arglists (meta v))))))
    "\nDocstring:\n==========\n\n  "
    (escape-html
     (or (:doc (meta v))
         "No docs attached."))]])

(defn gen-protocol-details [v s es]
  [:div
   [:h3 "Protocol: " es]
   [:pre
    "Docstring:\n==========\n\n  "
    (escape-html
     (or (:doc (meta v))
         "No docs attached."))
    "\n\nExtenders:\n==========\n\n"
    (escape-html
     (html
      (for [ex (extenders @v)]
        (indent (str "- " ex "\n")))))
    "\nSignatures:\n===========\n\n"
    (escape-html
     (html
      (binding [pp/*print-miser-width*  60
                pp/*print-right-margin* 80]
        (map (fn [[n als d]]
               (str (indent (with-out-str
                              (pp/pprint `(~n ~@als))))
                    (when d
                      (str (indent (str "\n  " d)) "\n"))))
             (for [sig (:sigs @v)
                   :let [s (second sig)]]
               [(:name s) (:arglists s) (:doc s)])))))]])

(defn gen-var-details [v s es]
  [:div
   [:h3 (when (:dynamic (meta v)) "Dynamic ") "Var: " es]
   [:pre "  " (escape-html
               (or (:doc (meta v))
                   "No docs attached."))]])

(defn gen-public-vars-details
  "Generates detailed docs for the public vars pubs."
  [project pubs]
  [:section {:id "details"}
   [:h2 "Details of Public Vars"]
   (for [[s v] pubs]
     (let [es (escape-html s)
           id (make-id s)]
       [:div {:id id}
        (cond
         (and (bound? v) (fn? @v))       (gen-fn-details v s es)
         (and (bound? v) (protocol? @v)) (gen-protocol-details v s es)
         :else                           (gen-var-details v s es))
        ;; Link to sources
        [:a {:href "#top"} "Back to top"]
        " "
        [:a {:href (source-link project v)} "View Source"]]))])

(defn gen-usage-docs
  [nsp]
  (when-let [details (:doc (meta (find-ns nsp)))]
    [:section
     [:h2 "Usage Documentation"]
     [:details {:open true}
      [:summary "Show/Hide"]
      [:pre (escape-html details)]]
     [:a {:href "#top"} "Back to top"]]))

(defmacro with-err-str
  "Evaluates exprs in a context in which *out* is bound to a fresh
  StringWriter.  Returns the string created by any nested printing
  calls."
  [& body]
  `(let [s# (new java.io.StringWriter)]
     (binding [*err* s#]
       ~@body
       (str s#))))

(defn html5-docs
  [project]
  ;; (clojure.pprint/pprint project)
  (when-not (seq project)
    (throw (RuntimeException. "Empty or no project map given!")))
  (let [docs-dir (or (:html5-docs-docs-dir project) "docs")
        err (with-err-str
              (println "Loading Files")
              (println "=============")
              (println)
              (let [done (atom #{})
                    all-files (files-in (or (:html5-docs-source-path project)
                                            (first (:source-paths project)))
                                        #".*\.clj")]
                (dotimes [_ 3]
                  (doseq [f (remove @done all-files)]
                    (try
                      (load-file f)
                      (swap! done conj f)
                      (println "  -" f)
                      (catch Throwable ex
                        (println "  - [FAIL]" f)
                        (binding [*out* *err*]
                          (clojure.stacktrace/print-stack-trace ex)))))))
              (let [nsps (filter #(and
                                   (if (:html5-docs-ns-includes project)
                                     (re-matches (:html5-docs-ns-includes project) (name %))
                                     true)
                                   (if (:html5-docs-ns-excludes project)
                                     (not (re-matches (:html5-docs-ns-excludes project) (name %)))
                                     true))
                                 (sort (map ns-name (all-ns))))
                    index-file (str docs-dir "/index.html")]
                (clojure.java.io/make-parents index-file)
                (spit index-file (gen-index-page project nsps))
                (println)
                (println "Generating Documentation")
                (println "========================")
                (println)
                (doseq [nsp nsps]
                  (spit (let [hf (str docs-dir "/" (name nsp) ".html")]
                          (println "  -" hf)
                          hf)
                        (html
                         html-header
                         [:html
                          [:head
                           [:meta {:charset "utf-8"}]
                           [:title (str "Namespace " nsp)]
                           [:style {:type "text/css"} css]]
                          (let [pubs (filter (fn [[s v]]
                                               ;; Exclude proxies
                                               (if-let [n (:name (meta v))]
                                                 (not (re-matches #".*\.proxy\$.*" (name n)))
                                                 true))
                                             (sort (ns-publics nsp)))]
                            [:body
                             ;; Namespace Header
                             [:div {:id "top"}
                              [:header
                               [:h1 "Namspace "(name nsp)]
                               [:h4 (shorten (:doc (meta (find-ns nsp))) 100)]]
                              ;; Namespace TOC
                              (gen-ns-toc nsp nsps)
                              ;; TOC of Vars
                              (gen-public-vars-toc pubs)
                              ;; Usage docs
                              (gen-usage-docs nsp)
                              ;; Contents
                              (gen-public-vars-details project pubs)
                              (page-footer)]])]))))
              (println)
              (println "Finished."))]
    (when (seq err)
      (println "Some warnings occured, see html5-docs.log.")
      (spit "html5-docs.log" err))))
