(ns io.github.sheluchin.rad-shadcn.tailwind
  "Tailwind has to scan this library's sources for the classes its components
  use, and import its theme. Both live wherever the dependency was fetched to
  (a :local/root checkout or ~/.gitlibs), so an app can't hard-code the path.

  Run from the app, with its own classpath, before each Tailwind build:

    clojure -M -m io.github.sheluchin.rad-shadcn.tailwind resources/tailwind/rad-shadcn.css

  and `@import \"./rad-shadcn.css\";` from the app's Tailwind entry file. The
  written file is machine-specific: add it to .gitignore."
  (:require
   [clojure.java.io :as io]))

(defn- resource-dir
  "The directory that holds classpath resource `path`. Tailwind can't read
  inside jars, so the library must come from a directory (:local/root or a
  git dep), not a Maven jar."
  [path]
  (let [url (io/resource path)]
    (when-not url
      (throw (ex-info (str "Not on the classpath: " path) {:path path})))
    (when-not (= "file" (.getProtocol url))
      (throw (ex-info (str "fulcro-rad-shadcn must be a :local/root or git dependency for Tailwind to read it; found " url)
                      {:url (str url)})))
    (.getParentFile (io/file url))))

(defn css
  "The CSS to import: this library's theme and an @source for its components."
  []
  (let [theme (io/file (resource-dir "rad_shadcn/theme.css") "theme.css")
        src   (resource-dir "io/github/sheluchin/rad_shadcn/ui.cljs")]
    (str "/* Written by io.github.sheluchin.rad-shadcn.tailwind; machine-specific, don't commit. */\n"
         "@import \"" (.getCanonicalPath theme) "\";\n"
         "@source \"" (.getCanonicalPath src) "\";\n")))

(defn -main [& [out]]
  (let [out (io/file (or out "resources/tailwind/rad-shadcn.css"))]
    (io/make-parents out)
    (spit out (css))
    (println "Wrote" (str out))))
