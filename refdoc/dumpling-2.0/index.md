---
layout: "default"
tag: "dumpling-2.0"
title: "Reference documentation for dumpling-2.0"
category: refdoc
prio: 2
prefix:
# used to generate version specific refdoc
---

# {{page.title}}

<a name="cliCommands">

## [CLI Commands]({{page.prefix}}cliCommands.html)

Predefined commands available from command line interface.

<a name="factories">

## [Runtime factories]({{page.prefix}}factories.html)

`ProcessRuntime` factory implementations, necessary starting point to any runtime inspection. To use from java/groovy code consult the [javadoc](./apidocs/com/github/olivergondza/dumpling/factory/package-summary.html). For the purposes of CLI client, each factory have a `kind` identifier attached to be used as `--in process:42` where `process` is the kind.

<a name="cliExports">

## [Groovy CLI exposed API]({{page.prefix}}cliExports.html)

API exposed in CLI Groovy scripts for easier access to Dumpling DSL.

<a name="queries">

## [Predefined queries]({{page.prefix}}queries.html)

Predefined queries available from [Dumpling DSL](./apidocs/com/github/olivergondza/dumpling/query/package-summary.html) to be used by `ThreadSet.query` method:

```java
threads.query(new BlockingTree().showStackTraces())
```

Note that queries are exposed in the form of [CLI commands](cliCommands.html).

<a name="threadPredicates">

## [Thread predicates]({{page.prefix}}threadPredicates.html)

Predicates to filter threads available from Java code to be used with `ThreadSet.where` method:

```java
threads.where(nameIs("a_thread"))
```

Node that `ThreadSet` implements convenient methods known from Groovy collections so the same operation can be written in native Groovy:

```groovy
threads.grep { it.name == "a_thread" }
```

<a name="apidocs">

## [Javadoc]({{page.prefix}}apidocs/)

Detailed javadoc of Dumpling DSL.
