---
title: A Simple Clojure IRC Client
tags: clojure network
---

The other night I was toying with the following script, I was going to
thrash it but figured it may help someone or me later on so I am dumping
it here. It doesn't do anything other then to sit idle in a channel,

     (ns irc
       (:import (java.net Socket)
                (java.io PrintWriter InputStreamReader BufferedReader)))

     (def freenode {:name "irc.freenode.net" :port 6667})
     (def user {:name "Nurullah Akkaya" :nick "nakkaya"})

     (declare conn-handler)

     (defn connect [server]
       (let [socket (Socket. (:name server) (:port server))
             in (BufferedReader. (InputStreamReader. (.getInputStream socket)))
             out (PrintWriter. (.getOutputStream socket))
             conn (ref {:in in :out out})]
         (doto (Thread. #(conn-handler conn)) (.start))
         conn))

     (defn write [conn msg]
       (doto (:out @conn)
         (.println (str msg "\r"))
         (.flush)))

     (defn conn-handler [conn]
       (while 
        (nil? (:exit @conn))
        (if (.ready (:in @conn)) 
          (let [msg (.readLine (:in @conn))]
            (println msg)
            (cond 
             (re-find #"^ERROR :Closing Link:" msg) 
             (dosync (alter conn merge {:exit true}))
             (re-find #"^PING" msg)
             (write conn (str "PONG "  (re-find #":.*" msg)))))
          (Thread/sleep 100))))

     (defn login [conn user]
       (write conn (str "NICK " (:nick user)))
       (write conn (str "USER " (:nick user) " 0 * :" (:name user))))

     ;;(def irc (connect freenode))
     ;;(login irc user)
     ;;(write irc "JOIN #clojure")
     ;;(write irc "QUIT")

Lots of this code should be self-explanatory, calling connect will open
a socket to the server, it will return a ref containing a reader and a
writer associated with the socket, it will also spawn a new thread that
will handle incoming messages from the server.

conn-handler will keep reading and printing from the socket until exit
key in the conn ref is set which happens when we receive a "Closing
Link" message from the server, every once in a while server will ping us
with "PING :randomstring" we need to reply "PONG :randomstring" else we
get disconnected. Thats all there is to it, as I said it doesn't do
anything but with a few regexes you can turn it in to client or a bot.
