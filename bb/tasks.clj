(ns tasks
  "Helpers for bb.edn. Run `bb tasks` for the list."
  (:require
   [babashka.fs :as fs]
   [babashka.process :refer [shell]]
   [clojure.string :as str]))

(def git-url "git@github.com:sheluchin/fulcro-rad-shadcn.git")
(def lint-dirs ["src" "test" "examples" "bb"])
(def tailwind-version "^4.3.3")

(def css-dir "target/tailwind")
(def css-import (str css-dir "/rad-shadcn.css"))

(defn clj
  "Run the clojure CLI with `args`; exits on failure."
  [& args]
  (apply shell "clojure" args))

(defn- out
  "Trimmed stdout of a command, or nil if it fails."
  [& args]
  (let [{:keys [exit out]} (apply shell {:out :string :err :string :continue true} args)]
    (when (zero? exit) (str/trim out))))

(defn- fail! [& msg]
  (binding [*out* *err*] (println (str/join " " msg)))
  (System/exit 1))

;;; Lint

(defn lint []
  ;; --copy-configs brings in the configs Fulcro, RAD and guardrails ship
  ;; (defsc, defmutation, >defn), so their macros don't read as unresolved.
  (shell "clj-kondo" "--lint" (out "clojure" "-Spath" "-M:test")
         "--dependencies" "--copy-configs" "--skip-lint")
  (apply shell "clj-kondo" "--lint" lint-dirs))

;;; Tailwind

(defn write-css [path]
  (clj "-M" "-m" "io.github.sheluchin.rad-shadcn.tailwind" path))

(def ^:private entry-css
  "A minimal app entry file, as in the comment at the top of theme.css."
  (str "@layer theme, base, components, utilities;\n"
       "@import \"tailwindcss/theme.css\" layer(theme);\n"
       "@import \"tailwindcss/preflight.css\" layer(base);\n"
       "@import \"tailwindcss/utilities.css\" layer(utilities) source(none);\n"
       "@import \"./rad-shadcn.css\";\n"))

(defn check-css []
  (let [cli (str css-dir "/node_modules/.bin/tailwindcss")
        in  (str css-dir "/app.css")
        o   (str css-dir "/app.out.css")]
    (when-not (fs/exists? cli)
      (fs/create-dirs css-dir)
      (shell "npm" "install" "--prefix" css-dir "--no-save" "--no-audit" "--no-fund"
             (str "tailwindcss@" tailwind-version) (str "@tailwindcss/cli@" tailwind-version)))
    (write-css css-import)
    (spit in entry-css)
    (shell cli "-i" in "-o" o)
    (let [css     (slurp o)
          missing (remove #(str/includes? css %)
                          ;; a theme token, and utilities only the components use
                          ["--primary:" ".bg-primary" ".rounded-md"])]
      (if (seq missing)
        (fail! "Tailwind output" o "is missing:" (str/join ", " missing))
        (println "OK:" o (str "(" (count css) " bytes)"))))))

;;; Consumers

(defn print-sha []
  (let [sha (out "git" "rev-parse" "HEAD")]
    (when (seq (out "git" "status" "--porcelain" "--untracked-files=no"))
      (println ";; WARNING: uncommitted changes; they are not in this sha"))
    (when-not (seq (out "git" "branch" "-r" "--contains" sha))
      (println ";; WARNING: HEAD is not on any remote branch; push it before pinning"))
    (println (str "io.github.sheluchin/fulcro-rad-shadcn {:git/url \"" git-url "\"\n"
                  "                                :git/sha \"" sha "\"}"))))
