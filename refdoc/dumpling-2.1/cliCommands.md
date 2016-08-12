---
title: CLI Commands
layout: default
---
[Reference documentation for dumpling-2.1](.)

# {{page.title}}

## blocking-tree

<pre style='word-wrap: break-word; white-space: pre-wrap;'>
Print trees of blocking threads

Usage: ./dumpling.sh blocking-tree [--show-stack-traces] -i (--in) KIND:LOCATOR
 --show-stack-traces    : List stack traces of all threads involved (default:
                          false)
 -i (--in) KIND:LOCATOR : Input for process runtime

Output format:

Thread blockage is visualized using tree hierarchy. Non-blocked threads are the roots of tree hierarchies where parent-child relationship represents blocking-blocked situation. Leaves of such trees represents blocked but not blocking threads. Only either blocked or blocking threads are reported.

The example output contains headers of 2 threads where one is owning on monitor and the other is blocked trying to acquire it:

    "owning_thread" prio=10 tid=47759910742016 nid=24496
        "blocked_thread" prio=10 tid=47088345200640 nid=32297

Deadlock handling:

Deadlock chains are not part of reported trees, though deadlocked threads are considered as potential blocking roots. In such case only non-deadlocked threads are reported as blocked children. Full deadlock report is presented under blocking trees.

</pre>

## deadlocks

<pre style='word-wrap: break-word; white-space: pre-wrap;'>
Detect cycles of blocked threads

Usage: ./dumpling.sh deadlocks [--show-stack-traces] -i (--in) KIND:LOCATOR
 --show-stack-traces    : List stack traces of all threads involved (default:
                          false)
 -i (--in) KIND:LOCATOR : Input for process runtime

Output format:

All detected deadlocks are reported separately. Threads in one deadlock are reported with all related locks.

    Deadlock #1:
    "ThreadA" daemon prio=10 tid=1481750528 nid=27336
        Waiting to <0x40dce6960> (a hudson.model.ListView)
        Acquired   <0x40dce0d68> (a hudson.plugins.nested_view.NestedView)
        Acquired   <0x49c5f7990> (a hudson.model.FreeStyleProject)
        Acquired * <0x404325338> (a hudson.model.Hudson)
    "ThreadB" daemon prio=10 tid=47091108077568 nid=17982
        Waiting to <0x404325338> (a hudson.model.Hudson)
        Acquired * <0x40dce6960> (a hudson.model.ListView)

Monitor thread is waiting to is listed first with "Waiting to" label, acquired monitor that block other thread's progress is highlighted using '*'.

</pre>

## grep

<pre style='word-wrap: break-word; white-space: pre-wrap;'>
Filter threads using groovy expression

Usage: ./dumpling.sh grep PREDICATE [-i (--in) KIND:LOCATOR] [-p (--porcelain)]
 PREDICATE              : Groovy expression used as a filtering criteria
 -i (--in) KIND:LOCATOR : Input for process runtime
 -p (--porcelain)       : Show in a format designed for machine consumption
                          (default: false)

Thread to be examined in the predicate is named 'thread'.

All Dumpling DSL classes and methods are imported. "D" property is exposed to access useful data/functions.

</pre>

## groovy

<pre style='word-wrap: break-word; white-space: pre-wrap;'>
Execute groovy script as a query

Usage: ./dumpling.sh groovy [SCRIPT_ARGS ...] [-e (--expression) VAL] [-i (--in) KIND:LOCATOR] [-p (--porcelain)] [-s (--script) VAL]
 SCRIPT_ARGS            : Arguments to be passed to the script
 -e (--expression) VAL  : Script expression to execute
 -i (--in) KIND:LOCATOR : Input for process runtime
 -p (--porcelain)       : Show in a format designed for machine consumption
                          (default: false)
 -s (--script) VAL      : Script file to execute

All Dumpling DSL classes and methods are imported. "D" property is exposed to access useful data/functions.

Script return value is used as a command exit value in case it is an Integer or Boolean.

</pre>

## groovysh

<pre style='word-wrap: break-word; white-space: pre-wrap;'>
Open Groovy shell to inspect runtime

Usage: ./dumpling.sh groovysh [SCRIPT_ARGS ...] [-i (--in) KIND:LOCATOR]
 SCRIPT_ARGS            : Arguments to be passed to the script
 -i (--in) KIND:LOCATOR : Input for process runtime

All Dumpling DSL classes and methods are imported. "D" property is exposed to access useful data/functions.
</pre>

## help

<pre style='word-wrap: break-word; white-space: pre-wrap;'>
Print dumpling usage

Usage: ./dumpling.sh help [COMMAND]
 COMMAND : Print detailed usage

</pre>

## threaddump

<pre style='word-wrap: break-word; white-space: pre-wrap;'>
Print runtime as string

Usage: ./dumpling.sh threaddump [-i (--in) KIND:LOCATOR] [-p (--porcelain)]
 -i (--in) KIND:LOCATOR : Input for process runtime
 -p (--porcelain)       : Show in a format designed for machine consumption
                          (default: false)

Allows to capture threaddump from factory such as jmx or process for later analysis.

</pre>

## top-contenders

<pre style='word-wrap: break-word; white-space: pre-wrap;'>
Detect top-contenders, threads that block largest number of other threads

Usage: ./dumpling.sh top-contenders [--show-stack-traces] -i (--in) KIND:LOCATOR
 --show-stack-traces    : List stack traces of all threads involved (default:
                          false)
 -i (--in) KIND:LOCATOR : Input for process runtime

List blocking threads and threads they block. The list is sorted by the number of blocked threads.

</pre>
