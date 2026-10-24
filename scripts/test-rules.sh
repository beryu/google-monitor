#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
test_dir=$(mktemp -d)
trap 'rm -rf "$test_dir"' EXIT
javac -d "$test_dir" app/src/main/java/com/beryu/tvhome/InputRules.java \
    app/src/test/java/com/beryu/tvhome/InputRulesTest.java
java -cp "$test_dir" com.beryu.tvhome.InputRulesTest
