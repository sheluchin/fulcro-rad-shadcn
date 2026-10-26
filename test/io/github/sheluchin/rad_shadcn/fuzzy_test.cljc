(ns io.github.sheluchin.rad-shadcn.fuzzy-test
  (:require
   [io.github.sheluchin.rad-shadcn.fuzzy :as sut]
   [fulcro-spec.core :refer [=> assertions behavior specification]]))

(specification "score"
  (behavior "matches the query's characters in order, case-insensitively"
    (assertions
      (some? (sut/score "rngclj" "ring-clojure/ring")) => true
      (some? (sut/score "RING" "ring-clojure/ring")) => true
      (sut/score "jlc" "ring-clojure/ring") => nil
      (sut/score "ringx" "ring-clojure/ring") => nil))
  (behavior "matches anything with a blank query"
    (assertions
      (sut/score "  " "anything") => 0))
  (behavior "ranks a substring above a scattered match"
    (assertions
      (< (sut/score "malli" "metosin/malli") (sut/score "malli" "m-a-l-l-i/x")) => true))
  (behavior "ranks a match starting a word above one inside a word"
    (assertions
      (< (sut/score "ring" "ring-clojure/ring") (sut/score "ring" "spring/boot")) => true
      (< (sut/score "mc" "metosin/malli-core") (sut/score "mc" "metosinmxc")) => true)))

(specification "search"
  (let [repos ["weavejester/compojure" "ring-clojure/ring" "metosin/reitit" "juxt/bidi"]]
    (behavior "keeps the matches, best first"
      (assertions
        (sut/search "ring" identity repos) => ["ring-clojure/ring"]
        (first (sut/search "rei" identity repos)) => "metosin/reitit"))
    (behavior "keeps the given order on ties and for a blank query"
      (assertions
        (sut/search "" identity repos) => repos))))
