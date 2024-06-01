theory NitpickTest
  imports
    Canonicalizations.Common
    Proofs.StampEvalThms
begin

phase ConditionalNode
  terminating size
begin

optimization Wrong: "x + y \<longmapsto> x - y"
  nitpick

  sorry

end

end
