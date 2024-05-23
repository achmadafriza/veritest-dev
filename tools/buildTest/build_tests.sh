# Please adjust the directories
pcregrep -r -nH -M 'optimization.*: "[^"]*"' /mnt/c/Programming/Thesis/veriopt-dev/isabelle/Optimizations/Canonicalizations > \
  ../../tests/veriopt/veriopt-rules.log

# Convert the grepped rules into json files for test suite
perl parse_optimization.pl ../../tests/veriopt/veriopt-rules.log ../../tests/veriopt/edited/

grep -r 'veriopt:' /mnt/c/Programming/Thesis/graal/compiler/**/*.java | \
  # Convert veriopt comments from graal compiler and removing TODOs
  sed -n 's/^.*\/\/ veriopt: \(.*\)/\1/p' | \
  sed 's/^.*TODO.*$//' | \
  # Convert the captured rules in the format of Rule: (...) into format similar to grep
  sed -n 's/^\([^:]*\): \(.*\)$/\.\/compiler\/grepped\.java:999:optimization \1: \"\2\"/p' | \
  # Change -> into \<longmapsto>
  sed -n 's/|->/\\<longmapsto>/p' | \
  # Remove any comments inside the optimization rule
  sed 's|//.*|\"|' | \
  # Remove any lines that still have todos
  grep -vi 'todo' | \
  # Remove the specific optimization rule as it is not finished yet
  grep -v 'MergeZeroExtendAdd\|MergeSignExtendAdd' > \
  ../../tests/graal/veriopt-rules.log

perl parse_optimization.pl ../../tests/graal/veriopt-rules.log ../../tests/graal/original/

perl parse_optimization.pl ../../tests/veriopt/veriopt-rules-malformed.log ../../tests/veriopt/malformed/