#!/usr/bin/env bash
# Same output as "main.sh" but using xargs.

processes=$(cat ./list | wc -l)
cat ./list | xargs -I{} -P ${processes} bash -eo pipefail -c "./program.sh | awk -W interactive '{print \"[{}] \"\$0}'"

echo "xargs exit code: $?"
