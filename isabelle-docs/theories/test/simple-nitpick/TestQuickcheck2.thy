theory TestQuickcheck2
  imports
    Canonicalizations.Common
    Proofs.StampEvalThms
begin

phase AutoNode
  terminating size
begin

optimization TryTest: "x \<longmapsto> x"
  quickcheck
  sorry

end (* End AutoNode *)

end
