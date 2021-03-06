(ns leiningen.html5-docs.core
  (:use hiccup.core)
  (:require [clojure.string :as str]
            [clojure.pprint :as pp]
            [clojure.java.io :as io]
            [leiningen.html5-docs.docset :as docset]
            clojure.stacktrace)
  (:import [java.io File]))

;; TODO: Keep in sync with project.clj
(def lein-html5-docs-version "3.0.3")

(def ^:dynamic *current-file*)
(def ^:dynamic *docset-base* false)

(defn files-in [^String dirpath pattern]
  (for [^File file (-> dirpath File. file-seq)
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
    ", version " lein-html5-docs-version
    "."]
   [:a {:href "http://www.w3.org/html/logo/"}
    [:img {:src "http://www.w3.org/html/logo/badge/html5-badge-h-solo.png"
           :width "63" :height "64" :alt "HTML5 Powered"
           :title "HTML5 Powered"}]]
   (when-not *docset-base*
     (html
      ;; Reference the search JavaScripts as last step to make the site render as
      ;; fast as possible.
      [:script {:src "http://code.jquery.com/jquery.min.js"}]
      [:script {:src "http://code.jquery.com/ui/1.11.4/jquery-ui.min.js"}]
      [:script {:src "api-search.js"}]))])

(def css
  "body { margin: 10px;
          padding: 10px;
          background-color: #DFE9F5;
          font-family: sans-serif; }

  #top { width: 800px;
         padding: 10px;
         margin-left: auto;
         margin-right: auto;
         overflow: hidden;
         background-color: #FFFFFF;
         color: #123154;
         border: 3px solid #B4D3F5; }

  pre { padding: 10px;
        border: 2px dashed #B4D3F5;
        background-color: #F0F6FD;
        font-family: monospace; }

  a { color: #4A6A8F; }
  a:hover { background-color: #B4D3F5; }
  a:visited { color: #425365; }

  td, th { padding-left: 5px;
           text-align: left; }

  #ui-widget {
    position: fixed;
    top: 0em;
    left: 0em;
  }")

(defn html-page [title contents]
  (html
   html-header
   [:html
    [:head
     [:meta {:charset "utf-8"}]
     [:title title]
     [:style {:type "text/css"} css]
     [:link {:rel "stylesheet"
             :href "http://code.jquery.com/ui/1.11.4/themes/smoothness/jquery-ui.css"}]]
    [:body
     [:div {:id "ui-widget"}
      (when-not *docset-base*
        (html
         [:input {:id "api-search"
                  :autofocus "autofocus"
                  :tabindex "1"
                  :placeholder "API Search"}]))]
     contents
     (page-footer)]]))

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
    (html-page
     (str pname " API Documentation")
     [:div {:id "top"}
      [:header
       [:h1 pname [:small " (version " (:version project) ")"]]
       [:p (:description project)]]
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
      [:section {:id "all-ns-toc" :class "ns-toc"}
       [:h2 "Namespaces"]
       [:table
        (for [nsp nsps]
          [:tr
           [:td [:a {:href (str (name nsp) ".html")}
                 (name nsp)]]
           [:td [:div
                 (shorten (:doc (meta (find-ns nsp))) 100)]]])
        [:tr
         [:td [:a {:href "var-index.html"} "Alphabetic Var Index"]]
         [:td ""]]]]])))

(defn gen-ns-toc
  "Generate a TOC of the other namespaces.
  nsp is the current namespace, nsps all namespaces."
  [nsp nsps]
  [:section {:id "other-ns-toc" :class "ns-toc"}
   [:h2 "Other Namespaces"]
   [:details {:open false}
    [:summary "Show/Hide"]
    [:table
     (for [onsp nsps
           :when (not= nsp onsp)]
       [:tr
        [:td [:a {:href (str (name onsp) ".html")}
              (name onsp)]]
        [:td [:div
              (shorten (:doc (meta (find-ns onsp))) 100)]]])
     [:tr
      [:td [:a {:href "index.html"} "Index Page"]]
      [:td ""]]
     [:tr
      [:td [:a {:href "var-index.html"} "Alphabetic Var Index"]]
      [:td ""]]]]])

