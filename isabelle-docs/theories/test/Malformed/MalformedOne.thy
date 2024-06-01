theory MalformedOne
  imports
    Canonicalizations.Common
    Proofs.StampEvalThms
begin

phase MalformedNode
  terminating size
begin

optimization Auto: "(true ? x then y) \<longmapsto> x"
  .

end

end
