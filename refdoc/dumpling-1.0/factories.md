---
title: Runtime factories
layout: default
---
[Reference documentation for dumpling-1.0](.)
# {{page.title}}
### [process](./apidocs/com/github/olivergondza/dumpling/factory/PidRuntimeFactory.html)
Create runtime from running process identified by PID.
### [jmx](./apidocs/com/github/olivergondza/dumpling/factory/JmxRuntimeFactory.html)
Create runtime from JMX process identified by PID or HOST:PORT combination. Credentials can be provided as USER:PASSWORD@HOST:PORT.
### [threaddump](./apidocs/com/github/olivergondza/dumpling/factory/ThreadDumpFactory.html)
Parse threaddrump from file, or standard input when '-' provided as a locator.
