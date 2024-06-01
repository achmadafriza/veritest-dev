theory TryTest
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

optimization TryConditionalEliminateKnownLess: "(x < y ? x : y) \<longmapsto> x"
  try
  sorry

optimization TryUnaryConstantFold: "UnaryExpr op c \<longmapsto> ConstantExpr (unary_eval op val_c) when is_int_val val_c"
  try
  sorry

optimization TryMulNegate: "(x * -(const (IntVal b 1)) ) \<longmapsto> -x when (stamp_expr x = IntegerStamp 32 l u)"
  quickcheck
  sorry

optimization TryAndSignExtend: "BinaryExpr BinAnd (UnaryExpr (UnarySignExtend In Out) (x)) 
                                               (const (new_int b e))
                              \<longmapsto> (UnaryExpr (UnaryZeroExtend In Out) (x))
                                  "
  try
  sorry

optimization WrongAndNeutral: "(x & (const (IntVal b 0))) \<longmapsto> x 
   when (wf_stamp x \<and> stamp_expr x = IntegerStamp b lo hi)"
  quickcheck
  nitpick
  sorry

end

end
