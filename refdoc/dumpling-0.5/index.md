---
layout: "default"
tag: "dumpling-0.5"
title: "Reference documentation for dumpling-0.5"
category: refdoc
prio: 5
prefix:
# used to generate version specific refdoc
---

# {{page.title}}

## [CLI Commands]({{page.prefix}}cliCommands.html)

Predefined commands available from command line interface.

## [Runtime factories]({{page.prefix}}factories.html)

`ProcessRuntime` factory implementations available from groovy.

## [Predefined queries]({{page.prefix}}queries.html)

Predefined queries avaialbe from groovy. To be used with `ThreadSet.query` method:

```groovy
threads.query(new BlockingTree().showStackTraces())
```

## [Thread predicates]({{page.prefix}}threadPredicates.html)

Predicates to filter threads available from groovy. To be used with `ThreadSet.where` method:

```groovy
threads.where(nameIs('a_thread'))
```

## [Javadoc]({{page.prefix}}apidocs/)

Detailed javadoc of Dumpling.
