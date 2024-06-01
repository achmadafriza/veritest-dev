theory NitpickFail
  imports
    Canonicalizations.Common
    Proofs.StampEvalThms
begin

phase NitpickFailNode
  terminating size
begin

optimization SubThenSubLeft: "(x - (x - y)) \<longmapsto>  y"
  nitpick
  sorry

end

end