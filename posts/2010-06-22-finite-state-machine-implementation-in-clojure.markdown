---
title: Finite State Machine Implementation in Clojure
tags: clojure
---

A finite state machine is set of states, one being the start state, each
state has a list of transitions, transitions in turn has conditions and
actions whenever a condition for a transition is met FSM performs the
action and enters the new state. FSM are widely used for game and
robotic AI. Most games are just a bunch of FSMs running little chunks of
code reacting to state changes. An NPC for example when instantiated can
be in patrol state and as soon as the player approaches, it might cause
it to transition into attack state which might cause it to run towards
you.

     (defn state-machine [transition-table initial-state]
       (ref initial-state :meta transition-table))

A state machine is a ref holding the current state, transition table
containing the list of states and transition rules are attached as meta
data.

     (def traffic-light
          {:green [{:conditions [] :transition :yellow}]
           :yellow  [{:conditions [] :transition :red}]
           :red [{:conditions [] :transition :green}]})

Transition table is represented as a map containing states as keys and
vector of maps containing condition, action and transition information.

     (defn- switch-state? [conds]
       (if (empty? conds)
         true
         (not (some false? (reduce #(conj %1 (if (fn? %2) (%2) %2)) [] conds)))))

     (defn- first-valid-transition [ts]
       (find-first #(= (second %) true)
                   (map #(let [{conds :conditions 
                                transition :transition
                                on-success :on-success} %]
                           [transition (switch-state? conds) on-success]) ts)))

     (defn update-state [state]
       (let [transition-list ((meta state) @state)
             [transition _ on-success] (first-valid-transition transition-list)]
         (if-not (nil? transition)
           (do 
             (if-not (nil? on-success)
               (on-success))
             (dosync (ref-set state transition))))))

Every time we try to update state machines state, first we get the list
of transition rules for the current state, then we start checking
conditions for transition in the order they appear in the vector first
transition that returns true for all its conditions is picked, if it has
a on-success function it will be executed and reference will be set to
the new state.

     (let [sm (state-machine traffic-light :green)] 
       (dotimes [_ 4]
         (println @sm)
         (update-state sm)))

     state-machine.core=> :green
     :yellow
     :red
     :green

Above example shows how traffic light state machine iterates through its
states. A more complicated and famous example is a *find-lisp* state
machine that would search for the word *lisp* in a character sequence,

     (defn find-lisp [char-seq]
       (let [start-trans {:conditions []
                          :on-success #(pop-char char-seq)
                          :transition :start}
             found-l-trans {:conditions [#(= (first @char-seq) \l)] 
                            :on-success #(pop-char char-seq)
                            :transition :found-l}]

         {:start [found-l-trans
                  start-trans]

          :found-l [found-l-trans
                    {:conditions [#(= (first @char-seq) \i)] 
                     :on-success #(pop-char char-seq)
                     :transition :found-i}
                    start-trans]

          :found-i [found-l-trans
                    {:conditions [#(= (first @char-seq) \s)] 
                     :on-success #(pop-char char-seq)
                     :transition :found-s}
                    start-trans]

          :found-s [found-l-trans
                    {:conditions [#(= (first @char-seq) \p)] 
                     :on-success #(do (println "Found Lisp")
                                      (pop-char char-seq))
                     :transition :start}
                    start-trans]}))

When we run it, it will print *Found Lisp* every time we find the
sequence of characters l,i,s,p in this particular order,

     (let [char-seq (ref "ablislasllllispsslis")
           sm (state-machine (find-lisp char-seq) :start)] 
       (dotimes [_ (count @char-seq)]
         (update-state sm)))

     state-machine.core=> Found Lisp

Even though it is not designed for this but
[Vijual](http://lisperati.com/vijual/) works for quick and dirty
visualization of state machines,

       (use 'vijual)
       (do (println )
           (draw-graph (prepare-nodes (state-machine traffic-light :start))))

     state-machine.core=> 
     +--------+   +-------+
     |        |   |       |
     | yellow |---|       |
     |        |   | green |
     +--------+   |       |
       |          |       |
       |          +-------+
       |            |
       |   +--------+
       |   |
     +-----+
     | red |
     +-----+

[state-machine.clj](/code/clojure/state-machine.clj)
