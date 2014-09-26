#!/bin/bash
dir="$( cd "$( dirname "$0" )" && pwd )"

if [ `ls $dir/target/dumpling-*-shaded.jar 2> /dev/null | wc -l` != 1 ]
then
  echo "No war found, building it now..."
  mvn clean package -DskipTests -f "$dir/pom.xml" > /dev/null
fi

jar=`ls $dir/target/dumpling-*-shaded.jar | head -n 1`
java -jar "$jar" "$@"
