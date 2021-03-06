---
title: Using Clojure to Connect to GMail via IMAP
tags: clojure imap gmail javamail
---

I had to dig through quite a few docs to get a couple of raw emails from
my IMAP account so I am posting the snippet for future reference.  I
have uploaded necessary jars to [clojars](http://clojars.org/) under the
group
[org.clojars.nakkaya.javax.mail](http://clojars.org/groups/org.clojars.nakkaya.javax.mail)
, in case anyone wants to play with it.

     (ns gmail.core
       (:use clojure.contrib.java-utils)
       (:import (javax.mail Session Folder Flags)
                (javax.mail.search FlagTerm)
                (javax.mail Flags$Flag)))

     (defn store [protocol server user pass]
       (let [p (as-properties [["mail.store.protocol" protocol]])]
         (doto (.getStore (Session/getDefaultInstance p) protocol)
           (.connect server user pass))))

     (def gmail (store "imaps" "imap.gmail.com" 
                       "nurullah@nakkaya.com" "super_secret_pass"))

Opening a connection is quite simple, we ask for a session instance and
connect to the store (IMAP server in JavaMail lingo).

     (defn folders 
       ([s] (folders s (.getDefaultFolder s)))
       ([s f]
          (let [sub? #(if (= 0 (bit-and (.getType %) 
                                        Folder/HOLDS_FOLDERS)) false true)]
            (map #(cons (.getName %) (if (sub? %) (folders s %))) (.list f)))))

Now that the connection to the store is ready, we need to get a list of
folders (Labels in GMail), to get a list of folders, first get the users
default folder, that will give us the top level folders we recursively
check each folder for sub folders returning a list of all folders and
subfolders.

     gmail.core=> (folders gmail)
     (("Fatura") ("INBOX") ("[Gmail]" ("Starred") ("Trash")) ("clojure") 
      ("compojure") ("firmata-devel") ("help-gnu-emacs") ("ikiteker") 
      ("incanter") ("java-dev") ("jna-users") ("leningen")
      ("metasploit") ("neurobotics") ("org-mode") ("pen-test") 
      ("ring-clojure"))

A folder name and a connection to the store is all that is needed to
interact with messages.

     (defn messages [s fd & opt]
       (let [fd (doto (.getFolder s fd) (.open Folder/READ_ONLY))
             [flags set] opt
             msgs (if opt 
                    (.search fd (FlagTerm. (Flags. flags) set)) 
                    (.getMessages fd))]
         (map #(vector (.getUID fd %) %) msgs)))

Before interacting with a folder we need to open it, either in read only
mode or read/write mode. Without the optional parameters, messages just
returns a sequence of messages, optional parameters allows us to search
for messages using flags such as seen, deleted etc.

     gmail.core=> (take 3 (messages gmail "INBOX"))

     ([8709 #<IMAPMessage com.sun.mail.imap.IMAPMessage@1320a41>] 
      [8712 #<IMAPMessage com.sun.mail.imap.IMAPMessage@3f4ebd>] 
      [8713 #<IMAPMessage com.sun.mail.imap.IMAPMessage@4a5c78>])

     gmail.core=> (messages gmail "clojure" Flags$Flag/SEEN false)

     ([11401 #<IMAPMessage com.sun.mail.imap.IMAPMessage@13b5a3a>] 
      [11402 #<IMAPMessage com.sun.mail.imap.IMAPMessage@1a0b53e>])

The whole reason, I came up with this snippet was to get a copy of raw
messages, saved using their IMAP UID,

    (defn dump [msgs]
      (doseq [[uid msg] msgs]
        (.writeTo msg (java.io.FileOutputStream. (str uid)))))

    (dump (take 3 (messages gmail "INBOX")))
