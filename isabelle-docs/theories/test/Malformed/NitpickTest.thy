theory NitpickTest
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

optimization Wrong: "x + y \<longmapsto> x - y"
  nitpick [debug = true]

  sorry

optimization WrongConditional: "(true ? x : y) \<longmapsto> y"
  nitpick [debug = true]
  quickcheck

  sorry

optimization WrongOpt: "x - (const (val[-y])) \<longmapsto> x + (const y)"
  try
  sorry

optimization WrongAndNots: "(~x) | (~y) \<longmapsto> ~(x | y)"
  quickcheck
  nitpick
  sorry

optimization WrongButRight: "x - -e \<longmapsto> x + e"
  apply (metis add_2_eq_Suc' less_SucI less_add_Suc1 not_less_eq size_binary_const size_non_add)
  using exp_sub_negative_value by blast

optimization WrongConditionalExtract: "(c ? false : true) \<longmapsto> ~c
                                          when isBoolean c"
  nitpick
  quickcheck
  sledgehammer
  sorry

optimization WrongConditionalEqualIsRHS: "((x eq x) ? x : y) \<longmapsto> y"
  nitpick
  quickcheck
  sorry

optimization WrongnormalizeX: "((x eq (const (IntVal 32 1))) ? 
                                  (const (IntVal 32 1)) : (const (IntVal 32 0))) \<longmapsto> x
                                   when (x = ConstantExpr (IntVal 32 0) | 
                                        (x = ConstantExpr (IntVal 32 1)))"
  nitpick
  quickcheck
  sorry

optimization WrongflipX: "((x eq (const (IntVal 32 0))) ? 
                            (const (IntVal 32 1)) : (const (IntVal 32 0))) \<longmapsto> x \<oplus> (const (IntVal 32 1))
                             when (x = ConstantExpr (IntVal 32 0) | 
                                  (x = ConstantExpr (IntVal 32 1)))"
  nitpick
  quickcheck
  sorry

optimization Wrongcondition_bounds: "((u \<ge> v) ? x : y) \<longmapsto> x 
    when (stamp_under (stamp_expr u) (stamp_expr v) \<and> wf_stamp u \<and> wf_stamp v)"
  try
  sorry

optimization WrongConditionalEliminateKnownLess: "((x \<ge> y) ? x : y) \<longmapsto> x 
                                 when (stamp_under (stamp_expr x) (stamp_expr y)
                                      \<and> wf_stamp x \<and> wf_stamp y)"
  nitpick
  quickcheck
  sorry

optimization TryShiftToMul: "x / y \<longmapsto> x << const (IntVal 64 i) 
                              when (i > 0 \<and> stamp_expr x = IntegerStamp 64 xl xh \<and> wf_stamp x \<and>
                                    64 > i \<and>
                                    y = exp[const (IntVal 64 (2 ^ unat(i)))])"
  nitpick
  quickcheck
  sorry

optimization TryNegateCancel: "-(-(x)) \<longmapsto> x"
  try
  sorry

optimization WrongMaskOutRHS: "(x \<oplus> y) \<longmapsto> x
                          when (is_ConstantExpr y
                             \<and> (stamp_expr (BinaryExpr BinXor x y) = IntegerStamp stampBits l h) 
                             \<and> (BinaryExpr BinAnd y (ConstantExpr (new_int stampBits (not 0))) 
                                                   = ConstantExpr (new_int stampBits (not 0))))"
  nitpick
  quickcheck
  sorry

optimization WrongMul: "x * (const exp[y]) \<longmapsto> x"
  nitpick
  quickcheck
  sledgehammer
  sorry                                          

optimization WrongNotCancel: "(~a) \<longmapsto> a"
  nitpick [debug= true]
  quickcheck
  sledgehammer
  sorry

optimization WrongEliminateRedundantFalse: "(x || (false)) \<longmapsto> x"
  nitpick
  quickcheck
  sledgehammer
  sorry

optimization WrongSubNegativeConstant: "(x - (const (IntVal b y))) \<longmapsto> 
                                    x + (const (IntVal b y))  when (y > 0)"
  quickcheck
  nitpick
  sorry

optimization WrongSubNegativeConstant2: "x - (const (val[-y])) \<longmapsto> x + (const y)"
  quickcheck
  nitpick
  sorry

optimization WrongSubSelfIsZero: "(x - x) \<longmapsto> (const (IntVal b 0))"
  quickcheck
  nitpick
  sledgehammer
  sorry

optimization WrongXorSelf: "(x \<oplus> x) \<longmapsto> true"
  quickcheck
  nitpick
  using size_non_const apply auto[1]
sledgehammer
  sorry

optimization AltAndSelf: "x & (const (IntVal b 1)) \<longmapsto> x"
  quickcheck
  nitpick [debug = true]
  sledgehammer
  sorry

optimization WrongBinaryFoldConstant: "BinaryExpr op (const e1) (const e2) \<longmapsto>
                                  ConstantExpr (bin_eval op e1 e2) when int_and_equal_bits e1 e2"
  quickcheck
  nitpick
  sledgehammer
  sorry

end

end
