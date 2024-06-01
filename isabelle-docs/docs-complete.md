# Veritest

## jedit

```
isabelle jedit -d . ROOT
```

## Isabelle Server

```
isabelle server
```

=>

```
server "isabelle" = 127.0.0.1:43189 (password "0aaaaea2-849b-4458-8812-0945b426e81c")
```

## Isabelle Client

```isabelle client -n isabelle```

=>

```
server "isabelle" = 127.0.0.1:43189 (password "0aaaaea2-849b-4458-8812-0945b426e81c")
OK {"isabelle_id":"b5f3d1051b13","isabelle_name":"Isabelle2023"}
```

## Isabelle Options

Interesting values:
```bash
option build_cluster_delay : real = 1.0
  -- "delay build process main loop (cluster)"
option build_cluster_identifier : string = "build_cluster"
  -- "ISABELLE_IDENTIFIER for remote build cluster sessions"
option build_cluster_root : string = "$USER_HOME/.isabelle/build_cluster"
  -- "root directory for remote build cluster sessions"
option build_database : bool = false
  -- "expose state of build process via central database"
option build_database_host : string = ""
option build_database_name : string = ""
option build_database_password : string = ""
option build_database_port : int = 0
option build_database_server : bool = false
option build_database_slice : real = 300
  -- "slice size in MiB for ML heap stored within database"
option build_database_ssh_host : string = ""
option build_database_ssh_port : int = 0
option build_database_ssh_user : string = ""
option build_database_synchronous_commit : string = "off" (standard "on")
  -- "see https://www.postgresql.org/docs/current/runtime-config-wal.html#GUC-SYNCHRONOUS-COMMIT"
option build_database_user : string = ""
option build_delay : real = 0.2
  -- "delay build process main loop (local)"
option build_engine : string = ""
  -- "alternative session build engine"
option build_hostname : string = ""
  -- "alternative hostname for build process (default $ISABELLE_HOSTNAME)"

...

option document : string = "" (standard "true")
  -- "build PDF document (enabled for "pdf" or "true", disabled for "" or "false")"
option document_bibliography : bool = false
  -- "explicitly enable use of bibtex (default: according to presence of root.bib)"
option document_build : string = "lualatex" (standard "build")
  -- "document build engine (e.g. build, lualatex, pdflatex)"
option document_cite_commands : string = "cite,nocite,citet,citep"
  -- "antiquotation commands to determine the structure of bibliography"
option document_comment_latex : bool = false
  -- "use regular LaTeX version of comment.sty, instead of historic plain TeX version"
option document_echo : bool = false
  -- "inform about document file names during session presentation"
option document_heading_prefix : string = "isamarkup" (standard)
  -- "prefix for LaTeX macros generated from 'chapter', 'section' etc."
option document_logo : string = "" (standard "_")
  -- "generate named instance of Isabelle logo (underscore means unnamed variant)"
option document_output : string = "" (standard "output")
  -- "document output directory"
option document_tags : string = ""
  -- "default command tags (separated by commas)"
option document_variants : string = "document"
  -- "alternative document variants (separated by colons)"

...

option mirabelle_actions : string = ""
  -- "Mirabelle actions (outer syntax, separated by semicolons)"
option mirabelle_dry_run : bool = false
  -- "initialize the actions, print on which goals they would be run, and finalize the actions"
option mirabelle_max_calls : int = 0
  -- "max. no. of calls to each action (0: unbounded)"
option mirabelle_output_dir : string = "mirabelle"
  -- "output directory for log files and generated artefacts"
option mirabelle_randomize : int = 0
  -- "seed to randomize the goals before selection (0: no randomization)"
option mirabelle_stride : int = 1
  -- "run actions on every nth goal (0: uniform distribution)"
option mirabelle_theories : string = ""
  -- "Mirabelle theories (names with optional line range, separated by commas)"

...

public option sledgehammer_provers : string = "cvc4 verit z3 e spass vampire zipperposition"
  -- "provers for Sledgehammer (separated by blanks)"
public option sledgehammer_timeout : int = 30
  -- "provers will be interrupted after this time (in seconds)"

...

public option threads : int = 0
  -- "maximum number of worker threads for prover process (0 = hardware max.)"
option threads_stack_limit : real = 0.25
  -- "maximum stack size for worker threads (in giga words, 0 = unlimited)"
option threads_trace : int = 0
  -- "level of tracing information for multithreading"

...

option timeout : real = 0
  -- "timeout for session build job (seconds > 0)"
option timeout_build : bool = true
  -- "observe timeout for session build"
public option timeout_scale : real = 1.0
  -- "scale factor for timeout in Isabelle/ML and session build"

...

option headless_consolidate_delay : real = 15
  -- "delay to consolidate status of command evaluation (execution forks)"

option headless_prune_delay : real = 60
  -- "delay to prune history (delete old versions)"

option headless_check_delay : real = 0.5
  -- "delay for theory status check during PIDE processing (seconds)"

option headless_check_limit : int = 0
  -- "maximum number of theory status checks (0 = unlimited)"

option headless_nodes_status_delay : real = -1
  -- "delay for overall nodes status check during PIDE processing (seconds, disabled for < 0)"

option headless_watchdog_timeout : real = 600
  -- "watchdog timeout for PIDE processing of broken theories (seconds, 0 = disabled)"

option headless_commit_cleanup_delay : real = 60
  -- "delay for cleanup of already imported theories (seconds, 0 = disabled)"

option headless_load_limit : real = 5.0
  -- "limit in MiB for loaded theory files (0 = unlimited)"
```

