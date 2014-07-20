(ns dragonmark.util.core
  "A bunch of functions and macros that are darned useful"
  (:require
   #+clj [clojure.pprint :as pp]
   clojure.string)
  )


(defn mapcatv
  "Like mapcat, but returns a vector... like mapv"
  [f coll]
  (let [results (map f coll)]
    (reduce into [] results)))

(defn removev
  "Like remove except for returning a vector"
  [pred coll]
  (filterv #(not (pred %)) coll))

(defn restv
  "Returns the vector abscent the first element. Falls throught to
rest if the parameter is not a Vector"
  [vec]
  (if (and (vector? vec) (seq vec))
    (subvec vec 1)
    (rest vec)))

(defn concatv
  "Concats sequentials into a Vector"
  ([] nil)
  ([v1 & vs] 
     (loop [root (if (vector? v1) v1 (into [] v1))
            v (first vs)
            xs (rest vs)]
       (if (and (nil? v) (empty? xs)) root
           (recur (into root v) (first xs) (rest xs))))
  ))

(defn consv
  "Prepends the value to the Vector and returns a Vector"
  [val vec]
  (if (vector? vec) (into [val] vec) (cons val vec)))

(defn map-tree
  "Applies the function f to each node in the tree, bottom-up.
Find the children using child-key and if child-key is missing, use :children"
  ([f tree] (map-tree f tree :children))
  ([f tree child-key]
     (let [child (get tree child-key)
           new-tree
           (cond
            (empty? child) tree
            
            (vector? child) (assoc tree child-key 
                                   (mapv #(map-tree f % child-key) child))

            (map? child)
            (assoc tree child-key (into {} 
                                        (map (fn [[k v]]
                                               [k (map-tree f v child-key)])
                                             child)))

            (sequential? child) (assoc tree child-key 
                                       (map #(map-tree f % child-key) child))

            :else (assoc tree child-key (f child)))
           ]
       (f new-tree)
       )))

(defn split-by
  "Splits the incoming collection by the predicate. 
Returns [(filter pred coll) (remove pred coll)]"
  [pred coll]
  (if (vector? coll)
    [(filterv pred coll) (removev pred coll)]
    [(filter pred coll) (remove pred coll)]))

#+clj 
(defmacro some-or
  "Like 'or' except returns the first computed value that is not nil"
  ([] nil)
  ([x] x)
  ([x & other]
     `(let [or# ~x]
        (if (nil? or#) (some-or ~@other) or#))))

#+clj (def ^:private scheduler
        "The JVM event scheduler"
        (delay
         (java.util.concurrent.Executors/newScheduledThreadPool 
          2
          (proxy [java.util.concurrent.ThreadFactory] []
            (newThread [runnable] 
              (let [t (Thread. runnable)]
                (.setName t "delayed execution thread")
                (.setDaemon t true)
                t)
              )))))

(defn exec-after
  "execute a function after a certain number of milliseconds"
  [f after]
  #+cljs (js/setTimeout f after)
  #+clj (.schedule @scheduler f after
                   java.util.concurrent.TimeUnit/MILLISECONDS))

  
(defn pretty
  "Pretty-formats the Clojure data structure"
  [x]
  (cond
   (string? x) x
   
   :else 
   #+clj (with-out-str (pp/pprint x))
   #+cljs (js/JSON.stringify (clj->js x))
   ))

(defn log
  "A platform-neutral logging facility."
  [ & rest]
  #+cljs (apply js/console.log rest)
  #+clj (let [string (clojure.string/join "" (map pretty rest))]
          (apply println rest)))

#+clj (def ^:private secure-random (java.security.SecureRandom.))

(def ^:private counter (atom 100000000000))

(defn- random-chars
  "generate random characters"
  []
  #+cljs (str (.getRandomString goog.string) (.getRandomString goog.string))
  #+clj (locking secure-random
          (let [a (.nextLong secure-random)
                b (.nextLong secure-random)]
            (str (Long/toString (Math/abs a) 36)
                 (Long/toString (Math/abs b) 36)
            )))
  )

(defn next-guid
  "Generate a monotonically increasing GUID with a ton of randomness"
  []
  (str "S" (swap! counter inc) (random-chars)))

(defn next-clock
  "Return a monotonically increasing number"
  []
  (swap! counter inc))

