theory SledgeFail
  imports
    Canonicalizations.Common
    Proofs.StampEvalThms
begin

(* Ignore this definition *)
definition isBoolean :: "IRExpr \<Rightarrow> bool" where
  "isBoolean e = (\<forall>m p cond. (([m,p] \<turnstile> e \<mapsto> cond) \<longrightarrow> (cond \<in> {IntVal 32 0, IntVal 32 1})))"

phase SledgeFailNode
  terminating size
begin

optimization Sledge: "x + -e \<longmapsto> x - e"
  apply (metis add_2_eq_Suc' less_SucI less_add_Suc1 not_less_eq size_binary_const size_non_add)
  sledgehammer
  sorry

end

end
