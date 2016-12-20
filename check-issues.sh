#!/bin/bash

readonly SCRIPT_HOME='/home/rpelisse/Repositories/redhat/jboss-set/in-bed-with-aphrodite.git'
readonly URL_PREFIX="https://issues.jboss.org/browse/"

readonly OUTPUT=$(mktemp)

echo "Outputfile is ${OUTPUT}"
source jira-env.sh
for issue in $(ls -1d ~/Repositories/redhat/issues/JBEAP-* )
do
    ${SCRIPT_HOME}/issue-status.sh "${URL_PREFIX}$(basename ${issue})"
done
