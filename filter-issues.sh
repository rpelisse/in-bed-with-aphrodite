#!/bin/bash

readonly SCRIPT_HOME=${SCRIPT_HOME:-'.'}
readonly SCRIPT_NAME=$(basename ${0} | sed -e 's;.sh;.scala;g')

usage() {
  echo "$(basename ${0}) -f <filter-name> [-c]"
  echo ''
  echo '-c clean the local cache, thus forcing a new fetch from server'
  exit
}

${SCRIPT_HOME}/run-scala.sh ${SCRIPT_HOME}/scala/${SCRIPT_NAME} ${@}
