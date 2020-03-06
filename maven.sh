#!/bin/sh

docker run -it --rm -v `pwd`:/usr/src/decision_tree -v maven-m2:/root/.m2 -w /usr/src/decision_tree maven "$@"
