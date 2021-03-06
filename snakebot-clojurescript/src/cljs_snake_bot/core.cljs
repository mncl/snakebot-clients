(ns cljs-snake-bot.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.nodejs :as nodejs]
            [cljs-snake-bot.messagehandler :as mh]
            [cljs-snake-bot.constants :as c]
            [cljs-snake-bot.settings :as s]
            [cljs-snake-bot.messages :as m]
            [cljs-snake-bot.printer :as p]
            [cljs.core.async :as async :refer [<! timeout]]))

(nodejs/enable-util-print!)

(def ws (nodejs/require "ws"))
(def socket (ws (str "ws://" s/host-name ":" s/host-port "/" s/game-mode)))

(defn json-str [obj]
  (JSON/stringify (clj->js obj)))

(defn json-parse [j]
  (js->clj (JSON/parse j) :keywordize-keys true))

(defn clean-up[]
  (.close socket))

(defn game-loop []
  (go-loop []
    (async/<! (async/timeout 10))
    (if (s/state-get :game-running)
      (recur)
      (clean-up))))

(defn setup-listener []
    (.on socket "message"
         (fn [msg] (let [response (mh/get-response-message (json-parse msg))]
                   (when (some? response) (.send socket (json-str response)))))))

(defn setup-socket []
  (.on socket "open"
       #(do (println "socket opened")
            (.send socket (json-str (m/get-player-registration-message "emi")))
          (setup-listener))))

(defn -main []
  (println "Booting up")
  (setup-socket)
  (p/renderer)
  (game-loop))

(set! *main-cli-fn* -main)
