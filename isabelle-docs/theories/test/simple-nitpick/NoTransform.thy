theory NoTransform
  imports
    Canonicalizations.Common
    Canonicalizations.AbsPhase
    Canonicalizations.AddPhase
    Canonicalizations.AndPhase
    Canonicalizations.BinaryNode
    Canonicalizations.ConditionalPhase
    Canonicalizations.MulPhase
    Canonicalizations.NewAnd
    Canonicalizations.NotPhase
    Canonicalizations.OrPhase
    Canonicalizations.ShiftPhase
    Canonicalizations.SignedDivPhase
    Canonicalizations.SignedRemPhase
    Canonicalizations.SubPhase
    Canonicalizations.XorPhase
    Proofs.StampEvalThms
begin

phase AutoNode                                                                      
  terminating size
begin

optimization TryTest: "x * (ConstantExpr (IntVal b 2)) \<longmapsto> x"
  nitpick
  sorry

end (* End AutoNode *)

end
