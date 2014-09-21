#!/bin/sh

refdoc="refdoc"
rm -rf refdoc

refdocs="refdoc.md"
rm $refdocs
cat > $refdocs <<EOF
---
layout: default
title: Reference documentation
---
# {{page.title}}
EOF

for tag in `git tag | grep dumpling- | grep -v '\-SNAPSHOT'`; do
    target="$refdoc/$tag"
    last_tag=$tag
    mkdir -p $target

    git checkout $tag src/main/ pom.xml
    mvn clean javadoc:javadoc
    mv target/site/apidocs $target/

    echo "[$tag]($target/)" >> $refdocs
    sed "s/TITLE/Reference documentation for $tag/" _includes/refdoc.index > $target/index.md
done
ln -s $last_tag $refdoc/latest
echo "[Latest]($refdoc/latest/)" >> $refdocs

git checkout gh-pages
git rm -rf src/main/ pom.xml