(defn gen-ns-vars-toc
  "Generates a TOC for all public vars in pubs."
  [pubs]
  [:section {:id "pubvars-toc"}
   [:h2 "Public Vars"]
   [:nav
    (interpose ", "
               (for [[s _] pubs]
                 [:a {:href (str "#" (make-id s))} (h s)]))]])

(defn indent
  [s]
  (reduce str
          (for [line (str/split-lines s)]
            (str "  " line "\n"))))

(defn- ensure-trailing-slash [^String s]
  (if (.endsWith s "/")
    s
    (str s "/")))

(defn source-link
  [project v]
  (when v
    (if-let [f (:file (meta v))]
      (str (let [url (:html5-docs-repository-url project)]
             (cond
               (string? url) url
               ;; fns are only read as lists, so we need to eval them
               (and (list? url)
                    (or (= 'fn (first url))
                        (= 'fn* (first url)))) ((eval url) project)
                        :else (throw (RuntimeException.
                                      (str ":html5-docs-repository-url must be a string or a fn but was "
                                           url (type url))))))
           (ensure-trailing-slash
            (str/replace (or (:html5-docs-source-path project)
                             (first (:source-paths project)))
                         (:root project)
                         ""))
           (.replaceFirst ^String f
                          (ensure-trailing-slash
                           (or (:html5-docs-source-path project)
                               (first (:source-paths project))))
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

(defn docset-path
  ([]
   (.getName (io/file *current-file*)))
  ([id]
   (str (.getName (io/file *current-file*)) "#" id)))

(defn gen-fn-details [id v s es]
  (let [kind (cond
               (constructor? v)     "Constructor"
               (:macro (meta v))    "Macro"
               (:protocol (meta v)) "Method"
               :else                "Function")]
    (when *docset-base*
      (docset/insert-into-db! *docset-base* s
                              kind (docset-path id)))
    [:div
     [:h4 (str kind ": " es)]
     [:pre
      (when-let [prot (:name (meta (:protocol (meta v))))]
        (str "Specified by protocol " (name prot) ".\n\n"))
      "Arglists:\n=========\n\n"
      (h
       (html
        (binding [pp/*print-miser-width*  60
                  pp/*print-right-margin* 80]
          (map #(let [sig `(~s ~@%)]
                  (indent (with-out-str
                            (pp/pprint sig))))
               (:arglists (meta v))))))
      "\nDocstring:\n==========\n\n  "
      (h
       (or (:doc (meta v))
           "No docs attached."))]]))

(defn gen-protocol-details [id v s es]
  (when *docset-base*
    (docset/insert-into-db! *docset-base* s "Protocol" (docset-path id)))
  [:div
   [:h4 "Protocol: " es]
   [:pre
    "Docstring:\n==========\n\n  "
    (h
     (or (:doc (meta v))
         "No docs attached."))
    "\n\nExtenders:\n==========\n\n"
    (h
     (html
      (for [ex (map ;; nil won't be printed so replace it with
                ;; "nil". (Protocols can be extended to nil.)
                #(if (nil? %) "nil" %)
                (extenders @v))]
        (indent (str "- " ex "\n")))))
    "\nSignatures:\n===========\n\n"
    (h
     (html
      (binding [pp/*print-miser-width*  60
                pp/*print-right-margin* 80]
        (map (fn [[n al d]]
               (str (indent (with-out-str
                              (pp/pprint `(~n ~@al))))
                    (when d
                      (str (indent (str "\n  " d)) "\n"))))
             (for [sig (:sigs @v)
                   :let [s (second sig)]
                   al (:arglists s)]
               [(:name s) al (when (= al (last (:arglists s)))
                               (:doc s))])))))]])

(defn gen-var-details [id v s es]
  (when *docset-base*
    (docset/insert-into-db! *docset-base* s "Variable" (docset-path id)))
  [:div
   [:h4 (when (:dynamic (meta v)) "Dynamic ") "Var: " es]
   [:pre "  " (h
               (or (:doc (meta v))
                   "No docs attached."))]])

(defn gen-ns-vars-details
  "Generates detailed docs for the public vars pubs."
  [project pubs]
  [:section {:id "details"}
   [:h2 "Details of Public Vars"]
   (for [[s v] pubs]
     (let [es (h s)
           id (make-id s)]
       [:div {:id id}
        (cond
          (and (bound? v) (fn? @v))       (gen-fn-details id v s es)
          (and (bound? v) (protocol? @v)) (gen-protocol-details id v s es)
          :else                           (gen-var-details id v s es))
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
      [:pre (h details)]]
     [:a {:href "#top"} "Back to top"]]))

(defmacro with-err-str
  "Evaluates exprs in a context in which *out* is bound to a fresh
  StringWriter.  Returns the string created by any nested printing
  calls."
  [& body]
  `(let [s# (java.io.StringWriter.)]
     (binding [*err* s#]
       ~@body
       (str s#))))

(defn ns-pubs-no-proxies [nsp]
  (into {}
        (filter (fn [[s v]]
                  (if-let [n (:name (meta v))]
                    (not (re-matches #".*\.proxy\$.*" (name n)))
                    true))
                (ns-publics nsp))))

(defn gen-namespace-pages [nsps docs-dir project]
  (doseq [nsp nsps]
    (binding [*current-file* (let [hf (str docs-dir "/" (name nsp) ".html")]
                               (println "  -" hf)
                               hf)]
      (when *docset-base*
        (docset/insert-into-db! *docset-base* (name nsp) "Namespace" (docset-path)))
      (spit *current-file*
            (let [pubs (sort (ns-pubs-no-proxies nsp))]
              (html-page
               (str "Namespace " nsp)
               ;; Namespace Header
               [:div {:id "top"}
                [:header
                 [:h1 "Namespace "(name nsp)]
                 [:p (shorten (:doc (meta (find-ns nsp))) 100)]]
                ;; Namespace TOC
                (gen-ns-toc nsp nsps)
                ;; TOC of Vars
                (gen-ns-vars-toc pubs)
                ;; Usage docs
                (gen-usage-docs nsp)
                ;; Contents
                (gen-ns-vars-details project pubs)]))))))

(defn all-syms-with-vars [nsps]
  (sort (apply merge-with
               (fn [val1 val2]
                 (if (coll? val1)
                   (conj val1 val2)
                   (sorted-set-by
                    (fn [v1 v2]
                      (compare (ns-name (:ns (meta v1)))
                               (ns-name (:ns (meta v2)))))
                    val1 val2)))
               (map ns-pubs-no-proxies nsps))))

(defn gen-var-index-page [nsps docs-dir]
  (let [all-vars (all-syms-with-vars nsps)
        idx (apply sorted-map
                   (mapcat (fn [x]
                             (let [n (first x)]
                               [(str (first (name n))) n]))
                           (partition-by #(first (name %)) (keys all-vars))))]
    (spit (str docs-dir "/var-index.html")
          (html-page
           "Alphabetic Var Index"
           [:div {:id "top"}
            [:header
             [:h1 "Alphabetic Var Index"]]
            (gen-ns-toc nil nsps)
            [:section
             [:h2 "Alphabetic Var Index"]
             [:p "Jump to: "
              (interpose
               " "
               (map
                (fn [[c sym]]
                  [:a {:href (str "#" (make-id sym))}
                   c])
                idx))]]
            [:section
             [:table
              [:tr [:th "Var Name"] [:th "Declaring Namespaces"]]
              (for [[sym vars] all-vars
                    :let [symid (make-id sym)]]
                (let [vars (if (coll? vars) vars [vars])]
                  [:tr
                   [:td [:div {:id symid} sym]]
                   [:td
                    (interpose ", "
                               (for [v vars
                                     :let [ns-name (name (ns-name (:ns (meta v))))]]
                                 [:a {:href (str ns-name ".html#" symid)}
                                  ns-name]))]]))]]]))))

(defn gen-search-index [nsps docs-dir]
  (let [all-vars (mapcat #(vals (ns-pubs-no-proxies %)) nsps)
        idx (apply sorted-map
                   (mapcat (fn [v]
                             (let [n (:name (meta v))
                                   nsname (ns-name (:ns (meta v)))
                                   qname (str nsname "/" n)]
                               [qname (str nsname ".html#" (make-id n))]))
                           all-vars))]
    (spit (str docs-dir "/api-search.js")
          (str "$(function() {\n"
               "  var index = [\n    "
               (apply str
                      (interpose
                       ",\n    "
                       (for [[qn link] idx]
                         (str "{label: \"" qn "\", value: \"" link "\"}"))))
               "  ];\n"
               "  $('#api-search').autocomplete({\n"
               "     source: index,\n"
               "     focus: function(event, ui) {\n"
               "       event.preventDefault();\n"
               "     },\n"
               "     select: function(event, ui) {\n"
               "       window.open(ui.item.value, '_self');\n"
               "       ui.item.value = '';\n"
               "     }\n"
               "  });\n"
               "});\n\n"))))

(defn html5-docs
  [project & args]
  (println (format "Running lein-html5-docs version %s." lein-html5-docs-version))
  ;; (clojure.pprint/pprint project)
  (when-not (seq project)
    (throw (RuntimeException. "Empty or no project map given!")))
  (binding [*docset-base* (when (some #{":docset"} args)
                            (str (or (:html5-docs-name project)
                                     (:name project))
                                 ".docset"))]
    (let [docs-dir (if *docset-base*
                     (let [dd (str *docset-base* "/Contents/Resources/Documents")]
                       (io/make-parents (str dd "/"))
                       (docset/create-docset-db-tables! *docset-base*)
                       (docset/write-info-plist *docset-base* (or (:html5-docs-name project)
                                                                  (:name project)))
                       (let [[i16 i32] (:html5-docs-docset-icons project)
                             fs (java.nio.file.FileSystems/getDefault)
                             path (fn [p]
                                    (let [[f & more] (str/split
                                                      p (re-pattern
                                                         (.getSeparator fs)))]
                                      (.getPath
                                       fs f (into-array String more))))
                             copy (fn [f1 f2]
                                    (let [^java.nio.file.Path p1 (path f1)]
                                      (when (.exists (.toFile p1))
                                        (java.nio.file.Files/copy
                                         p1 ^java.nio.file.Path (path f2)
                                         ^"[Ljava.nio.file.CopyOption;"
                                         (into-array
                                          java.nio.file.CopyOption
                                          [java.nio.file.StandardCopyOption/REPLACE_EXISTING])))))]
                         (copy i16 (str *docset-base* "/icon.png"))
                         (copy i32 (str *docset-base* "/icon@2x.png")))
                       dd)
                     (or (:html5-docs-docs-dir project) "docs"))
          err (with-err-str
                (println "Loading Files")
                (println "=============")
                (println)
                ;; Load all clojure files
                (let [all-files (files-in (or (:html5-docs-source-path project)
                                              (first (:source-paths project)))
                                          #".*\.clj")]
                  (doseq [f all-files]
                    (try
                      (load-file f)
                      (println "  -" f)
                      (catch Throwable ex
                        (println "  - [FAIL]" f)
                        (binding [*out* *err*]
                          (clojure.stacktrace/print-stack-trace ex))))))
                ;; Generate the docs
                (let [nsps (filter #(and
                                     (if (:html5-docs-ns-includes project)
                                       (re-matches (:html5-docs-ns-includes project) (name %))
                                       true)
                                     (if (:html5-docs-ns-excludes project)
                                       (not (re-matches (:html5-docs-ns-excludes project) (name %)))
                                       true))
                                   (sort (map ns-name (all-ns))))
                      index-file (str docs-dir "/index.html")]
                  (io/make-parents index-file)
                  (spit index-file (gen-index-page project nsps))
                  (println)
                  (println "Generating Documentation")
                  (println "========================")
                  (println)
                  (gen-namespace-pages nsps docs-dir project)
                  (gen-var-index-page nsps docs-dir)
                  (when-not *docset-base*
                    (gen-search-index nsps docs-dir)))
                (println)
                (println "Finished."))]
      (when (seq err)
        (println "Some warnings occured, see html5-docs.log.")
        (spit "html5-docs.log" err)))))
