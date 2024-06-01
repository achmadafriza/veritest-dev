# Reproduction Steps

Built on Isabelle2023

## Steps

1. Start Isabelle Server - Client

```isabelle server```

Response:

```
server "isabelle" = 127.0.0.1:43189 (password "0aaaaea2-849b-4458-8812-0945b426e81c")
OK {"isabelle_id":"b5f3d1051b13","isabelle_name":"Isabelle2023"}
```

On a new terminal:

```isabelle client -n isabelle```

Check to see if its already connected by:

```
echo 1
```

2. Build used sessions

Start on `./veriopt-dev/isabelle`

Argument payload:

```json
{
    "session": "OptimizationDSL",
    "options": ["document=false", "show_question_marks=false", "quick_and_dirty"],
    "dirs": ["."],
    "include_sessions": ["HOL-Library", "Graph", "Semantics", "Proofs"],
    "verbose": true
}
```

Copy this:

```
session_build { "session": "OptimizationDSL", "options": ["document=false", "show_question_marks=false", "quick_and_dirty"], "dirs": ["."], "include_sessions": ["HOL-Library", "Graph", "Semantics", "Proofs"], "verbose": true }
```

Continue after getting:

```
FINISHED {...}
```

3. Start a `Canonicalizations` session

Argument payload:

```json
{
    "session": "Canoncizalizations",
    "options": ["document=false", "show_question_marks=false", "quick_and_dirty"],
    "dirs": ["."],
    "include_sessions": ["OptimizationDSL"],
    "verbose": true
}
```

Copy this:

```
session_start { "session": "Canonicalizations", "options": ["document=false", "show_question_marks=false", "quick_and_dirty"], "dirs": ["."], "include_sessions": ["OptimizationDSL"], "verbose": true }
```

Result:

```json
NOTE {...}
...
FINISHED 
{
    "session_id":"486f31f0-b28a-419e-ad84-d4d96943dcab", // Copy This
    "tmp_dir":"/tmp/isabelle-achmad.afriza/server_session9749566282363746727", // Remember this
    "task":"ef2a1f79-4dc3-4baf-86f1-1ee9a0786f9b"
}
```

1. Copy the theory files to be tested on `tmp_dir`

```
cp -r ./ /tmp/isabelle-achmad.afriza/server_session9749566282363746727/
```

5. Send the theory files to be tested on Isabelle

Argument payload:

```json
{
    "session_id": "94986ab9-740d-40a5-8a4f-b5e5e949cc24", // Paste the session_id here
    "theories": ["./Veritest/test/sledgehammer/SledgeStageTwo"] // refer to the theory files here
}
```

Copy This:

```
use_theories { "session_id": "9e716396-ff38-414a-a911-947ede0ab357", "theories": ["./Veritest/test/sledgehammer/SledgeStageTwo"] }
```

`SledgeStageTwo` refers to `SledgeStageTwo.thy`.

You should see the results of the process along the lines of:

```json
NOTE {...}
...
FINISHED 
{
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
                  "file":"/tmp/isabelle-achmad.afriza/server_session18082195551759789780/Veritest/test/sledgehammer/SledgeStageTwo.thy"
               }
            },
            {
               "kind":"writeln",
               "message":"Sledgehammering...",
               "pos":{
                  "line":13,
                  "offset":260,
                  "end_offset":272,
                  "file":"/tmp/isabelle-achmad.afriza/server_session18082195551759789780/Veritest/test/sledgehammer/SledgeStageTwo.thy"
               }
            },
            {
               "kind":"writeln",
               "message":"No proof found",
               "pos":{
                  "line":13,
                  "offset":260,
                  "end_offset":272,
                  "file":"/tmp/isabelle-achmad.afriza/server_session18082195551759789780/Veritest/test/sledgehammer/SledgeStageTwo.thy"
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
         "node_name":"/tmp/isabelle-achmad.afriza/server_session18082195551759789780/Veritest/test/sledgehammer/SledgeStageTwo.thy"
      }
   ],
   "task":"da9605f0-5b09-47ea-b5c0-93f781eaa99e"
}
```

The errors will be shown on `errors`. `line` and `offset` will refer to the line and column of the theory file.

6. Purge the theory after processing

```
purge_theories { "session_id": "8a575525-2745-4698-84b9-41fd10a605e1", "all": true }
```

7. Reuse the theory after purging (Do step 5 again)

Note:

- Isabelle will complain if the argument is not to its format. Check if the JSON is wellformed.
- Remember to copy the files to the `temp_dir`
- To stop the session

```
session_stop { "session_id": "8a575525-2745-4698-84b9-41fd10a605e1" }
```

- It seems that purging the theories doesn't exactly purge the DSL used. Reusing the theory instead concatenates the theory to existing cached? theory or built `OptimizationDSL` session, resulting in `error.log`. It *may* be a bug on the DSL, since calling `purge_theories` should remove the theory entirely.

From the docs (Sec. 4.4.9):

> The `purge_theories` command updates the identified session by removing theories that are no longer required: theories that are used in pending `use_theories` tasks or imported by other theories are retained.

- `error.log` has all of the output for a 1st, 2nd, and 3rd run of using and purging a theory.