## Isabelle Sessions

Note:

> Coordination of independent build processes is at the discretion of the client (or end-user), just as for isabelle build and isabelle jedit. There is no built-in coordination of conflicting builds with overlapping hierarchies of session images. In the worst case, a session image produced by one task may get overwritten by another task!

Steps:

1. Build/start session from the ground up:

`session_build `

```json
{
    session: string, // session name
    preferences?: string, // -> isabelle options -l
    options?: [string], // options in ROOT
    dirs?: [string], // session dir
    include_sessions: [string], // sessions in ROOT
    verbose?: bool // -> isabelle build -v
}
```

Note:
- found an interesting option from docs:

```json
session_start {"session": "HOL", "options": ["headless_consolidate_delay=0.5", "headless_prune_delay=5"]}
```

2. Set theories for the session?

Note: I think you don't need to set theories for a session. All theories defined in dir on ROOT sessions are imported.

3. Use theories to check optimization rule:

`use_theories`

```json
{
    session_id: string, 
    theories: [string], 
    // Directory to find theories
    master_dir?: string, 
    // Prettify output
    pretty_margin?: double,
    unicode_symbols?: bool, 
    export_pattern?: string, 
    // Observer pattern notifications
    check_delay?: double,
    check_limit?: int,
    watchdog_timeout?: double,
    nodes_status_delay?: double
}
```

Notifications:

> The status of PIDE processing is checked every `check_delay` seconds, and bounded by `check_limit` attempts (default: 0, i.e. unbounded). A `check_limit` > 0 effectively specifies a global timeout of `check_delay` × `check_limit` seconds.
> If `watchdog_timeout` is greater than 0, it specifies the timespan (in seconds) after the last command status change of Isabelle/PIDE, before finishing with a potentially non-terminating or deadlocked execution.
> A non-negative `nodes_status_delay` enables continuous notifications of kind `nodes_status`, with a field of name and type `nodes_status`. The time interval is specified in seconds; by default it is negative and thus disabled.

4. profit



## Steps

Note: isabelle are picky about json formatting

1. `session_build`

```json
{
    session: "HOL-Library",
    options?: ["document=false"],
    verbose: true
}
```

1. `session_build`

```json
{
    session: "Graph",
    options?: ["document=false"],
    dirs?: ["Optimizations/DSL"],
    include_sessions: [string],
    verbose: true
}
```

1. `session_build`

```json
{
    "session": "OptimizationDSL",
    "options": ["document=false", "show_question_marks=false", "quick_and_dirty"],
    "dirs": ["."],
    "include_sessions": ["HOL-Library", "Graph", "Semantics", "Proofs"],
    "verbose": true
}
```

See log for sample output.

```json
FINISHED 
{
    "ok": true,
    "return_code": 0,
    "sessions": [
        {
            "session": "Pure",
            "ok": true,
            "timeout": false,
            "timing": {
                "elapsed": 0,
                "cpu": 0,
                "gc": 0
            },
            "return_code": 0
        },
        {
            "session": "HOL",
            "ok": true,
            "timeout": false,
            "timing": {
                "elapsed": 0,
                "cpu": 0,
                "gc": 0
            },
            "return_code": 0
        },
        {
            "session": "HOL-Library",
            "ok": true,
            "timeout": false,
            "timing": {
                "elapsed": 343.341,
                "cpu": 1163.28,
                "gc": 0
            },
            "return_code": 0
        },
        {
            "session": "Graph",
            "ok": true,
            "timeout": false,
            "timing": {
                "elapsed": 142.561,
                "cpu": 354.174,
                "gc": 0
            },
            "return_code": 0
        },
        {
            "session": "Semantics",
            "ok": true,
            "timeout": false,
            "timing": {
                "elapsed": 662.062,
                "cpu": 1163.976,
                "gc": 0
            },
            "return_code": 0
        },
        {
            "session": "Proofs",
            "ok": true,
            "timeout": false,
            "timing": {
                "elapsed": 47.246,
                "cpu": 88.609,
                "gc": 0
            },
            "return_code": 0
        },
        {
            "session": "OptimizationDSL",
            "ok": true,
            "timeout": false,
            "timing": {
                "elapsed": 35.794,
                "cpu": 49.056,
                "gc": 0
            },
            "return_code": 0
        }
    ],
    "task": "f3e3cd55-f9b4-47e5-b2f2-b3becd93dbed"
}
```

Elapsed time: about 20 mins.

session_build { "session": "OptimizationDSL", "options": ["document=false", "show_question_marks=false", "quick_and_dirty"], "dirs": ["/mnt/c/Programming/Thesis/veriopt-dev/isabelle"], "include_sessions": ["HOL-Library", "Graph", "Semantics", "Proofs"], "verbose": true }
session_build { "session": "Canonicalizations", "options": ["document=false", "show_question_marks=false", "quick_and_dirty"], "dirs": ["/mnt/c/Programming/Thesis/veriopt-dev/isabelle"], "include_sessions": ["OptimizationDSL"], "verbose": true }

NOTE, For fresh Isabelle:

- Need to build fresh HOL-Library:

```
session_build { "session": "HOL-Library", "options": ["document=false"], "dirs": [], "include_sessions": [], "verbose": true }
```

