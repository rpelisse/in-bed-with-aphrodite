#!/bin/bash


readonly JCOMMANDER_VERSION='1.48'
readonly JCOMMANDER=${JCOMMANDER:-"${HOME}/.m2/repository/com/beust/jcommander/${JCOMMANDER_VERSION}/jcommander-${JCOMMANDER_VERSION}.jar"}

readonly APHRODITE_VERSION='0.2.2'
readonly APHRODITE=${APHRODITE:-"${HOME}/.m2/repository/org/jboss/set/aphrodite/${APHRODITE_VERSION}/aphrodite-${APHRODITE_VERSION}.jar"}

readonly INTERACTIVE=${INTERACTIVE:-''}

if [ -z "${BZ_USERNAME}" ]; then
  echo "No 'username' for Bugzilla provided."
  exit 1
fi

if [ -z "${BZ_PASSWORD}" ]; then
  echo "No 'password' for Bugzilla provided."
  exit 2
fi

readonly SCRIPT=${1}

shift

scala -classpath "${APHRODITE}:${JCOMMANDER}" "${SCRIPT}" -u "${BZ_USERNAME}" -p "${BZ_PASSWORD}" ${@}
