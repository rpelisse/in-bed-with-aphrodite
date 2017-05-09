#!/bin/bash


readonly JCOMMANDER_VERSION='1.48'
readonly JCOMMANDER=${JCOMMANDER:-"${HOME}/.m2/repository/com/beust/jcommander/${JCOMMANDER_VERSION}/jcommander-${JCOMMANDER_VERSION}.jar"}

readonly APHRODITE_VERSION=${APHRODITE_VERSION:-'0.4.2-SNAPSHOT'}
readonly APHRODITE=${APHRODITE:-"${HOME}/.m2/repository/org/jboss/set/aphrodite/${APHRODITE_VERSION}/aphrodite-${APHRODITE_VERSION}.jar"}

readonly APHRODITE_CONFIG=${APHRODITE_CONFIG:-"$(pwd)/aphrodite-config.json"}

readonly INTERACTIVE=${INTERACTIVE:-''}

readonly SCRIPT=${1}

shift

scala -Daphrodite.config=${APHRODITE_CONFIG}  -classpath "${APHRODITE}:${JCOMMANDER}" "${SCRIPT}" ${@}  2>&1 | \
  sed -e '/INFOS:/d' -e  '/SLF4J:/d' -e '/logWarnMessage/d' -e '/svn repository/d' -e '/No repositories found which correspond to url:/d' -e '/org.jboss.set.aphrodite.Aphrodite init/d'