- Need to install `lualatex` package (due to `Graph` session), may be able to skip it after refactoring Veriopt.

1. Canonicalization: `session_build`

```json
{
    "session": "Canoncizalizations",
    "options": ["document=false", "show_question_marks=false", "quick_and_dirty"],
    "dirs": ["."],
    "include_sessions": ["OptimizationDSL"],
    "verbose": true
}
```

3. For each user: `session_start`

```json
{
    "session": "Canoncizalizations",
    "options": ["document=false", "show_question_marks=false", "quick_and_dirty"],
    "dirs": ["."],
    "include_sessions": ["OptimizationDSL"],
    "verbose": true
}
```

See log for full output.

session_start { "session": "Canonicalizations", "options": ["document=false", "show_question_marks=false", "quick_and_dirty"], "dirs": ["/mnt/c/Programming/Thesis/veriopt-dev/isabelle"], "include_sessions": ["OptimizationDSL"], "verbose": true }

```json
FINISHED 
{
    "session_id":"486f31f0-b28a-419e-ad84-d4d96943dcab",
    "tmp_dir":"/tmp/isabelle-achmad.afriza/server_session9749566282363746727",
    "task":"ef2a1f79-4dc3-4baf-86f1-1ee9a0786f9b"
}
```

Note: 
- why is it faster?
- `session_id` are tied to the server, not the client. Different clients can use the `session_id` from other client in a single server.

TODO:

- check to see if used without `session_build` first is slower.
- how to specify `tmp_dir`?

4. Copy/Build templated theory file into `tmp_dir`
5. Use theories for optimization rule checks: `use_theories`

```json
{
    "session_id": "486f31f0-b28a-419e-ad84-d4d96943dcab", 
    "theories": ["OptRule"], 
    "master_dir": "./Veritest/" // ???
}
```

use_theory { "session_id": "486f31f0-b28a-419e-ad84-d4d96943dcab", "theories": ["OptRule"] }

Note:
- When 1 client is used by multiple sessions, messages *can* be interleaved, but each message is synchronized to a client.

Result:

```json
...
NOTE 
{
    "percentage":27,
    "task":"ebd722e1-aea6-43e7-9cf0-c6ada24d96b1",
    "message":"theory Graph.StampLattice 27%","kind":"writeln",
    "session":"",
    "theory":"Graph.StampLattice"
}
...

FINISHED
{
    "ok": true,
    "errors": [],
    "nodes": [
        {
            "messages": [
                {
                    "kind": "writeln",
                    "message": "instantiation\n  void :: order\n  less_eq_void == less_eq :: void \\<Rightarrow> void \\<Rightarrow> bool\n  less_void == less :: void \\<Rightarrow> void \\<Rightarrow> bool",
                    "pos": {
                        "line": 17,
                        "offset": 295,
                        "end_offset": 308,
                        "file": "/mnt/c/Programming/Thesis/veriopt-dev/isabelle/Graph/StampLattice.thy"
                    }
                },
                {
                    "kind": "writeln",
                    "message": "consts\n  less_eq_void :: \"void \\<Rightarrow> void \\<Rightarrow> bool\"",
                    "pos": {
                        "line": 20,
                        "offset": 330,
                        "end_offset": 340,
                        "file": "/mnt/c/Programming/Thesis/veriopt-dev/isabelle/Graph/StampLattice.thy"
                    }
                },
                {
                    "kind": "writeln",
                    "message": "consts\n  less_void :: \"void \\<Rightarrow> void \\<Rightarrow> bool\"",
                    "pos": {
                        "line": 23,
                        "offset": 413,
                        "end_offset": 423,
                        "file": "/mnt/c/Programming/Thesis/veriopt-dev/isabelle/Graph/StampLattice.thy"
                    }
                },
                ...
                {
                    "kind": "writeln",
                    "message": "consts\n  bot_Stamp :: \"Stamp\"",
                    "pos": {
                        "line": 1027,
                        "offset": 29837,
                        "end_offset": 29847,
                        "file": "/mnt/c/Programming/Thesis/veriopt-dev/isabelle/Graph/StampLattice.thy"
                    }
                },
                {
                    "kind": "warning",
                    "message": "Projection as head in equation, in theorem:\nRep_intstamp (from_bounds (l::'a word, u::'a word)) \\<equiv> (l, u)",
                    "pos": {
                        "line": 1039,
                        "offset": 30195,
                        "end_offset": 30200,
                        "file": "/mnt/c/Programming/Thesis/veriopt-dev/isabelle/Graph/StampLattice.thy"
                    }
                },
                {
                    "kind": "writeln",
                    "message": "theorem Rep_intstamp (from_bounds (l::'a word, u::'a word)) = (l, u)",
                    "pos": {
                        "line": 1039,
                        "offset": 30195,
                        "end_offset": 30200,
                        "file": "/mnt/c/Programming/Thesis/veriopt-dev/isabelle/Graph/StampLattice.thy"
                    }
                }
            ],
            "exports": [],
            "status": {
                "percentage": 100,
                "unprocessed": 0,
                "running": 0,
                "finished": 1412,
                "failed": 0,
                "total": 1417,
                "consolidated": true,
                "canceled": false,
                "ok": true,
                "warned": 5
            },
            "theory_name": "Graph.StampLattice",
            "node_name": "/mnt/c/Programming/Thesis/veriopt-dev/isabelle/Graph/StampLattice.thy"
        }
    ],
    "task": "ebd722e1-aea6-43e7-9cf0-c6ada24d96b1"
}
```

