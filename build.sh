#!/bin/sh

#set -x

ROOT=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

# Run maven sire lifecycle with amended pom.xml
function run_amended_site() {
    amend_pom="$1"

    (head -n -1 pom.xml; cat $amend_pom; echo "</project>") > temp.pom.xml
    mvn -e -f temp.pom.xml site
    rm temp.pom.xml
}

refdoc="refdoc"

source_paths="src/main/ */src/main/ pom.xml */pom.xml dumpling.sh"

prio=0 # Used for ordering
for tag in $(git tag | grep dumpling- | grep -v '\-SNAPSHOT'); do
    release_name="${tag/-parent/}"
    if [[ $release_name == "dumpling-0."* ]]; then
        continue # To old to bother
    fi

    target="$refdoc/$release_name"
    ((prio++))

    echo -e "\n:: $release_name ::\n"

    checkout_paths="pom.xml dumpling.sh"
    if [[ "$release_name" == *"2."* ]]; then
        multimodule=true
        checkout_paths="$checkout_paths core/pom.xml cli/pom.xml groovy-api/pom.xml test-utils/pom.xml core/src/main/ cli/src/main/ groovy-api/src/main/ test-utils/src/main/"
    else
        multimodule=false
        checkout_paths="$checkout_paths src/main/"
    fi

    mkdir -p $target
    rm -rf $source_paths
    git checkout $tag $checkout_paths

    if [ ! -d $target/apidocs -o ! -f $target/index.md ]; then
        mvn -e -q clean package -DskipTests=true
    fi

    source_dir="."

    if [ ! -d $target/apidocs ]; then
        if [ "$multimodule" == true ]; then
            # No need to publish test-utils and cli. quite likely not even groovy-api
            source_dir="core"
        fi
        pushd $source_dir
        run_amended_site $ROOT/pom.xml.javadoc
        popd

        mv $source_dir/target/site/apidocs $target/
    fi

    if [ ! -f $target/index.md ]; then
        if [ "$multimodule" == true ]; then
            # Use cli as it contains all the classes
            source_dir="core"
        fi

        pushd $source_dir
        run_amended_site $ROOT/pom.xml.indexed
        popd

        mv $source_dir/target/site/apidocs/*.md $target/

        cp _includes/refdoc.index $target/index.md
        sed -i "s/TAG/$release_name/" $target/*.md
        sed -i "s/PRIO/$prio/" $target/index.md
    fi
done
cp _includes/refdoc.index $refdoc/index.md
sed -i -e "s/TAG/$release_name/" -e "s/prio: PRIO//" -e "s|prefix:|prefix: $release_name/|" -e "s/category: refdoc//" $refdoc/index.md

git checkout -q gh-pages
git rm -rfq --ignore-unmatch $source_paths
