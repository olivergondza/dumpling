#!/bin/bash
dir="$( cd "$( dirname "$0" )" && pwd )"
jar="$dir/target/dumpling.jar"
if [ ! -s $jar ]
then
  mvn package -DskipTests -f "$dir/pom.xml"
fi
java -jar "$jar" "$@"
