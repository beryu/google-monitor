#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
test_dir=$(mktemp -d)
trap 'rm -rf "$test_dir"' EXIT
javac -d "$test_dir" app/src/main/java/com/beryu/tvhome/InputRules.java \
    app/src/test/java/com/beryu/tvhome/InputRulesTest.java \
    app/src/main/java/com/beryu/tvhome/HomeRedirectRules.java \
    app/src/test/java/com/beryu/tvhome/HomeRedirectRulesTest.java \
    app/src/main/java/com/beryu/tvhome/HomeKeyRules.java \
    app/src/test/java/com/beryu/tvhome/HomeKeyRulesTest.java \
    app/src/main/java/com/beryu/tvhome/ThemeRules.java \
    app/src/test/java/com/beryu/tvhome/ThemeRulesTest.java
java -cp "$test_dir" com.beryu.tvhome.InputRulesTest
java -cp "$test_dir" com.beryu.tvhome.HomeRedirectRulesTest
java -cp "$test_dir" com.beryu.tvhome.HomeKeyRulesTest
java -cp "$test_dir" com.beryu.tvhome.ThemeRulesTest
