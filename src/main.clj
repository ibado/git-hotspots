(ns main)
(require '[clojure.java.io :as io])
(require '[clojure.java.process :refer [exec]])
(require '[clojure.string :as str])
(require '[clojure.tools.cli :refer [parse-opts]])

(defn get-log [path]
  (exec "git" "--git-dir" path "log" "--numstat" "--no-merges"))

(defn print-hotspots! [path file-prefix n]
  (doseq [entry
          (->>
           (str/split
            (get-log path)
            #"commit\s+[0-9a-fA-F]{40}")
           (filter not-empty)
           (map #(last (str/split % #"\n\n")))
           (map #(str/split % #"\n"))
           (map #(map (fn [l] (last (str/split l #"\t"))) %))
           (reduce (fn [acc it]
                     (reduce #(update %1 %2 (fnil inc 0)) acc it))
                   {})
           (sort-by val #(compare %2 %1))
           (filter #(str/ends-with? (key %) file-prefix))
           (take n))]
    (println entry)))

(defn git-directory? [path]
  (let [f (io/file path)]
    (and (str/ends-with? path ".git")
         (.exists f)
         (.isDirectory f))))

(def cli-options
  [["-d" "--git-dir GIT_DIR_PATH" "git dir path"
    :default "."
    :validate [#(git-directory? %) "Must be a valid directory"]]
   [nil "--file-sufix SUFIX" "file sufix to filter by"
    :default ""]
   ["-n" "--topn TOPN" "Top n hotspots"
    :default 10
    :validate [#(parse-long %) "Must be a number"]]
   ["-h" "--help" "Show this message"]])

(defn -main [& args]
  (let [m ((parse-opts args cli-options) :options)]
    (print-hotspots! (m :git-dir) (m :file-sufix) (m :topn))))

