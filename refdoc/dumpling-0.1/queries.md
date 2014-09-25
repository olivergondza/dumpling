---
title: Queries
layout: default
---
# {{page.title}}
### [TopContenders](./apidocs/com/github/olivergondza/dumpling/query/TopContenders.html)
Get threads that block other threads.

 Mapping between blocking thread and a set of blocked threads.
 Map is sorted by the number of blocked threads.
### [Deadlocks](./apidocs/com/github/olivergondza/dumpling/query/Deadlocks.html)

### [BlockingTree](./apidocs/com/github/olivergondza/dumpling/query/BlockingTree.html)
Get a forest of all blocking trees.

 Non-blocked threads are the roots of tree hierarchies where parent-child
 relationship represents blocking-blocked situation. Leaves of such trees
 represents blocked but not blocking threads.

 Only such branches are included that contain threads from initial set.
 Provide all threads in runtime to analyze all threads.
