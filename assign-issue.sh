#!/bin/bash

readonly SCRIPT_HOME=${SCRIPT_HOME:-'.'}
readonly SCRIPT_NAME=$(basename ${0} | sed -e 's;.sh;.scala;g')

# script args
readonly BUG_ID=${1}
readonly ESTIMATE=${2:-'18'}
readonly ASSIGNED_TO=${3:-'rpelisse@redhat.com'}
    # sadly comment has to be last, because of issues passing parameters with spaces :(
readonly COMMENT=${4:-'I will take a look at this as soon as possible'}

usage() {
  echo "$(basename ${0}) <bug-id> [estimate] [assigned_to]"
  echo ''
  exit
}

if [ -z "${BUG_ID}" ]; then
  echo "No bug ID provided."
  usage
  exit 1
fi

${SCRIPT_HOME}/run-scala.sh ${SCRIPT_HOME}/scala/${SCRIPT_NAME} -i "${BUG_ID}" -e "${ESTIMATE}" -a "${ASSIGNED_TO}" -c "${COMMENT}"
