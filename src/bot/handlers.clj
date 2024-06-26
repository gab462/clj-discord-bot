(ns bot.handlers
  (:require [clojure.core.async :as a]
            [discljord.messaging :as m]))

;; TODO: simplify handlers

(defn ping [state interaction]
  (println interaction)
  (m/create-interaction-response!
   (:messaging @state) (:id interaction) (:token interaction) 4
   :data {:content "pong"}))

(defn die [state interaction]
  (a/put! (:connection @state) [:disconnect])
  (m/create-interaction-response!
   (:messaging @state) (:id interaction) (:token interaction) 4
   :data {:content "Killing bot..."}))
