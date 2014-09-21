#!/bin/sh

refdoc="refdoc"

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
    if [ -d $target ]; then
        echo "Skipping $target as $target already exists"
        echo "[$tag]($target/)" >> $refdocs
        continue
    fi

    mkdir -p $target
    git checkout $tag src/main/
    mvn clean javadoc:javadoc
    mv target/site/apidocs $target/

    sed "s/TITLE/Reference documentation for $tag/" _includes/refdoc.index > $target/index.md
done
unlink $refdoc/latest
ln -s $last_tag $refdoc/latest
echo "[Latest]($refdoc/latest/)" >> $refdocs

git checkout gh-pages
git rm -rf src/main/ pom.xml
