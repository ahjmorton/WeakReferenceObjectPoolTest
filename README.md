# Object pool with dropped reference support and correction

## The disclaimer
Consider this an online form of note taking I am engaging in for my own benefit with the slighest chance of benefit to others. I knocked the project out over a couple of hours just as an experiment that I am documenting the results / conclusions of here.

## The idea
The original idea was born out of a desire to get to grips with Java's [ref package](http://docs.oracle.com/javase/1.5.0/docs/api/java/lang/ref/package-summary.html). 

The idea is to create an object pool with the following characteristics :

* Bounded to a user specified capacity 
+ Thread safe with blocking access if an object is not available
+ Allow user specified creation / renewable of objects
+ The ability to detect when a user has taken a reference out but not returned it to the pool then let it go out of scope.

The last bullet point help prevents accidental (or intentional) staving of the pool due to threads not returning objects back to the pool. Essentially it is protection against the users of the object pool being incompetent (although if you feel that protection is necessary then you have a deeper set of problems ;).

## The get-out disclaimer
** This approach used as-is should never be used in any production system, in any programming language ever.**

The primary reason for it's creation was to test a new idea as well as to get myself acquainted with Java's special reference classes.

## Running the code
1. Download it
2. Get Eclipse Juno
3. Add it to the Juno workspace
4. Run Test
5. Read output
6. If not bored, modify Test and goto 4

## Documentation of the approach
Currently the approach is that the `ObjectPool` class maintains a `WeakReference` to any object created in the pool with the reference queue set to a member of the `ObjectPool`. On calling the `init()` method of the `ObjectPool` a separate thread is spawned to poll the queue then re-create any instances that the garbage collector has said are gone.

## What is good about the approach
*It works*, you can have a number of threads randomly dropping objects and the pool will merrily keep on going like *magic*. 

## What sucks about the approach
We're using the garbage collector essentially as a callback mechanism for object deletion we need to force it to run more frequently than usual, this is done by spawning a thread to spam `System.gc()`. This has numerous problems ranging from decreased performance through to the pool just not working based on your JVM and GC settings. 
Currently the Java provides no way to bring an object out the jaws of the garbage collector; [soft references](http://docs.oracle.com/javase/6/docs/api/java/lang/ref/package-summary.html#reachability) provide something close where you an Object can still be reached despite going out of scope but with no callback mechanism it is not suitable for this purpose.

## Summing up
As stated this approach does suck because it *should* suck when using an automatic garbage collected language like Java. The ultimate goal of automatic garbage collection is to make life easier for the programmer by removing memory management, what a step back it would be if Oracle put in some mechanism to allow you effectively prevent garbage collection of certain objects? At that point you don't know if the object you're interacting with is spawning a bunch of objects that can never ever be garbage collected.

If Java had this facility you would see a lot of *magic* APIs like this one that are hard to reason about, scary to use, require a lot of documentation and break from JVM to JVM.