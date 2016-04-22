---
title: Runtime factories
layout: default
---
[Reference documentation for dumpling-2.0](.)
# {{page.title}}
### [threaddump](./apidocs/com/github/olivergondza/dumpling/cli/Factories$ThreadDump.html)
Parse threaddrump from file, or standard input when '-' provided as a locator.
### [process](./apidocs/com/github/olivergondza/dumpling/cli/Factories$Pid.html)
Create runtime from running process identified by PID.
### [jmx](./apidocs/com/github/olivergondza/dumpling/cli/Factories$Jmx.html)
Create runtime from JMX process identified by PID or HOST:PORT combination. Credentials can be provided as USER:PASSWORD@HOST:PORT.
