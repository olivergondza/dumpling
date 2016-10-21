---
layout: default
title: Dumpling as CLI tool
category: toplevel
prio: 20
dumpling-sh-url: https://bit.ly/dumpling-sh
---

# {{page.title}}

## Getting dumpling CLI

Dumpling provides convenient wrapper called [`./dumpling.sh`]({{page.dumpling-sh-url}}) to access dumpling in a convenient way:

```bash
$ wget {{page.dumpling-sh-url}} -O dumpling.sh
$ chmod +x dumpling.sh
```

Then run `./dumpling.sh help [<command>]` to list available commands.

## Batch processing

When using Dumpling CLI, `--in` option is conventionally used to choose a *factory implementation* and a *locator* in order to create runtime to inspect. Run one of Dumpling predefined queries against runtime obtained from different sources.

```bash
$ ./dumpling.sh deadlocks --in process:4242
$ ./dumpling.sh blocking-tree --in jmx localhost:4242
```

Run custom groovy query:

```bash
$ ./dumpling.sh groovy --in threaddump:jstack-crash.log <<< "D.runtime.threads.grep { it.status.waiting }"
```

## `D` object

All groovy scripts run from cli (using `groovy` or `groovysh` command) have
[additional API](./refdoc/#cliExports) available through the `D` object. The
`--in` option is optional here and the runtime(s) can be create from the script itself.

Continue to [Dumpling DSL Tutorial](./tutorial.html) or [Dumpling reference documentation](./refdoc/).

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
