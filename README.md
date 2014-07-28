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

Use dumpling to discover problems in thread dumps.

- Build self-contained `dumpling.jar`
```bash
    git clone https://github.com/olivergondza/dumpling.git
    cd dumpling
    mvn package
    cd ..
    mv dumpling/target/dumpling.jar .
```

- Invoke queries as needed
```bash
    java -jar dumpling.jar help
    java -jar dumpling.jar help <command>

    # Run build-in query (detect-deadlocks)
    java -jar dumpling.jar detect-deadlocks --in threaddump jstack-crash.log

    # Run custom groovy query
    java -jar dumpling.jar groovy --in threaddump jstack-crash.log <<< "print runtime.threads.grep { it.threadStatus == ThreadStatus.RUNNABLE }"
```

- Profit

## Modules

### model

Immutable domain objects. 

### query

Collection of predefined queries shipped with dumpling.

### factory

Different strategies to acquire process runtime.

### cli

Necessary glue to use dumpling from command line.
