(ns bot.core
  (:require [clojure.edn :as edn]
            [clojure.core.async :as a]
            [discljord.connections :as c]
            [discljord.messaging :as m]
            [discljord.events :as e]
            [bot.handlers :as handle]))

(def state (atom nil))

(def bot (edn/read-string (slurp "resources/bot.edn")))

(def handlers
  {:interaction-create
   [(fn [event-type interaction]
      (println interaction)
      (case (get-in interaction [:data :name])
        "ping" (handle/ping state interaction)
        "die" (handle/die state interaction)))]})

(def commands
  [{:name "ping" :description "Replies with pong" :options []}
   {:name "die" :description "Kill bot" :options []}])

(defn -main [& args]
  (let [event-ch (a/chan 100)
        connection-ch (c/connect-bot! (:token bot) event-ch :intents #{})
        messaging-ch (m/start-connection! (:token bot))
        created-commands @(m/bulk-overwrite-guild-application-commands!
                           messaging-ch (:app-id bot) (:guild bot) commands)]
    (reset! state {:connection connection-ch :event event-ch :messaging messaging-ch})
    (try (e/message-pump! event-ch (partial e/dispatch-handlers handlers))
      (finally
        (m/stop-connection! messaging-ch)
        (c/disconnect-bot! connection-ch)))))
