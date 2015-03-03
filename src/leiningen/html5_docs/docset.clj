(ns leiningen.html5-docs.docset
  (:require [clojure.java.jdbc :as db]))

(def db {:classname   "org.sqlite.JDBC"
         :subprotocol "sqlite"})

(defn create-docset-db-tables! [docset-base]
  (db/db-do-commands
   (assoc db :subname (str docset-base "/Contents/Resources/docSet.dsidx"))
   "DROP TABLE IF EXISTS searchIndex;"
   "DROP INDEX IF EXISTS anchor;"
   "CREATE TABLE searchIndex(id INTEGER PRIMARY KEY, name TEXT, type TEXT, path TEXT);"
   "CREATE UNIQUE INDEX anchor ON searchIndex (name, type, path);"))

(def insert-ps (atom nil))

(defn insert-into-db! [docset-base name type path]
  (let [db-spec (assoc db :subname (str docset-base "/Contents/Resources/docSet.dsidx"))]
    (when-not @insert-ps
      (reset! insert-ps
              (db/prepare-statement
               (db/get-connection db-spec)
               "INSERT OR IGNORE INTO searchIndex(name, type, path) VALUES (?, ?, ?);")))
    (db/execute! db-spec [@insert-ps name type path])))

(defn write-info-plist [docset-base docset-name]
  (spit (str docset-base "/Contents/Info.plist")
        (format "<?xml version=\"1.0\" encoding=\"UTF-8\"?>
<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">
<plist version=\"1.0\">
<dict>
  <key>CFBundleIdentifier</key>
  <string>%s</string>
  <key>CFBundleName</key>
  <string>%s</string>
  <key>DocSetPlatformFamily</key>
  <string>%s</string>
  <key>isDashDocset</key>
  <true/>
  <key>dashIndexFilePath</key>
  <string>index.html</string>
</dict>
</plist>"
                (clojure.string/lower-case docset-name)
                docset-name
                (clojure.string/lower-case docset-name))))
