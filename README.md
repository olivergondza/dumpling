# Dumpling

Dumpling is an object model and domain specific language to represent and query the state of
process threads and their lock based dependencies.

Dumpling is shipped with a collection of predefined queries to cover most common
situations as well as several factories to create thread model from various sources.

## Usage

### Bundle dumpling to your application

Make dumpling part of your application and inspect threads from within.

- Include following maven dependency
```xml
<dependency>
  <groupId>com.github.olivergondza.dumpling</groupId>
  <artifactId>dumpling</artifactId>
  <version>...</version>
</dependency>
```

- Instantiate runtime of current JVM
```java
// JvmRuntimeFactory creates model from host JVM
ProcessRuntime runtime = new JvmRuntimeFactory().currentRuntime();
```

- Get the work done

### Invoking dumpling from console

```bash
$ ./dumpling.sh help [<command>]

# Run build-in query (detect-deadlocks)
$ ./dumpling.sh detect-deadlocks --in threaddump jstack-crash.log

# Run custom groovy query
$ ./dumpling.sh groovy --in threaddump jstack-crash.log <<< "print runtime.threads.grep { runtime.threads.grep { it.threadStatus.waiting } }"

# Run interactive shell for investigation
$ ./dumpling.sh groovysh
groovy:000> rt = load("oraclejdk-1.7.0_51.log")
===> com.github.olivergondza.dumpling.model.ProcessRuntime@10745d92
groovy:000> rt.threads.grep { it.threadStatus == ThreadStatus.RUNNABLE }
```

Alternatively, users can build self-contained `./target/dumpling.jar` using `mvn package`.

Further reading:

- [Quickstart guide](DOCS/QUICKSTART.md)
- [Dumpling DSL tutorial](DOCS/INTRO.md)

## Modules

### model

Immutable domain objects. 

### query

Collection of predefined queries shipped with dumpling.

### factory

Different strategies to acquire process runtime.

### cli

Necessary glue to use dumpling from command line.
