---
layout: default
title: Built-in Dumpling
---

# {{page.title}}

To make dumpling part of your application and inspect threads from within, following dependency needs to be available. Put following to `pom.xml` to have Dumpling API available:

```xml
<dependency>
  <groupId>com.github.olivergondza</groupId>
  <artifactId>dumpling</artifactId>
</dependency>
```

```xml
<repositories>
  <repository>
    <id>ossrh</id>
    <url>https://oss.sonatype.org/content/repositories/releases</url>
  </repository>
</repositories>
```

Start with instantiating `ProcessRuntime` using one of factory implementation:

```java
// Create runtime from threaddump
new ThreadDumpFactory().fromFile(new File("jstack-crash.log"));
// Create model from host JVM
new JvmRuntimeFactory().currentRuntime();
```
