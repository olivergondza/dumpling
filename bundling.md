---
layout: default
title: Built-in Dumpling
category: toplevel
prio: 10
---

# {{page.title}}

## Setup

To make Dumpling part of your application and inspect threads from within, following dependency needs to be available:

```xml
<dependency>
  <groupId>com.github.olivergondza</groupId>
  <artifactId>dumpling</artifactId>
  <version>2.0</version>
</dependency>
```

## Usage

To access thread state, start instantiating `ProcessRuntime` using one of [factory implementations](./refdoc/#factories):

```java
// Create runtime from threaddump
new ThreadDumpFactory().fromFile(new File("jstack-crash.log"));
// Create runtime from host JVM
new JvmRuntimeFactory().currentRuntime();
```

Continue to [Dumpling DSL Tutorial](./tutorial.html) or [Dumpling reference documentation](./refdoc/).
