#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

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

readonly SCRIPT_OUTPUT_LOGFILE=$(mktemp)
${SCRIPT_HOME}/run-scala.sh ${SCRIPT_HOME}/scala/${SCRIPT_NAME} -i "${BUG_ID}" -d "${WORKSPACES_ROOT}" | tee "${SCRIPT_OUTPUT_LOGFILE}"

readonly CLIPBOARD_APP='gpaste-client'
which "${CLIPBOARD_APP}" 2>&1 > /dev/null
if [ ${?} -eq 0 ]; then
   workdir=$(cat "${SCRIPT_OUTPUT_LOGFILE}" | grep -e 'Creating workdir'  | cut -d: -f2 )
   "${CLIPBOARD_APP}" 'add' "${workdir}"
   if [ ${?} -eq 0 ]; then
     echo "Workdir full path has been added to clipboard"
   fi
fi
rm -f "${SCRIPT_OUTPUT_LOGFILE}"