Malformed Optimization Rule:

```json
FINISHED {
   "ok":false,
   "errors":[
      {
         "kind":"error",
         "message":"Inner syntax error\\<^here>\nFailed to parse term",
         "pos":{
            "line":11,
            "offset":138,
            "end_offset":150,
            "file":"/tmp/isabelle-achmadafriza/server_session12537762595186768507/MalformedOne.thy"
         }
      },
      {
         "kind":"error",
         "message":"Bad context for command \".\"\\<^here> -- using reset state",
         "pos":{
            "line":12,
            "offset":183,
            "end_offset":184,
            "file":"/tmp/isabelle-achmadafriza/server_session12537762595186768507/MalformedOne.thy"
         }
      }
   ],
   "nodes":[
      {
         "messages":[
            {
               "kind":"writeln",
               "message":"phase: MalformedNode\n  trm: Canonicalization.size\n  rules:",
               "pos":{
                  "line":7,
                  "offset":92,
                  "end_offset":97,
                  "file":"/tmp/isabelle-achmadafriza/server_session12537762595186768507/MalformedOne.thy"
               }
            },
            {
               "kind":"error",
               "message":"Inner syntax error\\<^here>\nFailed to parse term",
               "pos":{
                  "line":11,
                  "offset":138,
                  "end_offset":150,
                  "file":"/tmp/isabelle-achmadafriza/server_session12537762595186768507/MalformedOne.thy"
               }
            },
            {
               "kind":"error",
               "message":"Bad context for command \".\"\\<^here> -- using reset state",
               "pos":{
                  "line":12,
                  "offset":183,
                  "end_offset":184,
                  "file":"/tmp/isabelle-achmadafriza/server_session12537762595186768507/MalformedOne.thy"
               }
            }
         ],
         "exports":[
            
         ],
         "status":{
            "percentage":100,
            "unprocessed":0,
            "running":0,
            "finished":10,
            "failed":2,
            "total":12,
            "consolidated":true,
            "canceled":false,
            "ok":false,
            "warned":0
         },
         "theory_name":"Draft.MalformedOne",
         "node_name":"/tmp/isabelle-achmadafriza/server_session12537762595186768507/MalformedOne.thy"
      }
   ],
   "task":"f15d3215-7b64-42a3-8f22-b6ef3377e90b"
}
```

For an Optimization Rule, Sledgehammer test:

- No proof found:

```json
NOTE {"percentage":8,"task":"6de717ce-8783-4199-8b31-9a87ad85d6dd","message":"theory Draft.SledgeTest 8%","kind":"writeln","session":"","theory":"Draft.SledgeTest"}
NOTE {"percentage":17,"task":"6de717ce-8783-4199-8b31-9a87ad85d6dd","message":"theory Draft.SledgeTest 17%","kind":"writeln","session":"","theory":"Draft.SledgeTest"}
NOTE {"percentage":52,"task":"6de717ce-8783-4199-8b31-9a87ad85d6dd","message":"theory Draft.SledgeTest 52%","kind":"writeln","session":"","theory":"Draft.SledgeTest"}
NOTE {"percentage":99,"task":"6de717ce-8783-4199-8b31-9a87ad85d6dd","message":"theory Draft.SledgeTest 99%","kind":"writeln","session":"","theory":"Draft.SledgeTest"}
NOTE {"percentage":100,"task":"6de717ce-8783-4199-8b31-9a87ad85d6dd","message":"theory Draft.SledgeTest 100%","kind":"writeln","session":"","theory":"Draft.SledgeTest"}
FINISHED 
{
    "ok": true,
    "errors": [],
    "nodes": [
        {
            "messages": [
                {
                    "kind": "writeln",
                    "message": "consts\n  isBoolean :: \"IRExpr \\<Rightarrow> bool\"",
                    "pos": {
                        "line": 9,
                        "offset": 182,
                        "end_offset": 192,
                        "file": "/tmp/isabelle-achmad.afriza/server_session6210207076139024994/SledgeTest.thy"
                    }
                },
                {
                    "kind": "writeln",
                    "message": "phase: ConditionalNode\n  trm: Canonicalization.size\n  rules:",
                    "pos": {
                        "line": 15,
                        "offset": 462,
                        "end_offset": 467,
                        "file": "/tmp/isabelle-achmad.afriza/server_session6210207076139024994/SledgeTest.thy"
                    }
                },
                {
                    "kind": "writeln",
                    "message": "Sledgehammering...",
                    "pos": {
                        "line": 27,
                        "offset": 868,
                        "end_offset": 880,
                        "file": "/tmp/isabelle-achmad.afriza/server_session6210207076139024994/SledgeTest.thy"
                    }
                },
                {
                    "kind": "writeln",
                    "message": "No proof found",
                    "pos": {
                        "line": 27,
                        "offset": 868,
                        "end_offset": 880,
                        "file": "/tmp/isabelle-achmad.afriza/server_session6210207076139024994/SledgeTest.thy"
                    }
                }
            ],
            "exports": [],
            "status": {
                "percentage": 100,
                "unprocessed": 0,
                "running": 0,
                "finished": 23,
                "failed": 0,
                "total": 23,
                "consolidated": true,
                "canceled": false,
                "ok": true,
                "warned": 0
            },
            "theory_name": "Draft.SledgeTest",
            "node_name": "/tmp/isabelle-achmad.afriza/server_session6210207076139024994/SledgeTest.thy"
        }
    ],
    "task": "6de717ce-8783-4199-8b31-9a87ad85d6dd"
}
```

