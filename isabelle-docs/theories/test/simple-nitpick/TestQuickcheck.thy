theory TestQuickcheck
  imports
    Canonicalizations.Common
    Proofs.StampEvalThms
begin

phase AutoNode
  terminating size
begin

optimization TryTest: "(true ? x : y) \<longmapsto> y"
  nitpick
  sorry

end (* End AutoNode *)

end
