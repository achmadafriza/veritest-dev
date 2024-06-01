(* Each theory file starts with a header like the one below *)
theory Afriza
  imports
    Canonicalizations.Common
    Proofs.StampEvalThms
begin

(* Ignore this definition *)
definition isBoolean :: "IRExpr \<Rightarrow> bool" where
  "isBoolean e = (\<forall>m p cond. (([m,p] \<turnstile> e \<mapsto> cond) \<longrightarrow> (cond \<in> {IntVal 32 0, IntVal 32 1})))"


quickcheck_generator Value constructors: IntVal
quickcheck_generator IRExpr constructors: ConstantExpr

code_pred [show_modes] evaltree .
code_thms evaltree
code_deps evaltree

print_commands
export_code evaltree in SML

(* Use of the optimization keyword is restricted to within a phase *)
(* We start a phase and provide a pre-made termination measure, size *)
phase ConditionalNode
  terminating size
begin

(* An optimization that nitpick can show is wrong *)
optimization Wrong: "x + y \<longmapsto> x - y"
  nitpick

  (* We have sorry at the end so the rest of the file can be parsed *)
  (* Otherwise Isabelle will not allow the unfinished obligation introduced by the optimization command to continue *)
  sorry

lemma Wrong4: "(x::2 word) + (y::2 word) = (x::2 word) - (y::2 word)"
  nitpick[verbose]
  quickcheck
  sorry

lemma Wrong3: "val[x + y] \<noteq> UndefVal \<and> val[x - y] \<noteq> UndefVal \<Longrightarrow> val[x + y] = val[x - y]"
  apply auto                                                      
  quickcheck
  nitpick[verbose]
  sorry

lemma Wrong5: "bin[val[x + y]] = bin[val[x - y]]"
  quickcheck
  nitpick[verbose]
  sorry

optimization SubNegativeConstant: "(x - (const (IntVal b y))) \<longmapsto> 
                                    x + (const (IntVal b y))  when (y \<ge> 0)"
   defer
  quickcheck
  nitpick[verbose]
  sorry

optimization TestMalformed: "((x eq (const (IntVal 32 1))) ? 
                            (const (IntVal 32 1)) : (const (IntVal 32 0))) \<longmapsto> x \<oplus> (const (IntVal 32 1))
                             when (x = const (IntVal 32 0) | 
                                  (x = const (IntVal 32 1)))"
  sorry

(*code_pred (modes: i \<Rightarrow> i \<Rightarrow> i \<Rightarrow> o \<Rightarrow> bool)
  [show_steps,show_mode_inference,show_intermediate_results] 
  evaltree .*)

lemma WrongCond2:
"([m,p] \<turnstile> exp[x + y] \<mapsto> v) \<longrightarrow>
       ([m,p] \<turnstile> exp[x - y] \<mapsto> v)"
  apply auto
  quickcheck [verbose]
  nitpick

lemma Simp:
  assumes "[m,p] \<turnstile> x \<mapsto> xv"
  assumes "[m,p] \<turnstile> y \<mapsto> yv"
  shows "([m,p] \<turnstile> exp[x - y] \<mapsto> intval_add xv yv)"
  using assms apply auto
  quickcheck [verbose]
  nitpick

lemma WrongCond1: "
([m,p] \<turnstile> ConditionalExpr (ConstantExpr (IntVal (32::nat) (1::64 word))) x y \<mapsto> v) \<longrightarrow>
       ([m,p] \<turnstile> y \<mapsto> v)"
  nitpick

lemma WrongCond: "ConditionalExpr (ConstantExpr (IntVal (32) (1::64 word))) x y \<ge> y"
  apply simp
  nitpick

optimization WrongConditional: "(true ? x : y) \<longmapsto> y"
  apply auto quickcheck [verbose]
  nitpick[debug=true,verbose=true,destroy_constrs=false,dont_finitize,dont_box]

(* An optimization that requires no further proof *)
optimization Auto: "(true ? x : y) \<longmapsto> x"
  .

(* An optimization that sledgehammer can produce *)
(* The sledgehammer command is normally removed after the tactic is included in the theory file *)
optimization SemiAuto: "(c ? true : false) \<longmapsto> c
                                          when isBoolean c"
  using empty_iff insert_iff isBoolean_def by fastforce
  sorry

thm_oracles SemiAuto


(* An optimization that requires a rather nasty manual proof *)
(* Understanding this proof not required for your project! *)
optimization Manual: "((!e) ? x : y) \<longmapsto> (e ? y : x)"
  apply simp apply (rule allI; rule allI; rule allI; rule impI)
  subgoal premises p for m p v
  proof -
    obtain ev where ev: "[m,p] \<turnstile> e \<mapsto> ev"
      using p by blast
    then have notEv: "[m,p] \<turnstile> exp[!e] \<mapsto> intval_logic_negation ev"
      using evalDet p by fastforce
    have inverse: "val_to_bool ev = (\<not>(val_to_bool (intval_logic_negation ev)))"
      by (smt (verit) eval_bits_1_64 intval_logic_negation.elims logic_negate_def new_int.simps notEv of_bool_eq(2) one_neq_zero take_bit_of_0 take_bit_of_1 unfold_unary val_to_bool.simps(1))
    obtain xv where xvdef: "[m, p] \<turnstile> x \<mapsto> xv"
      using p by auto
    obtain yv where yvdef: "[m, p] \<turnstile> y \<mapsto> yv"
      using p by auto
    then show ?thesis
    proof (cases "val_to_bool ev")
      case True
      then have "[m, p] \<turnstile> exp[(!e) ? x : y] \<mapsto> yv"
        using ConditionalExpr evaltree_not_undef inverse notEv xvdef yvdef by auto
      also have "[m, p] \<turnstile> exp[e ? y : x] \<mapsto> yv"
        using True calculation ev evaltree_not_undef yvdef by auto
      ultimately show ?thesis
        by (metis evalDet p)
    next
      case False
      then have "[m, p] \<turnstile> exp[(!e) ? x : y] \<mapsto> xv"
        using ConditionalExpr evaltree_not_undef inverse notEv xvdef yvdef by auto
      also have "[m, p] \<turnstile> exp[e ? y : x] \<mapsto> xv"
        using ConditionalExpr False ev evaltree_not_undef xvdef yvdef by force
      ultimately show ?thesis
        by (metis evalDet p)
    qed
  qed
  done

end

end