- Auto:

```json
FINISHED {
   "ok":true,
   "errors":[
      
   ],
   "nodes":[
      {
         "messages":[
            {
               "kind":"writeln",
               "message":"phase: AutoNode\n  trm: Canonicalization.size\n  rules:",
               "pos":{
                  "line":7,
                  "offset":84,
                  "end_offset":89,
                  "file":"/tmp/isabelle-achmadafriza/server_session8705711287820245737/Auto.thy"
               }
            }
         ],
         "exports":[
            
         ],
         "status":{
            "percentage":100,
            "unprocessed":0,
            "running":0,
            "finished":12,
            "failed":0,
            "total":12,
            "consolidated":true,
            "canceled":false,
            "ok":true,
            "warned":0
         },
         "theory_name":"Draft.Auto",
         "node_name":"/tmp/isabelle-achmadafriza/server_session8705711287820245737/Auto.thy"
      }
   ],
   "task":"cdf8bb7d-c46d-4492-8b33-a8851f0863b0"
}
```

- AutoFail:

```json
FINISHED {
   "ok":false,
   "errors":[
      {
         "kind":"error",
         "message":"Failed to finish proof:\ngoal (2 subgoals):\n 1. Canonicalization.size (BinaryExpr BinSub x e)\n    < Suc (Suc (Suc (Suc (Canonicalization.size x +\n                          Canonicalization.size e))))\n 2. BinaryExpr BinAdd x (UnaryExpr UnaryNeg e) \\<sqsupseteq> BinaryExpr BinSub x e\nvariables:\n    e, x :: IRExpr",
         "pos":{
            "line":12,
            "offset":177,
            "end_offset":178,
            "file":"/tmp/isabelle-achmadafriza/server_session2909135338398421130/AutoFail.thy"
         }
      }
   ],
   "nodes":[
      {
         "messages":[
            {
               "kind":"writeln",
               "message":"phase: AutoFailNode\n  trm: Canonicalization.size\n  rules:",
               "pos":{
                  "line":7,
                  "offset":88,
                  "end_offset":93,
                  "file":"/tmp/isabelle-achmadafriza/server_session2909135338398421130/AutoFail.thy"
               }
            },
            {
               "kind":"error",
               "message":"Failed to finish proof:\ngoal (2 subgoals):\n 1. Canonicalization.size (BinaryExpr BinSub x e)\n    < Suc (Suc (Suc (Suc (Canonicalization.size x +\n                          Canonicalization.size e))))\n 2. BinaryExpr BinAdd x (UnaryExpr UnaryNeg e) \\<sqsupseteq> BinaryExpr BinSub x e\nvariables:\n    e, x :: IRExpr",
               "pos":{
                  "line":12,
                  "offset":177,
                  "end_offset":178,
                  "file":"/tmp/isabelle-achmadafriza/server_session2909135338398421130/AutoFail.thy"
               }
            }
         ],
         "exports":[
            
         ],
         "status":{
            "percentage":100,
            "unprocessed":0,
            "running":0,
            "finished":13,
            "failed":1,
            "total":14,
            "consolidated":true,
            "canceled":false,
            "ok":false,
            "warned":0
         },
         "theory_name":"Draft.AutoFail",
         "node_name":"/tmp/isabelle-achmadafriza/server_session2909135338398421130/AutoFail.thy"
      }
   ],
   "task":"a9d6c028-49c8-4b49-9cda-42d0b191fbe0"
}
```

- SledgeHammer-1:

