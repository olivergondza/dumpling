---
title: CLI Commands
layout: default
---
[Reference documentation for dumpling-0.1](.)
# {{page.title}}
### [`deadlocks`](./apidocs/com/github/olivergondza/dumpling/query/Deadlocks.Command.html)

<pre style='word-wrap: break-word'>
Detect cycles of blocked threads

Usage: ./dumpling.sh deadlocks [--show-stack-traces] -i (--in) KIND LOCATOR
 --show-stack-traces    : List stack traces of all threads involved
 -i (--in) KIND LOCATOR : Input for process runtime

Print sets of threads that form deadlocks.

</pre>
### [`top-contenders`](./apidocs/com/github/olivergondza/dumpling/query/TopContenders.Command.html)

<pre style='word-wrap: break-word'>
Detect top-contenders, threads that block largest number of other threads

Usage: ./dumpling.sh top-contenders [--show-stack-traces] -i (--in) KIND LOCATOR
 --show-stack-traces    : List stack traces of all threads involved
 -i (--in) KIND LOCATOR : Input for process runtime

List blocking threads and threads they block. The list is sorted by the number of blocked threads.

</pre>
### [`blocking-tree`](./apidocs/com/github/olivergondza/dumpling/query/BlockingTree.Command.html)

<pre style='word-wrap: break-word'>
Print trees of blocking threads

Usage: ./dumpling.sh blocking-tree [--show-stack-traces] -i (--in) KIND LOCATOR
 --show-stack-traces    : List stack traces of all threads involved
 -i (--in) KIND LOCATOR : Input for process runtime

Visualize blocked threads using tree hierarchy. Non-blocked threads are the roots of tree hierarchies where parent-child relationship represents blocking-blocked situation. Leaves of such trees represents blocked but not blocking threads. Only either blocked or blocking threads are reported.

The example output contains headers of 2 threads where one is owning on monitor and the other is blocked trying to acquire it:

    "owning_thread" prio=10 tid=47759910742016 nid=24496
        "blocked_thread" prio=10 tid=47088345200640 nid=32297

</pre>
### [`groovy`](./apidocs/com/github/olivergondza/dumpling/cli/GroovyCommand.html)

<pre style='word-wrap: break-word'>
Execute groovy script as a query

Usage: ./dumpling.sh groovy -i (--in) KIND LOCATOR
 -i (--in) KIND LOCATOR : Input for process runtime

Execute groovy script provided on stdin against runtime. All Dumpling DSL classes and methods are imported. Available variables:

  runtime: ProcessRuntime instance provided using --in option
  out: PrintStream for stdout
  err: PrintStream for stderr

Script return value is used as a command exit value in case it is an Integer or Boolean.

</pre>
### [`groovysh`](./apidocs/com/github/olivergondza/dumpling/cli/GroovyshCommand.html)

<pre style='word-wrap: break-word'>
Open Groovy shell to inspect runtime

Usage: ./dumpling.sh groovysh

There are several commands predefined:

  load(String): Load runtime threaddump from file.

</pre>
### [`help`](./apidocs/com/github/olivergondza/dumpling/cli/HelpCommand.html)

<pre style='word-wrap: break-word'>
Print dumpling usage

Usage: ./dumpling.sh help [COMMAND]
 COMMAND : Print detailed usage

</pre>
