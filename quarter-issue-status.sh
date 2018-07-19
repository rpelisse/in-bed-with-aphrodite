#!/bin/bash
readonly BURN_RATE_FILE="${HOME}/Documents/redhat/jboss-set/reports/cw-$(weekno | cut -f2 -d: | sed -e 's/ //g').$(date +%Y).md"

readonly FILTER_RESOLVED_ISSUES=${1}

if [ ! -e "${BURN_RATE_FILE}" ]; then
  echo "Burn rate file does not exist: ${BURN_RATE_FILE}"
  exit 1
fi

readonly URLS=$(grep -e '^- ' "${BURN_RATE_FILE}"  | sed -e 's/^- //' -e '/bugzilla./d' )

if [ -z "${URLS}" ]; then
  echo "No URLs in ${BURN_RATE_FILE}."
  exit 2
fi

printIssuesLists() {
  local output=${1}

  if [ ! -z "${FILTER_RESOLVED_ISSUES}" ]; then
    cat "${output}" | sed -e '/RESOLVED/d' -e '/POST/d' -e '/VERIFIED/d' -e '/CLOSED/d'  -e '/MODIFIED/d' -e '/ON_QA/d' | sort -k2
  else
    nbIssue=0
    cat "${output}" | sed -e 's/MODIFIED/RESOLVED/' -e 's/CLOSED/RESOLVED/' | sort -k2
  fi
}

output=$(mktemp)
echo -n 'Retrieving issues... '
$(pwd)/issue-status.sh ${URLS} >> "${output}"
echo 'Done.'

export nbIssue=0
printIssuesLists "${output}" | sed -e 's/MODIFIED/RESOLVED/' -e 's/CLOSED/RESOLVED/' | sort -k2 | \
while
  read issue
do
  nbIssue=$(expr "${nbIssue}" + 1)
  echo "${nbIssue}) ${issue}"
done
echo "Burn Rate is : $(wc -l "${output}" | sed -e 's/^\([0-9]*\) *.$/\1/')"
rm -f "${output}"