```json
FINISHED {
   "ok":true,
   "errors":[
      
   ],
   "nodes":[
      {
         "messages":[
            {
               "kind":"writeln",
               "message":"phase: SledgeStageTwoNode\n  trm: Canonicalization.size\n  rules:",
               "pos":{
                  "line":7,
                  "offset":86,
                  "end_offset":91,
                  "file":"/tmp/isabelle-achmadafriza/server_session14993555341281912068/Sledge.thy"
               }
            },
            {
               "kind":"writeln",
               "message":"Sledgehammering...",
               "pos":{
                  "line":12,
                  "offset":189,
                  "end_offset":201,
                  "file":"/tmp/isabelle-achmadafriza/server_session14993555341281912068/Sledge.thy"
               }
            },
            {
               "kind":"writeln",
               "message":"e found a proof...",
               "pos":{
                  "line":12,
                  "offset":189,
                  "end_offset":201,
                  "file":"/tmp/isabelle-achmadafriza/server_session14993555341281912068/Sledge.thy"
               }
            },
            {
               "kind":"writeln",
               "message":"verit found a proof...",
               "pos":{
                  "line":12,
                  "offset":189,
                  "end_offset":201,
                  "file":"/tmp/isabelle-achmadafriza/server_session14993555341281912068/Sledge.thy"
               }
            },
            {
               "kind":"writeln",
               "message":"cvc4 found a proof...",
               "pos":{
                  "line":12,
                  "offset":189,
                  "end_offset":201,
                  "file":"/tmp/isabelle-achmadafriza/server_session14993555341281912068/Sledge.thy"
               }
            },
            {
               "kind":"writeln",
               "message":"spass found a proof...",
               "pos":{
                  "line":12,
                  "offset":189,
                  "end_offset":201,
                  "file":"/tmp/isabelle-achmadafriza/server_session14993555341281912068/Sledge.thy"
               }
            },
            {
               "kind":"writeln",
               "message":"zipperposition found a proof...",
               "pos":{
                  "line":12,
                  "offset":189,
                  "end_offset":201,
                  "file":"/tmp/isabelle-achmadafriza/server_session14993555341281912068/Sledge.thy"
               }
            },
            {
               "kind":"writeln",
               "message":"vampire found a proof...",
               "pos":{
                  "line":12,
                  "offset":189,
                  "end_offset":201,
                  "file":"/tmp/isabelle-achmadafriza/server_session14993555341281912068/Sledge.thy"
               }
            },
            {
               "kind":"writeln",
               "message":"zipperposition: Try this: apply (simp add: less_SucI size_binary_rhs trans_less_add2) (0.9 ms)",
               "pos":{
                  "line":12,
                  "offset":189,
                  "end_offset":201,
                  "file":"/tmp/isabelle-achmadafriza/server_session14993555341281912068/Sledge.thy"
               }
            },
            {
               "kind":"writeln",
               "message":"zipperposition found a proof...",
               "pos":{
                  "line":12,
                  "offset":189,
                  "end_offset":201,
                  "file":"/tmp/isabelle-achmadafriza/server_session14993555341281912068/Sledge.thy"
               }
            },
            {
               "kind":"writeln",
               "message":"spass: Try this: apply (simp add: less_Suc_eq size_binary_rhs trans_less_add2) (11 ms)",
               "pos":{
                  "line":12,
                  "offset":189,
                  "end_offset":201,
                  "file":"/tmp/isabelle-achmadafriza/server_session14993555341281912068/Sledge.thy"
               }
            },
            {
               "kind":"writeln",
               "message":"cvc4: Found duplicate proof",
               "pos":{
                  "line":12,
                  "offset":189,
                  "end_offset":201,
                  "file":"/tmp/isabelle-achmadafriza/server_session14993555341281912068/Sledge.thy"
               }
            },
            {
               "kind":"writeln",
               "message":"zipperposition: Found duplicate proof",
               "pos":{
                  "line":12,
                  "offset":189,
                  "end_offset":201,
                  "file":"/tmp/isabelle-achmadafriza/server_session14993555341281912068/Sledge.thy"
               }
            },
            {
               "kind":"writeln",
               "message":"e: Try this: apply (metis Suc_eq_plus1 add.commute size_binary_rhs trans_less_add2) (8 ms)",
               "pos":{
                  "line":12,
                  "offset":189,
                  "end_offset":201,
                  "file":"/tmp/isabelle-achmadafriza/server_session14993555341281912068/Sledge.thy"
               }
            },
            {
               "kind":"writeln",
               "message":"verit: Found duplicate proof",
               "pos":{
                  "line":12,
                  "offset":189,
                  "end_offset":201,
                  "file":"/tmp/isabelle-achmadafriza/server_session14993555341281912068/Sledge.thy"
               }
            },
            {
               "kind":"writeln",
               "message":"vampire: Try this: apply (simp add: less_SucI pos_add_strict size_binary_rhs size_pos) (0.7 ms)",
               "pos":{
                  "line":12,
                  "offset":189,
                  "end_offset":201,
                  "file":"/tmp/isabelle-achmadafriza/server_session14993555341281912068/Sledge.thy"
               }
            },
            {
               "kind":"writeln",
               "message":"Done",
               "pos":{
                  "line":12,
                  "offset":189,
                  "end_offset":201,
                  "file":"/tmp/isabelle-achmadafriza/server_session14993555341281912068/Sledge.thy"
               }
            }
         ],
         "exports":[
            
         ],
         "status":{
            "percentage":100,
            "unprocessed":0,
            "running":0,
            "finished":13,
            "failed":0,
            "total":13,
            "consolidated":true,
            "canceled":false,
            "ok":true,
            "warned":0
         },
         "theory_name":"Draft.Sledge",
         "node_name":"/tmp/isabelle-achmadafriza/server_session14993555341281912068/Sledge.thy"
      }
   ],
   "task":"121b90f5-7c70-434a-bf5f-931517524ea0"
}
```

- SledgeFail:

```json
FINISHED {
   "ok":true,
   "errors":[
      
   ],
   "nodes":[
      {
         "messages":[
            {
               "kind":"writeln",
               "message":"consts\n  isBoolean :: \"IRExpr \\<Rightarrow> bool\"",
               "pos":{
                  "line":8,
                  "offset":119,
                  "end_offset":129,
                  "file":"/tmp/isabelle-achmadafriza/server_session16428751108126897833/SledgeFail.thy"
               }
            },
            {
               "kind":"writeln",
               "message":"phase: SledgeFailNode\n  trm: Canonicalization.size\n  rules:",
               "pos":{
                  "line":11,
                  "offset":256,
                  "end_offset":261,
                  "file":"/tmp/isabelle-achmadafriza/server_session16428751108126897833/SledgeFail.thy"
               }
            },
            {
               "kind":"writeln",
               "message":"Sledgehammering...",
               "pos":{
                  "line":17,
                  "offset":440,
                  "end_offset":452,
                  "file":"/tmp/isabelle-achmadafriza/server_session16428751108126897833/SledgeFail.thy"
               }
            },
            {
               "kind":"writeln",
               "message":"No proof found",
               "pos":{
                  "line":17,
                  "offset":440,
                  "end_offset":452,
                  "file":"/tmp/isabelle-achmadafriza/server_session16428751108126897833/SledgeFail.thy"
               }
            }
         ],
         "exports":[
            
         ],
         "status":{
            "percentage":100,
            "unprocessed":0,
            "running":0,
            "finished":18,
            "failed":0,
            "total":18,
            "consolidated":true,
            "canceled":false,
            "ok":true,
            "warned":0
         },
         "theory_name":"Draft.SledgeFail",
         "node_name":"/tmp/isabelle-achmadafriza/server_session16428751108126897833/SledgeFail.thy"
      }
   ],
   "task":"862540d4-bda0-4d23-8d0d-4e6c2f024351"
}
```

