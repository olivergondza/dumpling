---
title: Groovy CLI exposed API
layout: default
---
[Reference documentation for dumpling-2.1](.)

# {{page.title}}

```
D.args: java.util.List
  CLI arguments passed to the script

D.load.process(int): com.github.olivergondza.dumpling.model.ProcessRuntime
       Load runtime from process identified by PID.

D.load.threaddump(String): com.github.olivergondza.dumpling.model.ProcessRuntime
       Load runtime from threaddump.

D.load.jmx(String): com.github.olivergondza.dumpling.model.ProcessRuntime
       Load runtime via JMX using JMX connection string.

D.load.jmx(int): com.github.olivergondza.dumpling.model.ProcessRuntime
       Load runtime via JMX from process identified by PID.

D.load.jvm: com.github.olivergondza.dumpling.model.ProcessRuntime
       Capture runtime of current JVM.

D.runtime: com.github.olivergondza.dumpling.model.ProcessRuntime
  Current runtime passed via `--in` option. null if not provided.



```
