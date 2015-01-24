---
title: Runtime factories
layout: default
---
[Reference documentation for dumpling-0.6](.)
# {{page.title}}
### [PidRuntimeFactory](./apidocs/com/github/olivergondza/dumpling/factory/PidRuntimeFactory.html)
Create ProcessRuntime from running local process.

 Process ID is used as a locator.

 This implementations invokes jstack binary and delegates to ThreadDumpFactory so it shares its features
 and limitations.
### [ThreadDumpFactory](./apidocs/com/github/olivergondza/dumpling/factory/ThreadDumpFactory.html)
Instantiate ProcessRuntime from threaddump produced by <tt>jstack</tt> or similar tool.
### [JmxRuntimeFactory](./apidocs/com/github/olivergondza/dumpling/factory/JmxRuntimeFactory.html)
Create runtime from running process via JMX interface.

 A process can be identified by process ID or by host and port combination.
### [JvmRuntimeFactory](./apidocs/com/github/olivergondza/dumpling/factory/JvmRuntimeFactory.html)
Create ProcessRuntime from state of current JVM process.
