#!/usr/bin/env bash

iterations=$((1 + RANDOM % 10))
pause=$((1 + RANDOM % 10))
exit_code=$((RANDOM % 2))

for (( i=1; i<=${iterations}; i++ ))
do
  date
  sleep ${pause}
done

echo "exiting ${exit_code}"
exit ${exit_code}
