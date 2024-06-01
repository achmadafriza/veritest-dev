theory Sledge
  imports
    Canonicalizations.Common
    Proofs.StampEvalThms
begin

phase SledgeStageTwoNode
  terminating size
begin

optimization SubThenSubLeft: "(x - (x - y)) \<longmapsto>  y"
  sledgehammer
  sorry

end

end