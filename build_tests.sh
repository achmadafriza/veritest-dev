pcregrep -r -nH -M 'optimization.*: "[^"]*"' /mnt/c/Programming/Thesis/veriopt-dev/isabelle/Optimizations > ./tests/veriopt/veriopt-rules.log
perl parse_optimization.pl ./tests/veriopt/veriopt-rules.log ./tests/veriopt/original/

grep -r 'veriopt:' /mnt/c/Programming/Thesis/graal/compiler/**/*.java | sed -n 's/^.*\/\/ veriopt: \(.*\)/\1/p' | sed 's/^.*TODO.*$//' | sed -n 's/^\([^:]*\): \(.*\)$/\.\/compiler\/grepped\.java:999:optimization \1: \"\2\"/p' | sed -n 's/|->/\\<longmapsto>/p' | sed 's|//.*|\"|' | grep -vi 'todo' | grep -v 'MergeZeroExtendAdd\|MergeSignExtendAdd' > ./tests/graal/veriopt-rules.log
perl parse_optimization.pl ./tests/graal/veriopt-rules.log ./tests/graal/original/