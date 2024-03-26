#!/bin/bash

# turn on bash's job control
set -m

# Start the helper process
/root/Isabelle/bin/isabelle server -p 8081 &

# Start the primary process and put it in the background
(sleep 5 &&
  ${JAVA_HOME}/bin/java -jar /root/app/app.jar -Dlog4j2.contextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector)

exit $?
