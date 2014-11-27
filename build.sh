#!/bin/sh

refdoc="refdoc"

prio=0
for tag in `git tag | grep dumpling- | grep -v '\-SNAPSHOT'`; do
    target="$refdoc/$tag"
    last_tag=$tag
    ((prio++))

    mkdir -p $target
    git checkout $tag src/main/ pom.xml

    if [ ! -d $target/apidocs ]; then
        # Insert generic javadoc configuration
        sed -i -e "/<.build>/r pom.xml.javadoc" pom.xml
        perl -0 -pi -e "s|<build>.*</build>||gs" pom.xml
        mvn -e clean site
        mv target/site/apidocs $target/
    fi

    if [ ! -f $target/index.md ]; then
        # Insert indexed javadoc configuration
        git checkout $tag pom.xml
        sed -i -e "/<.build>/r pom.xml.indexed" pom.xml
        perl -0 -pi -e "s|<build>.*</build>||gs" pom.xml
        mvn -e site
        mv target/site/apidocs/*.md $target/

        cp _includes/refdoc.index $target/index.md
        sed -i "s/TAG/$tag/" $target/*.md
        sed -i "s/PRIO/$prio/" $target/index.md # Used for ordering
    fi
done
cp _includes/refdoc.index $refdoc/index.md
sed -i -e "s/TAG/Dumpling Latest/" -e "s/prio: PRIO//" -e "s|prefix:|prefix: $tag/|" -e "s/category: refdoc//" $refdoc/index.md

git checkout gh-pages
git rm -rf --ignore-unmatch src/main/ pom.xml
