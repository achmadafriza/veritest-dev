theory Auto
  imports
    Canonicalizations.Common
    Proofs.StampEvalThms
begin

phase AutoNode
  terminating size
begin

optimization Auto: "(true ? x : y) \<longmapsto> x"
  .

end (* End AutoNode *)

end
