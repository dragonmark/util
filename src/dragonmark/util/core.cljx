(ns dragonmark.util.core
  "A bunch of functions and macros that are darned useful")


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

