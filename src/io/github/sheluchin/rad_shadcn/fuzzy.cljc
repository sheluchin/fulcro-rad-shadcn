(ns io.github.sheluchin.rad-shadcn.fuzzy
  "Fuzzy matching for pickers: the query's characters must appear in order,
  not necessarily together (\"rngclj\" matches ring-clojure/ring)."
  (:require
   [clojure.string :as str]))

(defn- boundary?
  "Whether index `i` of `s` starts a word: the start, or after / . - _ or a space."
  [s i]
  (or (zero? i) (contains? #{\/ \. \- \_ \space} (nth s (dec i)))))

(defn score
  "How well `query` matches `text`, lower is better; nil if it doesn't. Each
  gap between matched characters costs its length, up to 3; a match starting
  a word earns 5 back; an exact substring beats any scattered match. A blank
  query matches with 0."
  [query text]
  (let [q (str/lower-case (str/trim (str query)))
        t (str/lower-case (str text))]
    (cond
      (str/blank? q) 0
      (str/includes? t q) (let [i (str/index-of t q)]
                            (- i 1000 (if (boundary? t i) 2 0)))
      :else (loop [qi 0, ti 0, prev nil, acc 0]
              (cond
                (= qi (count q)) acc
                (= ti (count t)) nil
                (= (nth q qi) (nth t ti))
                (recur (inc qi) (inc ti) ti
                       (+ acc
                          (min 3 (if prev (- ti prev 1) ti))
                          (if (boundary? t ti) -5 0)))
                :else (recur qi (inc ti) prev acc))))))

(defn search
  "The items whose `(key-fn item)` matches `query`, best first; ties keep
  their order (callers pass the most popular first)."
  [query key-fn items]
  (->> items
       (keep-indexed (fn [i item] (when-let [s (score query (key-fn item))] [s i item])))
       (sort-by (juxt first second))
       (mapv peek)))
