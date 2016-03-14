(ns dragonmark.util.props
  (:require [dragonmark.util.core :as dc])
  #+cljs (:require-macros [dragonmark.util.core :as dc])
  #+clj (:import [java.net InetAddress]
                 [java.io InputStream File])
  )

;; /**
;;  * Configuration management utilities.
;;  *
;;  * If you want to provide a configuration file for a subset of your application
;;  * or for a specific environment, Dragonmark Utils expects configuration files to be named
;;  * in a manner relating to the context in which they are being used. The standard
;;  * name format is:
;;  *
;;  * <pre>
;;  *   modeName.userName.hostName.props
;;  *
;;  *   examples:
;;  *   dpp.yak.props
;;  *   test.dpp.yak.props
;;  *   production.moose.props
;;  *   prod.moose.props
;;  *   staging.dpp.props
;;  *   test.default.props
;;  *   default.props
;;  * </pre>
;;  *
;;  * with hostName and userName being optional, and modeName being one of
;;  * "test", "staging", "production" (and its synonym "prod"), "pilot", "profile", or "default".
;;  * The standard Dragonmark Utils properties file extension is "props".
;;  */

(def run-modes
  "Enumeration of available run modes."
  [:dev
   :test
   :staging
   :prod
   :production
   ])

(def run-mode
  "The current run-mode"
  (atom
   (or
    #+clj (some-> (System/getProperty "run.mode") .toLowerCase keyword)
    #+clj (when 
              (->> (Exception.) .getStackTrace (map #(.getClassName %)) 
                   (filter #(.startsWith % "clojure.test")) empty? not) :test)
    :dev)))

(defn production-mode? 
  "Is the system running in production mode"
  []
  (or
   (= @run-mode :prod)
   (= @run-mode :production)
   (= @run-mode :staging)))


(defn dev-mode? 
  "Is the system running in production mode"
  []
  (or
   (= @run-mode :dev)))


(defn test-mode? 
  "Is the system running in production mode"
  []
  (or
   (= @run-mode :test)))

(def ^:private user-name
  (or
   #+clj (System/getProperty "user.name")
   "dragon"))

(def ^:private mode-name (name @run-mode))

(def ^:private host-name 
  #+clj (try
          (.getHostName (InetAddress/getLocalHost))
          (catch Exception e "localhost"))
  #+cljs "browser"
  )

;; A list of propperties to try
(def ^:private to-try
  (map
   #(str % ".props")
   [(str "/props/" mode-name "." user-name "." host-name)
    (str "/props/" mode-name "." user-name)
    (str "/props/" mode-name "." host-name)
    (str "/props/" mode-name ".default" )
    (str "/props/" user-name )
    "/props/default" 
    (str "/" mode-name "." user-name "." host-name)
    (str "/" mode-name "." user-name)
    (str "/" mode-name "." host-name)
    (str "/" mode-name ".default" )
    (str "/" user-name )
    "/default"
   ]))

(defn docker-props
  []
  #+clj (let [tf (File. "/.dockerenv")]
          (when (.exists tf)
            (let [pf (File. "/data/default.props")]
              (when (.exists pf)
                (-> pf .toURI .toURL)))))
  #+cljs nil)

(defn ^:private env-related-props
  "Is the property_file env var set?"
  []
  #+clj (if-let [pf-name (System/getProperty "property_file")]
          (let [pf (File. pf-name)]
            (when (.exists pf)
              (-> pf .toURI .toURL))))
  #+cljs nil)

(defn ^:private augment
  "augments a list of files with other files based on Docker and env"
  [lst]
  (let [potential [(docker-props)
                   (env-related-props)]
        potential (remove nil? potential)]
    (if (empty? potential) lst (concat potential lst)))
  )

(defn ^:private find-files
  "Looks at the list to try and returns a list of input streams"
  []
  #+clj
  (->>
   to-try
   (map (fn [f] 
          (try
            (-> (.getClass dev-mode?) (.getResource f))
            (catch Exception e nil))))
   augment
   (remove nil?))
  #+cljs nil
  )

(def info "The properties" (atom {}))

(def ^:private last-checked
  "The last change time on the props file"
  (atom -1))

(defn refresh-properties
  "try to reload the properties file"
  []
  (try
    (let [possible (find-files)
          [s-exp last-mod] (->>
                  (map
                   #+cljs (fn [x] nil)
                   #+clj (fn [x]
                           (try
                             (let [opened (.openConnection x)
                                   last-mod (.getLastModified opened)]
                               (if (= @last-checked last-mod)
                                 @info
                                 (let [conn (.getContent opened)
                                       content (slurp conn)
                                       s-exp (read-string content)]
                                   ;;(reset! info s-exp)
                                   ;;(reset! last-checked last-mod)
                                   [s-exp last-mod]
                                   )
                                 ))
                             (catch Exception _ nil)
                             ))
                   possible)
                  (remove nil?)
                  first)
          ]

      (when (and s-exp (not (= s-exp @info)))
        (reset! info s-exp)
        (reset! last-checked last-mod))
      (or s-exp @info)
      ) 
    (catch #+cljs js/Object #+clj Exception e 
           ;; FIXME log exceptions
           nil
           )))


(defn- run-check-loop
  "Set up the check loop"
  []
  (refresh-properties)
  (dc/exec-after
   run-check-loop
   (if (dev-mode?) 
     1000 ;; check every second in dev mode
     60000 ;; check every minute in not dev mode
     )))

(run-check-loop)

(defn on-prop-change
  "On the change of properties, put the keys from the
new property contents in the supplied atom."
  [keys atom]
  (letfn [(update [key r old new]
            (let [keys (if (sequential? keys) keys [keys])
                  value (reduce get new keys)]
              (when (not (= value @atom))
                (reset! atom value))
              ))]
    (update :k info {} @info)
    (add-watch info nil update)
    @atom
    ))
