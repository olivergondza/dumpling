---
layout: "default"
tag: "dumpling-0.1"
title: "Reference documentation for dumpling-0.1"
category: "refdoc"
prio: 1
# used to generate version specific refdoc
---

# {{page.title}}

## [CLI Commands](cliCommands.html)

Predefined commands available from command line interface.

## [Runtime factories](factories.html)

`ProcessRuntime` factory implementations available from groovy.

## [Predefined queries](queries.html)

Predefined queries avaialbe from groovy. To be used with `ThreadSet.query` method:

```groovy
threads.query(new BlockingTree().showStackTraces())
```

## [Thread predicates](threadPredicates.html)

Predicates to filter threads available from groovy. To be used with `ThreadSet.where` method:

```groovy
threads.where(nameIs('a_thread'))
```

## [Javadoc](apidocs/)

Detailed javadoc of Dumpling.