- SledgeHammer-2:

```json
FINISHED {
   "ok":true,
   "errors":[
      
   ],
   "nodes":[
      {
         "messages":[
            {
               "kind":"writeln",
               "message":"phase: SledgeStageTwoNode\n  trm: Canonicalization.size\n  rules:",
               "pos":{
                  "line":7,
                  "offset":94,
                  "end_offset":99,
                  "file":"/tmp/isabelle-achmadafriza/server_session5310811877369891935/SledgeStageTwo.thy"
               }
            },
            {
               "kind":"writeln",
               "message":"Sledgehammering...",
               "pos":{
                  "line":13,
                  "offset":260,
                  "end_offset":272,
                  "file":"/tmp/isabelle-achmadafriza/server_session5310811877369891935/SledgeStageTwo.thy"
               }
            },
            {
               "kind":"writeln",
               "message":"No proof found",
               "pos":{
                  "line":13,
                  "offset":260,
                  "end_offset":272,
                  "file":"/tmp/isabelle-achmadafriza/server_session5310811877369891935/SledgeStageTwo.thy"
               }
            }
         ],
         "exports":[
            
         ],
         "status":{
            "percentage":100,
            "unprocessed":0,
            "running":0,
            "finished":15,
            "failed":0,
            "total":15,
            "consolidated":true,
            "canceled":false,
            "ok":true,
            "warned":0
         },
         "theory_name":"Draft.SledgeStageTwo",
         "node_name":"/tmp/isabelle-achmadafriza/server_session5310811877369891935/SledgeStageTwo.thy"
      }
   ],
   "task":"3c86b173-dd5d-431f-bc3c-4c8c5eafb8fa"
}
```

Sledgehammer Done:

```json
FINISHED {
   "ok":true,
   "errors":[
      
   ],
   "nodes":[
      {
         "messages":[
            {
               "kind":"writeln",
               "message":"phase: SledgeDoneNode\n  trm: Canonicalization.size\n  rules:",
               "pos":{
                  "line":7,
                  "offset":90,
                  "end_offset":95,
                  "file":"/tmp/isabelle-achmadafriza/server_session9144700765802995366/SledgeDone.thy"
               }
            },
            {
               "kind":"writeln",
               "message":"theorem\n  val_and_nots:\n    intval_and (intval_not (x::Value)) (intval_not (y::Value)) =\n    intval_not (intval_or x y)",
               "pos":{
                  "line":13,
                  "offset":192,
                  "end_offset":194,
                  "file":"/tmp/isabelle-achmadafriza/server_session9144700765802995366/SledgeDone.thy"
               }
            },
            {
               "kind":"writeln",
               "message":"theorem\n  exp_and_nots:\n    BinaryExpr BinAnd (UnaryExpr UnaryNot (x::IRExpr))\n     (UnaryExpr UnaryNot (y::IRExpr)) \\<sqsupseteq>\n    UnaryExpr UnaryNot (BinaryExpr BinOr x y)",
               "pos":{
                  "line":17,
                  "offset":324,
                  "end_offset":326,
                  "file":"/tmp/isabelle-achmadafriza/server_session9144700765802995366/SledgeDone.thy"
               }
            },
            {
               "kind":"warning",
               "message":"No proof state",
               "pos":{
                  "line":22,
                  "offset":515,
                  "end_offset":527,
                  "file":"/tmp/isabelle-achmadafriza/server_session9144700765802995366/SledgeDone.thy"
               }
            }
         ],
         "exports":[
            
         ],
         "status":{
            "percentage":100,
            "unprocessed":0,
            "running":0,
            "finished":26,
            "failed":0,
            "total":27,
            "consolidated":true,
            "canceled":false,
            "ok":true,
            "warned":1
         },
         "theory_name":"Draft.SledgeDone",
         "node_name":"/tmp/isabelle-achmadafriza/server_session9144700765802995366/SledgeDone.thy"
      }
   ],
   "task":"dbfbeb57-6a86-4b78-8cec-f13dbeb41e11"
}
```

Nitpick Test:

- Found Counterexample:

