---
layout: default
title: Dumpling DSL tutorial
prio: 30
---
# {{page.title}}

Dumpling model objects have convenient `toString()` implementations that mimics
jstack output. Note that this output is not supposed to be compatible with any
format nor read by any tool. It aims for presenting as much information as
possible in a convenient way while still looking familiar to `jstack` users.

Central abstraction in dumpling is `ThreadSet`, a subset of all threads in JVM
runtime. `ProcessRuntime.getThreads()` provides access to all threads that can be
further filtered. `ThreadSet.where(ProcessThread.Predicate)` can narrow the set
using either one of predefined predicates or Java 8 lambda expression.

```java
runtime.getThreads().where(nameIs("MyThread"))
// or since Java 8
runtime.getThreads().where(it -> "main".equals(it.getName()))
````

`ThreadSet` also overrides groovy collection methods so Dumpling DSL and known
groovy methods can by used side by side.

```groovy
// Find RUNNABLE ajp threads
runtime.threads.grep { it.threadStatus == ThreadStatus.RUNNABLE }.where(nameContains(~/ajp-.*/))
```

## Predefined queries

Predefined queries can be run against `ThreadSet` or whole runtime simply:

```groovy
runtime.query(new BlockingTree().showStackTraces())
```

Query transforms thread set into a result object that usually represent the outcome
as in terms of known Dumpling abstractions.
