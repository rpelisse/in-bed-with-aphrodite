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

if [ -z "${#}" ]; then
  echo "No bug ID provided."
  usage
  exit 1
fi

readonly SET_ISSUES_STATUS="${SCRIPT_HOME}/set-issues-status.csv"
if [ -e "${SET_ISSUES_STATUS}" ]; then
    set_status=$(grep -e "${@}" "${SET_ISSUES_STATUS}" | cut -d\   -f2 | sed -e 's/ //g' )
fi

if [ ! -z "${set_status}" ]; then
  ${SCRIPT_HOME}/run-scala.sh ${SCRIPT_HOME}/scala/${SCRIPT_NAME} -i "${@}" | sed -e "s/ASSIGNED/${set_status}/" -e "s/NEW/${set_status}/" -e "s/POST/${set_status}/"
else
   ${SCRIPT_HOME}/run-scala.sh ${SCRIPT_HOME}/scala/${SCRIPT_NAME} -i "${@}"
fi
