#!/bin/bash

readonly CLASSPATH=${CLASSPATH:-"${HOME}/.m2/repository/org/jboss/jbossset/bugclerk-dist/1.0.1.Final-SNAPSHOT/bugclerk-dist-1.0.1.Final-SNAPSHOT-shaded.jar"}
readonly APHRODITE_CONFIG=${APHRODITE_CONFIG:-"$(pwd)/aphrodite-config.json"}

readonly SCRIPT=${1}

shift

scala  -Daphrodite.config=${APHRODITE_CONFIG}  -classpath "${CLASSPATH}" "${SCRIPT}" ${@}  2>&1 | \
    sed -e '/INFOS:/d' -e  '/SLF4J:/d' -e '/logWarnMessage/d' -e '/svn repository/d' \
    -e '/No repositories found which correspond to url:/d' -e '/org.jboss.set.aphrodite.Aphrodite init/d' \
    -e '/Unable to initiatilise Aphrodite, as a valid org.jboss.set.aphrodite.spi.RepositoryService does not exist./d'
