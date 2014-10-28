#!/bin/bash
dir="$( cd "$( dirname "$0" )" && pwd )"

if [ `ls $dir/target/dumpling-*-shaded.jar 2> /dev/null | wc -l` != 1 ]; then
  if [ -f $dir/pom.xml -a -d $dir/src/ ]; then
    echo "No war found, building it now..."
    mvn clean package -DskipTests -f "$dir/pom.xml" > /dev/null
    jar=`ls $dir/target/dumpling-*-shaded.jar | head -n 1`
  else
    jar="$dir/dumpling.jar"
    if [ ! -f $jar ]; then
      echo "No sourcces found, downloading it now..."
      wget --no-check-certificate -nv -O $jar https://oss.sonatype.org/content/repositories/releases/com/github/olivergondza/dumpling/0.2/dumpling-0.2-shaded.jar || rm $jar
    fi
  fi
fi

java -jar "$jar" "$@"
