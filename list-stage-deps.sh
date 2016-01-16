#!/bin/bash

readonly SCRIPT_HOME=${SCRIPT_HOME:-'.'}
readonly SCRIPT_NAME=$(basename ${0} | sed -e 's;.sh;.scala;g')

# script args
readonly BUG_ID=${1}

usage() {
  echo "$(basename ${0}) <bug-id>"
  echo ''
  exit
}

if [ -z "${BUG_ID}" ]; then
  echo "No bug ID provided."
  usage
  exit 1
fi

${SCRIPT_HOME}/run-scala.sh ${SCRIPT_HOME}/scala/${SCRIPT_NAME} -i "${BUG_ID}"
