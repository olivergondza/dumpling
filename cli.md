---
layout: default
title: Dumpling as CLI tool
category: toplevel
prio: 20
dumpling-sh-url: https://raw.githubusercontent.com/olivergondza/dumpling/master/dumpling.sh
---

# {{page.title}}

## Getting dumpling CLI

Dumpling provides convenient wrapper called [`./dumpling.sh`]({{page.dumpling-sh-url}}) to access dumpling in a convenient way:

```bash
$ wget {{page.dumpling-sh-url}}
$ chmod +x dumpling.sh
```

Then run `./dumpling.sh help [<command>]` to list available commands.

## Batch processing

When using Dumpling CLI, `--in` option is conventionally used to choose a *factory implementation* and a *locator* (yes, an option with 2 values). Run one of Dumpling predefined queries against threaddump.

```bash
$ ./dumpling.sh deadlocks --in process 4242
$ ./dumpling.sh blocking-tree --in jmx localhost:4242
```

Run custom groovy query:

```bash
$ ./dumpling.sh groovy --in threaddump jstack-crash.log <<< "D.runtime.threads.grep { it.status.waiting }"
```

## Interactive investigation

For interactive investigation there is `groovysh` command to open the shell, load threaddumps and query its state as necessary.

```groovy
$ ./dumpling.sh groovysh
groovy:000> rt = D.load.threaddump("jstack-crash.log")
===> com.github.olivergondza.dumpling.model.ProcessRuntime@6555694
groovy:000> rt.query(new Deadlocks())
===>

0 deadlocks detected

groovy:000>
```

`groovysh` command does not accept `--in` option. Instead, it provides `load` function to read any number of threaddumps once shell is started.

Note that capturing runtime from current JVM don not make sense when using
Dumpling from CLI.

Continue to [Dumpling DSL Tutorial](./tutorial.html) or [Dumpling reference documentation](./refdoc/).
