---
title: Using clojure-contrib Generic Interfaces
tags: clojure
---

Generic interfaces in clojure-contrib provides multimethods that you can
implement for your custom data structures, which allows you to define
various operations such as arithmetic, comparison etc. 


     (ns vector2d
       (:use [clojure.contrib.types :only (deftype)]
             [clojure.contrib.generic :only (root-type)])
       (:require [clojure.contrib.generic.arithmetic :as ga]))

     (defstruct vector2d-struct :x :y)

     (deftype ::vector2d vector2d
       (fn [x y] (struct vector2d-struct x y))
       (fn [v] (vals v)))

     (derive ::vector2d root-type)

     (defmethod ga/+ [::vector2d ::vector2d]
       [u v]
       (let [[ux uy] (vals u)
             [vx vy] (vals v)]
         (vector2d (ga/+ ux vx) (ga/+ uy vy))))

The only problem is when you want to use it, you need to tell Clojure to
not load the function from core but instead use multimethods from the
generic interface.

     (ns core
       (:refer-clojure :exclude [+])
       (:use (clojure.contrib.generic [arithmetic :only [+]]))
       (:refer vector2d))

     (+ (vector2d 2 3) (vector2d 2 3))

Generic interface also contains collection operations as multimethods
which allows you to implement operations such as assoc, conj, seq etc.

     (ns grid
       (:use [clojure.contrib.types :only (deftype)]
             [clojure.contrib.generic :only (root-type)])
       (:require [clojure.contrib.generic.collection :as gc]))

     (defstruct grid-struct :open :closed)

     (deftype ::grid grid
       (fn [o c] (struct grid-struct o c))
       (fn [g] (vals g)))

     (derive ::grid root-type)

     (defmethod gc/seq ::grid
       [g]
       (gc/seq (:open g)))

     (gc/seq (grid [1 2 3] [4 5 6]))
     ;;(1 2 3)
