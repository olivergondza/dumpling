---
layout: default
title: Built-in Dumpling
category: toplevel
prio: 10
---

# {{page.title}}

## Setup

To make Dumpling part of your application and inspect threads from within, following dependency needs to be available:

{% assign repo = site.github.public_repositories | where: "name", site.github_repo | first %}
{{ repo.tags | size }}

```xml
<dependency>
  <groupId>com.github.olivergondza</groupId>
  <artifactId>dumpling</artifactId>
  <version>0.7</version>
</dependency>
```

Bits are released to Sonatype OSS reporsitory, make sure it is configured either in `pom.xml` or `settings.xml`:

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

Continue to [Dumpling DSL Tutorial](./tutorial.html) or [Dumpling reference documentation](./refdoc/).
