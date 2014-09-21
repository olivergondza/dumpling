---
layout: default
title: Built-in Dumpling
---

# {{page.title}}

## Setup

To make dumpling part of your application and inspect threads from within, following dependency needs to be available:

```xml
<dependency>
  <groupId>com.github.olivergondza</groupId>
  <artifactId>dumpling</artifactId>
</dependency>
```

Bits are released to Sonatype OSS reporsitory, make sure it is configured either in pom.xml or settings.xml:

```xml
<repositories>
  <repository>
    <id>ossrh</id>
    <url>https://oss.sonatype.org/content/repositories/releases</url>
  </repository>
</repositories>
```

## Usage

To access thread state, start instantiating `ProcessRuntime` using one of factory implementation:

```java
// Create runtime from threaddump
new ThreadDumpFactory().fromFile(new File("jstack-crash.log"));
// Create runtime from host JVM
new JvmRuntimeFactory().currentRuntime();
```

Continue to [Dumpling DSL Tutorial](./tutorial.html).
