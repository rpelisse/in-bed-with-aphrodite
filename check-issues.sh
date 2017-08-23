#!/bin/bash

readonly SCRIPT_HOME=${SCRIPT_HOME:-'/home/rpelisse/Repositories/redhat/jboss-set/in-bed-with-aphrodite.git'}
export SCRIPT_HOME
readonly APHRODITE_CONFIG=${APHRODITE_CONFIG:-"${SCRIPT_HOME}/aphrodite-config.json"}
export APHRODITE_CONFIG
readonly URL_PREFIX="https://issues.jboss.org/browse/"

readonly OUTPUT=$(mktemp)

if [ ! -d "${SCRIPT_HOME}" ]; then
  echo "Provided SCRIPT_HOME does not exist: ${SCRIPT_HOME}"
  exit 1
fi

echo "Outputfile is ${OUTPUT}" 1>&2
readonly RESULT_FILE=$(mktemp)
for issue in $(ls -1d /home/rpelisse/Repositories/redhat/issues/JBEAP-* )
do
    ${SCRIPT_HOME}/issue-status.sh "${URL_PREFIX}$(basename ${issue})" >> "${RESULT_FILE}"
done
cat "${RESULT_FILE}" | sort -k2
rm -f "${RESULT_FILE}"
