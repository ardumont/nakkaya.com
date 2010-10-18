---
title: Java Native Access from Clojure
tags: clojure jna c
---

I tried to pick up
[JNI](http://en.wikipedia.org/wiki/Java_Native_Interface) multiple times
but in the end, I got bored. There is so much boiler plate code that you
have to write even for trivial things. A while ago I stumbled upon
a project called [JNA](https://jna.dev.java.net/) (Java Native Access),
it allows you to access [native shared
libraries](http://en.wikipedia.org/wiki/Shared_library#Shared_libraries)
from Java without using the Java Native Interface. I have been meaning
to play with it for a while, last night i had some free time, I thought
I give it a shot.

I have created two implementations, first one is the
[documented](http://en.wikipedia.org/wiki/Java_Native_Access) way of
calling native libraries, it works but it will present problems for
some functions, such as there is no way to create a method that accepts
variable number of arguments using gen-interface macro which is a big
problem for functions like printf, you have to know before hand how many
variables you will call it with. There is also the problem of structs, 

    // Original C code
    typedef struct _Point {
      int x, y;
    } Point;

In order to represent this struct, in Java one would use,

    // Equivalent JNA mapping
    class Point extends Structure { public int x, y; }

which can't be done in Clojure, at first I thought I was stuck, but
turns out there are workarounds.

First, documented way of calling printf,

    (gen-interface
     :name jna.CLibrary
     :extends [com.sun.jna.Library]
     :methods [[printf [String] void]])

We create a interface that extends com.sun.jna.Library (use full package
name even if you import it!!), and define which methods we will be
calling. You need to compile this before hand. Now you can call printf,

    (def glibc (Native/loadLibrary "c" jna.CLibrary))
    (.printf glibc "Hello, World.. \n")

Obvious problem here, is that this will only work for simple functions,
pretty much all functions that does something interesting, will expect
some sort of structure as a parameter which we can not emulate in
Clojure.
 
While digging through the documentation, I found the
[Function](https://jna.dev.java.net/javadoc/com/sun/jna/Function.html)
class which allows you to make calls without creating an interface, with
it we can now pass variables as an array which allows us to call printf
with variable length arguments.

    (defmacro jna-call [lib func ret & args] 
      `(let [library#  (name ~lib)
             function# (com.sun.jna.Function/getFunction library# ~func)] 
         (.invoke function# ~ret (to-array [~@args]))))

With a simple macro we can now make any native call we want,

    (jna-call :c "printf" Integer "kjhkjh")

    ;Some POSIX Calls
    (jna-call :c "mkdir" Integer "/tmp/jnatesttemp" 07777)
    (jna-call :c "rename" Integer "/tmp/jnatesttemp" "/tmp/jnatesttempas")
    (jna-call :c "rmdir" Integer "/tmp/jnatesttempas")


Armed with this macro, I thought I can solve the age old Java question,
How to find the free space available on the disk? (Pre 1.6). This is
where I hit the second wall, the call to get free space on my Mac OS X
is,
[statvfs](http://developer.apple.com/mac/library/documentation/Darwin/Reference/ManPages/man3/statvfs.3.html) 
which expects a string pointing to the directory and a struct that it
will fill the information for us, a struct which we can not emulate in
Clojure. Couple more hours of google fun, it turns out that this can also
be worked around. You can request a Pointer object from JNA which you
can pass to functions,

    (defmacro jna-malloc [size] 
      `(let [buffer# (java.nio.ByteBuffer/allocateDirect ~size)
             pointer# (Native/getDirectBufferPointer buffer#)]
         (.order buffer# java.nio.ByteOrder/LITTLE_ENDIAN)
         {:pointer pointer# :buffer buffer#}))

You give JNA a ByteBuffer it will give you a pointer, you can pass this
Pointer around instead of a Structure. 

    (let [struct (jna-malloc 44)] 
      (jna-call :c "statvfs" Integer "/git" (:pointer struct))
      (let [fbsize (.getInt (:buffer struct))
            frsize (.getInt (:buffer struct) 4)
            blocks (.getInt (:buffer struct) 8)
            bfree (.getInt (:buffer struct) 12)
            bavail (.getInt (:buffer struct) 16)]
    
        (println "f_fbsize" fbsize)
        (println "f_frsize" frsize)
        (println "blocks" blocks)
        (println "bfree" bfree)  
        (println "bavail" bavail)))

Now we can just do the math and get free space. C equivalent would be,

    #include <stdio.h>
    #include <string.h>
    #include <sys/statvfs.h>

    int main( int argc, char *argv[] ){

      struct statvfs fiData;
      char fnPath[128];

      strcpy(fnPath, argv[1]);

      statvfs(fnPath,&fiData);

      printf("Disk %s: \n", fnPath);
      printf("\tf_bsize: %u\n", fiData.f_bsize);
      printf("\tf_frsize: %i\n", fiData.f_frsize);
      printf("\tf_blocks: %i\n", fiData.f_blocks);
      printf("\tf_bfree: %i\n", fiData.f_bfree);
      printf("\tf_bavail: %i\n", fiData.f_bavail);
    }

C output,

    $ gcc spc.c && ./a.out /git
    Disk /git: 
            f_bsize: 1048576
            f_frsize: 4096
            f_blocks: 60965668
            f_bfree: 33754724
            f_bavail: 33690724

Clojure output,

    jna=> f_fbsize 1048576
    f_frsize 4096
    blocks 60965668
    bfree 33754724
    bavail 33690724
    nil

I have picked up a few tips from this experiment, get a very simple
C/C++ program going, you need to know the sizes of different types and
structures, you are still playing with C so be prepared to play with
bytes to get/send the information you need.

Overall this is a very good weapon to add to your arsenal, when you need
some functionality which Java does not support. JNA is much slower than
JNI, so this is not useful to speed things up. C is C so you will have
crashes and complex function signatures will drive you nuts.

#### Resources

 - [Java Native Access](http://en.wikipedia.org/wiki/Java_Native_Access)
   on Wikipedia
 - [JNA API
   Documentation](https://jna.dev.java.net/javadoc/overview-summary.html)
 - [STATVFS(3)](http://developer.apple.com/mac/library/documentation/Darwin/Reference/ManPages/man3/statvfs.3.html)
