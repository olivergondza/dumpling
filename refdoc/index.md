---
layout: "default"
tag: "dumpling-0.6"
title: "Reference documentation for dumpling-0.6"


prefix: dumpling-0.6/
# used to generate version specific refdoc
---

# {{page.title}}

<a name="cliCommands">
## [CLI Commands]({{page.prefix}}cliCommands.html)

Predefined commands available from command line interface.

<a name="factories">
## [Runtime factories]({{page.prefix}}factories.html)

`ProcessRuntime` factory implementations, necessary starting point to any runtime inspection.

<a name="queries">
## [Predefined queries]({{page.prefix}}queries.html)

Predefined queries available from groovy. To be used with `ThreadSet.query` method:

```groovy
threads.query(new BlockingTree().showStackTraces())
```

Note that queries are often exposed in the form of CLI commands.

<a name="threadPredicates">
## [Thread predicates]({{page.prefix}}threadPredicates.html)

Predicates to filter threads available from groovy. To be used with `ThreadSet.where` method:

```groovy
threads.where(nameIs('a_thread'))
```
{% assign version = page.tag | remove_first : "dumpling-" %}
{% if version >= "0.7" %}
<a name="cliExports">
## [Groovy CLI exposed API]({{page.prefix}}cliExports.html)

API exposed in CLI Groovy scripts.
{% endif %}

<a name="apidocs">
## [Javadoc]({{page.prefix}}apidocs/)

Detailed javadoc of Dumpling.
