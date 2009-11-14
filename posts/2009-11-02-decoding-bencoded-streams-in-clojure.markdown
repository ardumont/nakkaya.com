---
title: Decoding BEncoded Streams in Clojure
tags: clojure bencode
---

> EDIT: I have updated the code to include both decoding and encoding,
> information on how to encode can be found
> [here](/2009/11/14/bencoding-objects-in-clojure/)

Bencode is the encoding used by file sharing system BitTorrent. Torrent
files are simply Bencoded dictionaries. This post will walk you through
my Bencode decoder, if you want to jump right in to code, it is
[here](/code/clojure/bencode.clj).

You can read about the BitTorrent specification
[here](http://www.bittorrent.org/beps/bep_0003.html) there is also a lot
of information on
[theory.org](http://wiki.theory.org/index.php/BitTorrentSpecification)
of course don't forget to check out the
[Wikipedia](http://en.wikipedia.org/wiki/Bencode) article.


Now specs out of the way, let's dissect the code,

    (defn decode [stream & i]
      (let [indicator (if (nil? i) (.read stream) (first i))]
        (cond 
         (and (>= indicator 48) 
              (<= indicator 57)) (decode-string stream indicator)
         (= (char indicator) \i) (decode-number stream \e)
         (= (char indicator) \l) (decode-list stream)
         (= (char indicator) \d) (decode-map stream))))

decode will read one byte from the stream determine it's type and call
the appropirate function.

    (defn- decode-number [stream delimeter & ch]
      (loop [i (if (nil? ch) (.read stream) (first ch)), result ""]
        (let [c (char i)]
          (if (= c delimeter)
            (BigInteger. result)
            (recur (.read stream) (str result c))))))

decode-number takes the stream that we are processing and a delimiter,
when delimiter is read we stop reading, this function is used to decode
both numbers formatted as "i23e" and byte string length "10:".


    (defn- decode-string [stream ch]
      (let [length (decode-number stream \: ch)
            buffer (make-array Byte/TYPE length)]
        (.read stream buffer)
        (String. buffer "ISO-8859-1")))

decode-string will parse the string length variable and read indicated
bytes from the string, i build the string using ISO-8859-1 that way SHA-1
hashes will not be corrupted.

    (defn- decode-list [stream]
      (loop [result []]
        (let [c (char (.read stream))]
          (if (= c \e)
            result
            (recur (conj result (decode stream (int c))))) )))

decode-list will call decode on the items until the list delimiter "e"
is read. Lists are returned as Clojure vectors.

    (defn- decode-map [stream] 
      (apply hash-map (decode-list stream)))

decode-map will decode the map as a list then apply hash-map on it
producing a Clojure map.

Download code [here](/code/clojure/bencode.clj).