```json
NOTE {"percentage":13,"task":"5ee9d336-86e9-425d-b8a3-df4a62e09509","message":"theory Draft.NitpickTest 13%","kind":"writeln","session":"","theory":"Draft.NitpickTest"}
NOTE {"percentage":40,"task":"5ee9d336-86e9-425d-b8a3-df4a62e09509","message":"theory Draft.NitpickTest 40%","kind":"writeln","session":"","theory":"Draft.NitpickTest"}
NOTE {"percentage":99,"task":"5ee9d336-86e9-425d-b8a3-df4a62e09509","message":"theory Draft.NitpickTest 99%","kind":"writeln","session":"","theory":"Draft.NitpickTest"}
NOTE {"percentage":100,"task":"5ee9d336-86e9-425d-b8a3-df4a62e09509","message":"theory Draft.NitpickTest 100%","kind":"writeln","session":"","theory":"Draft.NitpickTest"}
FINISHED 
{
    "ok": true,
    "errors": [],
    "nodes": [
        {
            "messages": [
                {
                    "kind": "writeln",
                    "message": "phase: ConditionalNode\n  trm: Canonicalization.size\n  rules:",
                    "pos": {
                        "line": 10,
                        "offset": 296,
                        "end_offset": 301,
                        "file": "/tmp/isabelle-achmad.afriza/server_session6210207076139024994/NitpickTest.thy"
                    }
                },
                {
                    "kind": "writeln",
                    "message": "Nitpicking formula...",
                    "pos": {
                        "line": 16,
                        "offset": 435,
                        "end_offset": 442,
                        "file": "/tmp/isabelle-achmad.afriza/server_session6210207076139024994/NitpickTest.thy"
                    }
                },
                {
                    "kind": "writeln",
                    "message": "Nitpick found a counterexample:\n  Free variables:\n    x::IRExpr = ConstantExpr V\\<^sub>1\n    y::IRExpr = ConstantExpr V\\<^sub>2",
                    "pos": {
                        "line": 16,
                        "offset": 435,
                        "end_offset": 442,
                        "file": "/tmp/isabelle-achmad.afriza/server_session6210207076139024994/NitpickTest.thy"
                    }
                }
            ],
            "exports": [],
            "status": {
                "percentage": 100,
                "unprocessed": 0,
                "running": 0,
                "finished": 15,
                "failed": 0,
                "total": 15,
                "consolidated": true,
                "canceled": false,
                "ok": true,
                "warned": 0
            },
            "theory_name": "Draft.NitpickTest",
            "node_name": "/tmp/isabelle-achmad.afriza/server_session6210207076139024994/NitpickTest.thy"
        }
    ],
    "task": "5ee9d336-86e9-425d-b8a3-df4a62e09509"
}
```

- No Counterexample:

```json
FINISHED {
   "ok":true,
   "errors":[
      
   ],
   "nodes":[
      {
         "messages":[
            {
               "kind":"writeln",
               "message":"phase: NitpickFailNode\n  trm: Canonicalization.size\n  rules:",
               "pos":{
                  "line":7,
                  "offset":91,
                  "end_offset":96,
                  "file":"/tmp/isabelle-achmadafriza/server_session1163477979405991358/NitpickFail.thy"
               }
            },
            {
               "kind":"writeln",
               "message":"Nitpicking formula...",
               "pos":{
                  "line":12,
                  "offset":191,
                  "end_offset":198,
                  "file":"/tmp/isabelle-achmadafriza/server_session1163477979405991358/NitpickFail.thy"
               }
            },
            {
               "kind":"writeln",
               "message":"Nitpick checked 21 of 55 scopes",
               "pos":{
                  "line":12,
                  "offset":191,
                  "end_offset":198,
                  "file":"/tmp/isabelle-achmadafriza/server_session1163477979405991358/NitpickFail.thy"
               }
            }
         ],
         "exports":[
            
         ],
         "status":{
            "percentage":100,
            "unprocessed":0,
            "running":0,
            "finished":13,
            "failed":0,
            "total":13,
            "consolidated":true,
            "canceled":false,
            "ok":true,
            "warned":0
         },
         "theory_name":"Draft.NitpickFail",
         "node_name":"/tmp/isabelle-achmadafriza/server_session1163477979405991358/NitpickFail.thy"
      }
   ],
   "task":"32e642f5-1f3d-4430-b141-903384510dac"
}
```

TODO: pretty print use_theory

FINISHED {"session_id":"94986ab9-740d-40a5-8a4f-b5e5e949cc24","tmp_dir":"/tmp/isabelle-achmad.afriza/server_session15679256825508530927","task":"48ab0f21-a2d6-4f57-80ef-fc7013b324fd"}

use_theories { "session_id": "94986ab9-740d-40a5-8a4f-b5e5e949cc24", "theories": ["./Veritest/test/sledgehammer`/SledgeStageTwo"] }
use_theories { "session_id": "94986ab9-740d-40a5-8a4f-b5e5e949cc24", "theories": ["SledgeTest"] }
/mnt/c/Programming/Thesis/veriopt-dev/isabelle/Veritest/Afriza.thy

use_theories { "session_id": "94986ab9-740d-40a5-8a4f-b5e5e949cc24", "theories": ["/mnt/c/Programming/Thesis/veriopt-dev/isabelle/Veritest/Afriza.thy"] }

1. User request done, `session_stop`

```json
{ "session_id": "94986ab9-740d-40a5-8a4f-b5e5e949cc24" }
```

if not, `purge_theories`

```json
{ "session_id": "94986ab9-740d-40a5-8a4f-b5e5e949cc24", "theories": ["./Veritest/test/sledgehammer/Sledge"] }
```

```json
{ "session_id": "94986ab9-740d-40a5-8a4f-b5e5e949cc24", "all": true }
```