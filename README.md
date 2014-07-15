# Dumpling

Dumpling is an object model and domain specific language to represent state of
process threads and lock based dependencies.

Dumpling is shipped with a collection of predefined queries to cover most common
situations as well as several factories to create thread model from various sources.

## Usage

### Bundle dumpling to your application

- Include following maven dependency

    <dependency>
      <groupId>com.github.olivergondza.dumpling</groupId>
      <artifactId>dumpling</artifactId>
      <version>...</version>
    </dependency>

- Instantiate runtime of current JVM

    ProcessRuntime runtime = new JvmRuntimeFactory().currentRuntime()

- Get the work done

### Invoking dumpling from console

- Build self-contained `dumpling.jar`

    git clone https://github.com/olivergondza/dumpling.git
    cd dumpling
    mvn package
    cd ..
    mv dumpling/target/dumpling.jar .

- Invoke queries as needed

    java -jar dumpling.jar help
    java -jar dumpling.jar help <command>

    # Run build-in query (`detect-deadlocks`)
    java -jar dumpling.jar detect-deadlocks --in threaddump jstack-crash.log

    # Run custom groovy query
    java -jar dumpling.jar groovy --in threaddump jstack-crash.log <<< "print runtime.threads.grep { it.threadStatus == ThreadStatus.RUNNABLE }"

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
