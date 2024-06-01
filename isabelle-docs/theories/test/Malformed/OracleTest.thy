theory OracleTest
  imports
    Canonicalizations.Common
    Proofs.StampEvalThms
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
begin

phase ConditionalNode
  terminating size
begin

optimization TemporaryTheory: "(e + (const (IntVal 32 0))) \<longmapsto> e"
  using AddNeutral_Exp by blast
  sledgehammer
  sorry

thm_oracles TemporaryTheory

end

end
