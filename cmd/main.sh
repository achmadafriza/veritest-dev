#!/usr/bin/env bash
# Source: https://gist.github.com/wndhydrnt/3be3cd4552ec356f98e7497b0d4a426c
# Example:
# $ bash ./main.sh
# [baz] Sun Aug 11 12:05:28 CEST 2019
# [foo] Sun Aug 11 12:05:28 CEST 2019
# [bar] Sun Aug 11 12:05:28 CEST 2019
# [foo] Sun Aug 11 12:05:31 CEST 2019
# [baz] Sun Aug 11 12:05:31 CEST 2019
# [foo] exiting
# [baz] Sun Aug 11 12:05:34 CEST 2019
# [bar] Sun Aug 11 12:05:37 CEST 2019
# [baz] Sun Aug 11 12:05:37 CEST 2019
# [baz] Sun Aug 11 12:05:40 CEST 2019
# [baz] exiting
# [bar] Sun Aug 11 12:05:46 CEST 2019
# [bar] Sun Aug 11 12:05:55 CEST 2019
# [bar] Sun Aug 11 12:06:04 CEST 2019
# [bar] exiting
# $ echo $?
# 1

pids=""
RESULT=0

for l in $(cat list); do
  # 'bash -eo pipefail -c' allows for capturing the correct exit code.
  # It is needed because the output of ./program.sh is piped through awk to prefix the output.
  # The pipe makes the line to always exit with a code of 0 because 'awk' always succeeds without -o pipefail.
  bash -eo pipefail -c "./program.sh | awk '{print \"[${l}] \"\$0}'" &
  pids="${pids} $!"
done

# 'jobs -p' might work as well
for pid in $pids; do
  # 'wait' is nice and returns the exit code of a process even if the process has exited already!
  wait $pid || let "RESULT=1"
done

exit ${RESULT}
