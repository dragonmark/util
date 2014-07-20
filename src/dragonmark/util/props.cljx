(ns dragonmark.util.props
  (:require [dragonmark.util.core :as dc])
  #+cljs (:require-macros [dragonmark.util.core :as dc])
  #+clj (:import [java.net InetAddress]
                 [java.io InputStream])
  )

;; /**
;;  * Configuration management utilities.
;;  *
;;  * If you want to provide a configuration file for a subset of your application
;;  * or for a specific environment, Lift expects configuration files to be named
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
;;  *   staging.dpp.props
;;  *   test.default.props
;;  *   default.props
;;  * </pre>
;;  *
;;  * with hostName and userName being optional, and modeName being one of
;;  * "test", "staging", "production", "pilot", "profile", or "default".
;;  * The standard Lift properties file extension is "props".
;;  */

(def run-modes
  "Enumeration of available run modes."
  [:dev
   :test
   :staging
   :prod
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
    "/props/default" 
    (str "/props/" user-name )
    (str "/" mode-name "." user-name "." host-name)
    (str "/" mode-name "." user-name)
    (str "/" mode-name "." host-name)
    (str "/" mode-name ".default" )
    (str "/" user-name )
    "/default"
   ]))

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
          mapped (->>
                  (map
                   #+cljs (fn [x] nil)
                   #+clj (fn [x]
                           (let [opened (.openConnection x)
                                 last-mod (.getLastModified opened)]
                             (if (= @last-checked last-mod)
                               @info
                               (let [conn (.getContent opened)
                                     content (slurp conn)
                                     s-exp (read-string content)]
                                 (reset! info s-exp)
                                 (reset! last-checked last-mod)
                                 s-exp
                                 )
                             )))
                   possible)
                  (remove nil?)
                  first)
          ]
      (when (and mapped (not (= mapped @info)))
        (reset! info mapped))
      (or mapped @info)
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
              (reset! atom value)
              ))]
    (update :k info {} @info)
    (add-watch info nil update)
    @atom
    ))
