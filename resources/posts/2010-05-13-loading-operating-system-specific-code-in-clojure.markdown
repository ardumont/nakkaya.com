---
title: Loading Operating System Specific Code in Clojure
tags: clojure apple linux
---

Here is a little trick that took a little while to come up with, let me
begin by stating the problem, even though Java applications does run on
multiple platforms without modification, applications will still feel
foreign if you don't take care of the little things such as key
bindings, events etc... Since I am primarily on Mac OS X, I want my
applications to blend in as much as possible, for that Apple provides
[Mac OS Runtime for
Java](http://en.wikipedia.org/wiki/Mac_OS_Runtime_for_Java) which allows
your Java application to receive various Mac OS X events, but this can
not be used like any other Java class since it will not be available on
operating systems other than Mac OS X, using it will cause the
application to crash. The trick is, collecting OS specific code into
their respective modules following snippet is from my applications
mac-adapter module, 

     (ns tubes.mac-adapter
       (:use clojure.contrib.with-ns))

     (with-ns 'tubes.core
       (defn install-adapter [frame list]
         (tubes.mac-adapter/mac-application-adapter frame)
         (tubes.mac-adapter/mac-keybindings frame list)))

Each module will define a common function in this case install-adapter,
but not in the adapters namespace but in the namespace where we will
call it from, in this case tubes.core,

     (ns tubes.core)

     (if-not (nil? (System/getProperty "mrj.version"))
       (load "/tubes/mac_adapter")
       (load "/tubes/uni_adapter"))

Now depending on the OS we can load the appropriate adapter, this allows
us to only instantiate Mac specific classes on a Mac and Linux specific
classes on a Linux box, since each adapter in turn defines a common
function install-adapter in our current namespace, we can call it
without worrying about which OS we are on,

     (install-adapter frame list)
