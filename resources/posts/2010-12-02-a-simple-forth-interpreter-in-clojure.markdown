---
title: A Simple Forth Interpreter in Clojure
tags: clojure forth
---

Just for fun I sat down and started writing a Forth interpreter in
Clojure. This implementation only does some simple arithmetic, it has
dup and "." (print), but lacks things like control structures.

Our Forth environment has two key components, 

 - Dictionary
 - Stack

Dictionary holds both primitive words, those that are implemented in
Clojure and user defined words, it is a Clojure map which uses a word as
its key and a Clojure function as its value. Stack is implemented using
a list, words operate on stack by poping some values operating on them
and pushing the result back to stack.

     (ns forth
       (:refer-clojure :exclude [pop!]))

     (declare forth-eval)

     (defn pop! [stack]
       (let [first (first @stack)]
         (swap! stack pop)
         first))

     (defn push! [stack item]
       (swap! stack conj item))

     (defn next-token [stream]
       (if (. stream hasNextBigInteger)
         (. stream nextBigInteger)
         (. stream next)))

     (defn init-env []
       (let [stream (java.util.Scanner. System/in)
             stack (atom '())
             dict (atom {})
             prim (fn [id f] (swap! dict assoc id f))]
         (prim ".s" #(do (println "---")
                         (doseq [s @stack] (println s))
                         (println "---")))
         (prim "cr" #(println))
         (prim "+" #(push! stack (+ (pop! stack) (pop! stack))))
         (prim "*" #(push! stack (* (pop! stack) (pop! stack))))
         (prim "/" #(let [a (pop! stack)
                          b (pop! stack)]
                      (push! stack (/ b a))))
         (prim "-" #(let [a (pop! stack)
                          b (pop! stack)]
                      (push! stack (- b a))))
         (prim "dup" #(push! stack (first @stack)))
         (prim "." #(println (pop! stack)))
         (prim ":" #(let [name (next-token stream)
                          block (loop [b [] n (next-token stream)]
                                  (if (= n ";")
                                    b
                                    (recur (conj b n) (next-token stream))))]
                      (prim name (fn [] (doseq [w block]
                                          (forth-eval dict stack w))))))
         [dict stack stream]))

     (defn forth-eval [dict stack token]
       (cond (contains? @dict token) ((@dict token))
             (number? token) (push! stack token)
             :default (println token "??")))

     (defn repl [env]
       (let [[dict stack stream] env
             token (next-token stream)]
         (when (not= token "bye")
           (forth-eval dict stack token)
           (repl env))))

Forth has no explicit grammar which makes it extremely easy to parse, we
tokenize the input using whitespace as a delimiter, each token is then
sent to *forth-eval* which first checks, if the token is in the
dictionary if it is, corresponding function for the word is executed if
the token is not a word, we check if it is a valid number, if it is we
push it to the stack, if it is neither a word or a number an error
message is printed, this is repeated until the token *bye* is read in
which case we exit.

     forth=> (repl (init-env))
     5 6 + 7 8 + *
     .
     165
     cr

     3 2 1 + *
     . cr
     9

     : sq dup * ;
     2 sq
     .
     4
     bye
     nil
     forth=> 
