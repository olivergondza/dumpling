#!/bin/sh

refdoc="refdoc"

refdocs="refdoc.md"
rm $refdocs
cat > $refdocs <<EOF
---
layout: default
title: Reference documentation
prio: 90
---
# {{page.title}}
EOF

for tag in `git tag | grep dumpling- | grep -v '\-SNAPSHOT'`; do
    target="$refdoc/$tag"
    last_tag=$tag
    echo "[$tag]($target/)" >> $refdocs
    if [ -d $target ]; then
        echo "Skipping $target as $target already exists"
        continue
    fi

    mkdir -p $target
    git checkout $tag src/main/ pom.xml

echo "JAVADOC"
    # Insert generic javadoc configuration
    sed -i -e "/<.build>/r pom.xml.javadoc" pom.xml
    perl -0 -pi -e "s|<build>.*</build>||gs" pom.xml
    mvn -e clean site
    
echo "INDEXED"
    # Insert generic indexed javadoc configuration
    git checkout $tag pom.xml
    sed -i -e "/<.build>/r pom.xml.indexed" pom.xml
    perl -0 -pi -e "s|<build>.*</build>||gs" pom.xml

    mvn -e site
    mv target/site/apidocs $target/

    sed "s/TITLE/Reference documentation for $tag/" _includes/refdoc.index > $target/index.md
done
unlink $refdoc/latest
ln -s $last_tag $refdoc/latest
echo "[Latest]($refdoc/latest/)" >> $refdocs

git checkout gh-pages
git rm -rf --ignore-unmatch src/main/
