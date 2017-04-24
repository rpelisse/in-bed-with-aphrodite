#!/bin/bash

readonly SCRIPT_HOME='/home/rpelisse/Repositories/redhat/jboss-set/in-bed-with-aphrodite.git'
readonly URL_PREFIX="https://issues.jboss.org/browse/"

readonly OUTPUT=$(mktemp)

if [ ! -d "${SCRIPT_HOME}" ]; then
  echo "Provided SCRIPT_HOME does not exist: ${SCRIPT_HOME}"
  exit 1
fi

echo "Outputfile is ${OUTPUT}" 1>&2
source jira-env.sh
for issue in $(ls -1d ~/Repositories/redhat/issues/JBEAP-* )
do
    ${SCRIPT_HOME}/issue-status.sh "${URL_PREFIX}$(basename ${issue})"
done
