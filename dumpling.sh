#!/bin/bash
#
# The MIT License
#
# Copyright (c) 2014 Red Hat, Inc.
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
# THE SOFTWARE.

# CLI Dumpling wrapper for unix. Read more: https://olivergondza.github.io/dumpling/cli.html

function download() {
  if [ "$DUMPLING_SNAPSHOTS" == "true" ]; then
    DUMPLING_SNAPSHOTS="snapshots"
  else
    DUMPLING_SNAPSHOTS="releases"
  fi

  url_prefix="https://oss.sonatype.org/content/repositories/${DUMPLING_SNAPSHOTS}/com/github/olivergondza/dumpling"
  metadata_url="${url_prefix}/maven-metadata.xml"
  latest=$(wget --no-check-certificate $metadata_url -O - 2> /dev/null | grep \<version\> | sed -e "s/\s*<[^>]*>//g" | tail -n 1)
  jar_url="${url_prefix}/$latest/dumpling-$latest-shaded.jar"
  echo "Downloading Dumpling $latest now..." >&2
  wget --no-check-certificate -nv -O $1 $jar_url
  if [ $? != 0 ]; then
    echo "Download failed" >&2
    rm -f $1
    exit 1
  fi
}

function working_java() {
  $1 -version > /dev/null 2>&1
  return $?
}

function run_java() {
  if working_java "java"; then
    exe="java"
  else
    # Find interpreter examining running processes
    candidates=$(ps -ef | awk '{ print $8 }' | grep bin/java\\b)
    for candidate in $candidates; do
      if working_java $candidate; then
        exe=$candidate
        break
      fi
    done

    if [ "$exe" == "" ]; then
      echo "No java interpreter found. Make sure there is one on PATH" >&2
      exit 1
    fi
  fi
  $exe "$@"
}

dir="$( cd "$( dirname "$0" )" && pwd )"
cli="$dir/cli"

if [ $(ls $cli/target/dumpling-*-shaded.jar 2> /dev/null | wc -l) != 1 ]; then
  grep -q com.github.olivergondza.dumpling.cli.Main $cli/pom.xml 2> /dev/null
  if [ $? == 0 ]; then
    echo "No dumpling.jar found, building it now..." >&2
    mvn clean package -DskipTests -f "$cli/pom.xml" > /dev/null
    jar=$(ls $cli/target/dumpling-*-shaded.jar | head -n 1)
  else
    jar="$dir/dumpling.jar"
    if [ ! -f $jar ]; then
      download $jar
    fi
  fi
else
  jar=$(ls $cli/target/dumpling-*-shaded.jar | head -n 1)
fi

# Performance optimization for single runtime queries, provide DUMPLING_OPTS to override
: ${DUMPLING_OPTS="-XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Xmx256M -Xms256M"}
run_java $DUMPLING_OPTS -jar "$jar" "$@"
