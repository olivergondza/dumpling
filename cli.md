---
layout: default
title: Dumpling as CLI tool
---

# {{page.title}}

Dumpling exposes its features in form of CLI from self-contained `dumpling.jar`. Use `mvn package` to build it and `java -jar ./target/dumpling.jar` to run. Alternatively, you can use `./dumpling.sh` as a convenient wrapper. Run `./dumpling.sh help [<command>]` to see what it offers.

Run one of Dumpling predefined queries against threaddump:

```bash
$ ./dumpling.sh deadlocks --in threaddump jstack-crash.log
```

When using Dumpling CLI, `--in` option is conventionally used to choose a *factory implementation* and a *locator* (yes, an option with 2 values).

In case of running queries exposed through CLI, the command itself chooses suitable output format for query result. For convenience, command output and `toString()` output of query result are the same.

Run custom groovy query:

```bash
$ ./dumpling.sh groovy --in threaddump jstack-crash.log <<< "runtime.threads.grep { it.threadStatus.waiting }"
```

For interactive investigation there is `groovysh` command to open the shell, load threaddumps and query its state as necessary.

```groovy
$ ./dumpling.sh groovysh
groovy:000> rt = load("jstack-crash.log")
===> com.github.olivergondza.dumpling.model.ProcessRuntime@6555694
groovy:000> rt.query(new Deadlocks())
===>

0 deadlocks detected

groovy:000>
```

`groovysh` command does not accept `--in` option. Instead, it provides `load` function to read any number of threaddumps once shell is started.

Note that capturing runtime from current JVM don not make sense when using
Dumpling from CLI.
