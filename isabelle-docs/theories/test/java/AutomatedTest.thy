theory AutomatedTest
  imports
    Canonicalizations.Common
    Proofs.StampEvalThms
begin

phase TemporaryNode
  terminating size
begin

optimization TemporaryTheory: "(true ? x : y) \<longmapsto> x"

.
sorry

end

end

