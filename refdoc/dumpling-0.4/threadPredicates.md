---
title: Thread predicates
layout: default
---
[Reference documentation for dumpling-0.4](.)
# {{page.title}}
### [nameIs](./apidocs/com/github/olivergondza/dumpling/model/ProcessThread.html#nameIs(java.lang.String))
Match thread by name.
### [nameContains](./apidocs/com/github/olivergondza/dumpling/model/ProcessThread.html#nameContains(java.util.regex.Pattern))
Match thread its name contains pattern.
### [nameContains](./apidocs/com/github/olivergondza/dumpling/model/ProcessThread.html#nameContains(java.lang.String))
Match thread its name contains string.
### [waitingOnLock](./apidocs/com/github/olivergondza/dumpling/model/ProcessThread.html#waitingOnLock(java.lang.String))

### [waitingToLock](./apidocs/com/github/olivergondza/dumpling/model/ProcessThread.html#waitingToLock(java.lang.String))
Match thread that is waiting on lock identified by <tt>className</tt>.
### [acquiredLock](./apidocs/com/github/olivergondza/dumpling/model/ProcessThread.html#acquiredLock(java.lang.String))
Match thread that has acquired lock identified by <tt>className</tt>.
