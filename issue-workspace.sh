#!/bin/bash

readonly SCRIPT_HOME=${SCRIPT_HOME:-'.'}
readonly SCRIPT_NAME=$(basename ${0} | sed -e 's;.sh;.scala;g')

readonly WORKSPACES_ROOT=${WORKSPACES_ROOT:-"${HOME}/Repositories/redhat/issues"}

readonly BUG_ID=${1}

usage() {
  echo "$(basename ${0}) -i <bugId> -r <rootDir>"
  echo
}

if [ -z "${BUG_ID}" ]; then
  echo "No bug ID provided."
  usage
  exit 1
fi

if [ ! -d "${WORKSPACES_ROOT}" ]; then
  echo "Provided root dir is not a directory: ${WORKSPACES_ROOT}."
  exit 2
fi

${SCRIPT_HOME}/run-scala.sh ${SCRIPT_HOME}/scala/${SCRIPT_NAME} -i "${BUG_ID}" -d "${WORKSPACES_ROOT}"
