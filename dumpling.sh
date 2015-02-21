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
  metadata_url="https://oss.sonatype.org/content/repositories/releases/com/github/olivergondza/dumpling/maven-metadata.xml"
  latest=`wget --no-check-certificate $metadata_url -O - 2> /dev/null | grep \<latest\> | sed -e "s/<[^>]*>//g" -e "s/\s*//"`
  jar_url="https://oss.sonatype.org/content/repositories/releases/com/github/olivergondza/dumpling/$latest/dumpling-$latest-shaded.jar"
  echo "Downloading Dumpling $latest now..." >&2
  wget --no-check-certificate -nv -O $1 $jar_url || rm $1
}

dir="$( cd "$( dirname "$0" )" && pwd )"

if [ `ls $dir/target/dumpling-*-shaded.jar 2> /dev/null | wc -l` != 1 ]; then
  if [ -f $dir/pom.xml -a -d $dir/src/ ]; then
    echo "No war found, building it now..." >&2
    mvn clean package -DskipTests -f "$dir/pom.xml" > /dev/null
    jar=`ls $dir/target/dumpling-*-shaded.jar | head -n 1`
  else
    jar="$dir/dumpling.jar"
    if [ ! -f $jar ]; then
      download $jar
    fi
  fi
else
  jar=`ls $dir/target/dumpling-*-shaded.jar | head -n 1`
fi

java -jar "$jar" "$@"
