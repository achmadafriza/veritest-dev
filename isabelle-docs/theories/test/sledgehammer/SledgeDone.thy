theory SledgeDone
  imports
    Canonicalizations.Common
    Proofs.StampEvalThms
begin

phase SledgeDoneNode
  terminating size
begin

lemma val_and_nots:
  "val[~x & ~y] = val[~(x | y)]"
  by (cases x; cases y; auto simp: take_bit_not_take_bit)

lemma exp_and_nots:
  "exp[~x & ~y] \<ge> exp[~(x | y)]"
   using val_and_nots by force

optimization AndNotsSledge: "(~x) & (~y) \<longmapsto> ~(x | y)"
  apply (metis add_2_eq_Suc' less_SucI less_add_Suc1 not_less_eq size_binary_const size_non_add)
  using exp_and_nots by auto
  sledgehammer

end

end