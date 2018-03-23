#!/bin/bash

readonly BUGCLERK_JAR=${BUGCLERK_JAR:-"${HOME}/.m2/repository/org/jboss/jbossset/bugclerk-dist/1.0.2/bugclerk-dist-1.0.2-shaded.jar"}
readonly JSOUP_JAR=${JSOUP_JAR:-'/home/rpelisse/.m2/repository/org/jsoup/jsoup/1.8.3.redhat-2/jsoup-1.8.3.redhat-2.jar'}
readonly CLASSPATH=${BUGCLERK_JAR}:${JSOUP_JAR}:${CLASSPATH}

readonly APHRODITE_CONFIG=${APHRODITE_CONFIG:-"$(pwd)/aphrodite-config.json"}

readonly SCRIPT=${1}

checkIfFileExists() {
  local file=${1}

  if [ ! -e "${file}" ]; then
    echo "No such file: ${file} - can't run scripts without it, aborting."
    exit 1
  fi
}

checkIfFileExists "${BUGCLERK_JAR}"
checkIfFileExists "${JSOUP_JAR}"
checkIfFileExists "${APHRODITE_CONFIG}"

shift

scala  -Daphrodite.config=${APHRODITE_CONFIG}  -classpath "${CLASSPATH}" "${SCRIPT}" ${@}  2>&1 | \
    sed -e '/INFOS:/d' -e  '/SLF4J:/d' -e '/logWarnMessage/d' -e '/svn repository/d' \
    -e '/No repositories found which correspond to url:/d' -e '/org.jboss.set.aphrodite.Aphrodite init/d' \
    -e '/Unable to initiatilise Aphrodite, as a valid org.jboss.set.aphrodite.spi.RepositoryService does not exist./d'
