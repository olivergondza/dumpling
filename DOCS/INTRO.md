# Dumpling DSL tutorial

## Get runtime

`ProcessRuntime` is Dumpling abstraction to hold all threads running in JVM at
the time it was captured. It is the single entry point to the domain model.

There are several factory implementations to create it. Currently, there are two ways
to obtain the runtime: parsing treaddump or capturing runtime of the current process.
Let's use [oraclejdk-1.7.0_51.log](https://github.com/olivergondza/dumpling/blob/master/src/test/resources/com/github/olivergondza/dumpling/factory/ThreadDumpFactoryTest/oraclejdk-1.7.0_51.log)
as nontrivial threaddump example.

    // Create runtime from threaddump
    new ThreadDumpFactory().fromFile(new File("oraclejdk-1.7.0_51.log"));
    // Create model from host JVM
    new JvmRuntimeFactory().currentRuntime();

When using Dumpling CLI `--in` options is conventionally used to choose a *factory
implementation* and a *locator* (yes, an option with 2 values). To create runtime
from threaddump file use:

    java -jar dumpling.jar <COMMAND> --in threaddump oraclejdk-1.7.0_51.log

Note that capturing runtime from current JVM don not make sense when using
Dumpling from CLI.

## Run predefined queries

Predefined queries can be run against `ProcessRuntime` or `ThreadSet` (arbitrary
subset of process threads) to deliver declared typed result value.

    Map<ProcessThread, ThreadSet> contenders = runtime.query(new TopContenders());

In case of running queries exposed through CLI, the command itself chooses suitable
output format for query result.

    java -jar dumpling.jar top-contenders --in threaddump oraclejdk-1.7.0_51.log

## Run custom queries

Dumpling model objects have convenient `toString()` implementations that mimics
jstack output. Note that this output is not supposed to be compatible with any
format nor read by any tool. It aims for presenting as much information as
possible in a convenient way while still looking familiar to jstack users.
Following examples uses groovy for brevity:

    // List all threads in runtime
    println runtime.getThreads()

    // Count threads
    println runtime.getThreads().size()

    // List thread names
    runtime.threads.each { println it.name }

    // Filter threads using custom criteria
    runtime.threads.grep { it.state == Thread.State.RUNNABLE }
