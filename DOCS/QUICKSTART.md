# Dumpling Quickstart

## Get runtime

`ProcessRuntime` is Dumpling abstraction to hold all threads running in JVM at
the time it was captured. It is the single entry point to the domain model.

There are several factory implementations to create it. Currently, there are two ways
to obtain the runtime: parsing treaddump or capturing runtime of the current process.
Let's use [oraclejdk-1.7.0_51.log](https://github.com/olivergondza/dumpling/blob/master/src/test/resources/com/github/olivergondza/dumpling/factory/ThreadDumpFactoryTest/oraclejdk-1.7.0_51.log)
as nontrivial threaddump example.

```java
// Create runtime from threaddump
new ThreadDumpFactory().fromFile(new File("oraclejdk-1.7.0_51.log"));
// Create model from host JVM
new JvmRuntimeFactory().currentRuntime();
```

When using Dumpling CLI `--in` options is conventionally used to choose a *factory
implementation* and a *locator* (yes, an option with 2 values). To create runtime
from threaddump file use:

```bash
java -jar dumpling.jar <COMMAND> --in threaddump oraclejdk-1.7.0_51.log
```

Note that capturing runtime from current JVM don not make sense when using
Dumpling from CLI.

`groovysh` command does not accept any threaddump on commandline. Instead, it
provides `load` function to read any number of threaddumps.

```groovy
rt = load("oraclejdk-1.7.0_51.log")
===> com.github.olivergondza.dumpling.model.ProcessRuntime@10745d92
rt.threads.grep { it.threadStatus == ThreadStatus.RUNNABLE }
```

## Run predefined queries

Predefined queries can be run against `ProcessRuntime` or `ThreadSet` (arbitrary
subset of process threads) to deliver declared typed result value.

```java
TopContenders.Result contenders = runtime.query(new TopContenders());
```

In case of running queries exposed through CLI, the command itself chooses suitable
output format for query result.

```bash
java -jar dumpling.jar blocking-tree --in threaddump oraclejdk-1.7.0_51.log
```

## Run custom queries

Custom queries can either be run from java when dumpling is bundled or from console
using build-in groovy interpreter.

`groovy` command can be used to invoke custom query:
```
./dumpling.sh groovy --in threaddump jstack-crash.log <<< "print runtime.threads.grep { it.threadStatus == ThreadStatus.RUNNABLE }"
```

For interactive investigation there is `groovysh` command to open the shell, load
threaddumps and query its state as necessary.

```
$ ./dumpling.sh groovysh
groovy:000> rt = load("oraclejdk-1.7.0_51.log")
===> com.github.olivergondza.dumpling.model.ProcessRuntime@6555694
groovy:000> rt.query(new Deadlocks())
===>

0 deadlocks detected

groovy:000>
```
