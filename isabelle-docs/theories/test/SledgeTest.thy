(* Each theory file starts with a header like the one below *)
theory SledgeTest
  imports
    Canonicalizations.Common
    Proofs.StampEvalThms
begin

(* Ignore this definition *)
definition isBoolean :: "IRExpr \<Rightarrow> bool" where
  "isBoolean e = (\<forall>m p cond. (([m,p] \<turnstile> e \<mapsto> cond) \<longrightarrow> (cond \<in> {IntVal 32 0, IntVal 32 1})))"


(* Use of the optimization keyword is restricted to within a phase *)
(* We start a phase and provide a pre-made termination measure, size *)
phase ConditionalNode
  terminating size
begin

(* An optimization that requires no further proof *)
optimization Auto: "(true ? x : y) \<longmapsto> x"
  .

(* An optimization that sledgehammer can produce *)
(* The sledgehammer command is normally removed after the tactic is included in the theory file *)
optimization SemiAuto: "(c ? true : false) \<longmapsto> c
                                          when isBoolean c"
  sledgehammer
  using empty_iff insert_iff isBoolean_def by fastforce

optimization Sledge: "x + -e \<longmapsto> x - e"
  apply (metis add_2_eq_Suc' less_SucI less_add_Suc1 not_less_eq size_binary_const size_non_add)
  sledgehammer
  sorry

end

end
