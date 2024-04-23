pcregrep -r -nH -M 'optimization.*: "[^"]*"' ../isabelle/Optimizations > ./test/veriopt/veriopt-rules.log
perl parse_optimization.pl ./test/veriopt/veriopt-rules.log ./test/veriopt/original/

grep -r 'veriopt:' /mnt/c/Programming/Thesis/graal/**/*.java | sed -n 's/^.*\/\/ veriopt: \(.*\)/\1/p' | sed 's/^.*TODO.*$//' | sed -n 's/^\([^:]*\): \(.*\)$/\.\/compiler\/grepped\.java:999:optimization \1: \"\2\"/p' | sed -n 's/|->/\\<longmapsto>/p' | sed 's|//.*|\"|' | grep -vi 'todo' | grep -v 'MergeZeroExtendAdd\|MergeSignExtendAdd' > ./test/graal/veriopt-rules.log
perl parse_optimization.pl ./test/graal/veriopt-rules.log ./test/graal/original/