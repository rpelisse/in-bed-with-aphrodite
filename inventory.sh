#!/bin/bash

readonly REPORT_FILE=${REPORT_FILE:-"/home/rpelisse/Documents/redhat/jboss-set/reports/cw-$(weekno | cut -f2 -d: | sed -e 's/ //g' ).2017.md"}
readonly ISSUES_LIST_FILES=${ISSUES_LIST_FILES:-'/tmp/listofissues.txt'}
readonly RESULT_FILE=${RESULT_FILE:-$(mktemp)}

if [ ! -e "${ISSUES_LIST_FILES}" ]; then
  grep -e 'JBEAP-' ${REPORT_FILE} | sed -e 's/^* //' | sed -e 's/\([0-9]\) .*$/\1/' -e 's/ -//g' | sort -u > "${ISSUES_LIST_FILES}"
  if [ ! -e "${ISSUES_LIST_FILES}" ]; then
    echo "Issues list file is missing: ${ISSUES_LIST_FILES}."
    exit 1
  fi
fi

if [ "$(wc -l "${ISSUES_LIST_FILES}" | cut -d\  -f1 )" -lt 1 ]; then
  echo "Issues list file appears empty: ${ISSUES_LIST_FILES}."
  exit 2
fi

echo "Resulting saved into ${RESULT_FILE}."
cat "${ISSUES_LIST_FILES}" | \
while
  read issue
do
  ./issue-status.sh "${issue}" | sed -e '/MODIFIED/d' -e '/VERIFIED/d' -e '/CLOSED/d'
done | tee "${RESULT_FILE}"
