#!/bin/bash
readonly BURN_RATE_FILE="${HOME}/Documents/redhat/jboss-set/reports/cw-$(weekno | cut -f2 -d: | sed -e 's/ //g').$(date +%Y).md"

readonly NO_FILTER=${1}

if [ ! -e "${BURN_RATE_FILE}" ]; then
  echo "Burn rate file does not exist: ${BURN_RATE_FILE}"
  exit 1
fi

readonly URLS=$(grep -e '^- ' "${BURN_RATE_FILE}"  | sed -e 's/^- //')

if [ -z "${URLS}" ]; then
  echo "No URLs in ${BURN_RATE_FILE}."
  exit 2
fi

output=$(mktemp)
$(pwd)/issue-status.sh ${URLS} 2> /dev/null > "${output}"
nbIssue=$(wc -l "${output}")
echo "Burn Rate is : ${nbIssue}"
if [ ! -z "${NO_FILTER}" ]; then
    cat "${output}" | sed -e '/POST/d' -e '/VERIFIED/d' -e '/CLOSED/d'  -e '/MODIFIED/d' | sort -k2
else
    cat "${output}"
fi
rm -f "${output}"
