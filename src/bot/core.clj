(ns bot.core
  (:require [clojure.edn :as edn]
            [clojure.core.async :as a]
            [discljord.connections :as c]
            [discljord.messaging :as m]
            [discljord.events :as e]
            [bot.handlers :as handle]))

(def state (atom nil))

(def bot (edn/read-string (slurp "resources/bot.edn")))

(def commands
  [{:name "ping"
    :description "Replies with pong"
    :options []
    :handler handle/ping}
   {:name "die"
    :description "Kill bot"
    :options []
    :handler handle/die}])

; TODO: handle command arguments
(def handlers
  {:interaction-create
   [(fn [event-type interaction]
      (let [name (get-in interaction [:data :name])
            handler (->> commands (filter #(= (:name %) name)) first :handler)]
        (handler state interaction)))]})

(defn -main [& args]
  (let [event-ch (a/chan 100)
        connection-ch (c/connect-bot! (:token bot) event-ch :intents #{})
        messaging-ch (m/start-connection! (:token bot))
        created-commands @(m/bulk-overwrite-guild-application-commands!
                           messaging-ch (:app-id bot) (:guild-id bot) (map #(dissoc % :handler) commands))]
    (reset! state {:connection connection-ch :event event-ch :messaging messaging-ch})
    (try (e/message-pump! event-ch (partial e/dispatch-handlers handlers))
      (finally
        (m/stop-connection! messaging-ch)
        (c/disconnect-bot! connection-ch)))))
