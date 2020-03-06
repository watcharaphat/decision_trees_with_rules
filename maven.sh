#!/bin/sh

DIRNAME="$(cd $(dirname $0) && pwd)"
docker run -it --rm -v ${DIRNAME}:/usr/src/decision_tree -v maven-m2:/root/.m2 -w /usr/src/decision_tree maven "$@"
