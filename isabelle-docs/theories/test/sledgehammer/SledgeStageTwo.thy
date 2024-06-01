theory SledgeStageTwo
  imports
    Canonicalizations.Common
    Proofs.StampEvalThms
begin

phase SledgeStageTwoNode
  terminating size
begin

optimization SubThenSubLeft: "(x - (x - y)) \<longmapsto> y"
  apply (simp add: less_Suc_eq size_binary_rhs trans_less_add2)
  sledgehammer
  sorry

end

end