// This is supporting software for CS321/CS322 Compilers and Language Design.
// Copyright (c) Portland State University
//---------------------------------------------------------------------------
// For CS322 W'16 (J. Li).

// SC0 code generator.
//
//
import java.util.*;
import java.io.*;
import ast.*;

class SC0Gen {

  static class GenException extends Exception {
    public GenException(String msg) { super(msg); }
  }

  // The var array
  //
  // Usage:
  //   vars.add(name) --- add a var
  //   vars.indexOf(name) --- get a var's index
  //
  static ArrayList<String> vars = new ArrayList<String>();

  // The main routine
  //
  public static void main(String [] args) throws Exception {
    if (args.length == 1) {
      FileInputStream stream = new FileInputStream(args[0]);
      Ast0.Program p = new ast.Ast0Parser(stream).Program();
      stream.close();
      List<String> code = gen(p);
      String[] insts = code.toArray(new String[0]);
      int i = 0;
      System.out.print("# Stack Code (SC0)\n\n");
      for (String inst: insts) {
	System.out.print(i++ + ". " + inst + "\n");
      }
    } else {
      System.out.println("You must provide an input file name.");
    }
  }

  // Ast0.Program ---
  // Ast0.Stmt[] stmts;
  //
  // Template:
  //   code: {stmt.c}
  //
  static List<String> gen(Ast0.Program n) throws Exception {
    ArrayList<String> code = new ArrayList<String>();
    for (Ast0.Stmt s: n.stmts)
      code.addAll(gen(s));
    return code;
  }

  // STATEMENTS

  static List<String> gen(Ast0.Stmt n) throws Exception {
    if (n instanceof Ast0.Block)       return gen((Ast0.Block) n);
    else if (n instanceof Ast0.Assign) return gen((Ast0.Assign) n);
    else if (n instanceof Ast0.If)     return gen((Ast0.If) n);
    else if (n instanceof Ast0.While)  return gen((Ast0.While) n);
    else if (n instanceof Ast0.Print)  return gen((Ast0.Print) n);
    throw new GenException("Unknown Ast0 Stmt: " + n);
  }
  

  // Ast0.Block ---
  // Ast0.Stmt[] stmts;
  //
  static List<String> gen(Ast0.Block n) throws Exception {
    List<String> code = new ArrayList<String>();

    for(Ast0.Stmt s: n.stmts) {
      code.addAll(gen(s));
    }

    return code;
  }

  // Ast0.Assign ---
  // Ast0.Id lhs;
  // Ast0.Exp rhs;
  //
  // Assign x 1
  // Assign y x
  //        CONST 1
  //        STORE 0 # Store x (x's index is 0)
  //        LOAD 0  # Load x
  //        STORE 1 # Store y ( y's index is 1)
  //
  //
  static List<String> gen(Ast0.Assign n) throws Exception {
    List<String> code = gen(n.rhs);
    
    String varId = n.lhs.nm;

    if(!vars.contains(varId)) {
      vars.add(varId);
      code.add("STORE " + vars.indexOf(varId));
    }
    else {
      code.add("STORE " + vars.indexOf(varId));
    }

    return code;
  }

  // Ast0.If ---
  // Ast0.Exp cond;
  // Ast0.Stmt s1, s2;
  //
  // Template:
  //   code: cond.c
  //         + "IFZ +n1"       # n1 = s1.c's size +2 if s2 exists 
  //         + s1.c            #    | s1.c's size +1 otherwise
  //         [+ "GOTO +n2"]    # n2 = s2.c's size +1
  //         [+ s2.c] 
  //
  static List<String> gen(Ast0.If n) throws Exception {
    List<String> code = gen(n.cond);
    List<String> s1code = gen(n.s1);
    code.add("IFZ +" + (s1code.size() + (n.s2==null ? 1 : 2)));
    code.addAll(s1code);
    if (n.s2 != null) {
      List<String> s2code = gen(n.s2);
      code.add("GOTO +" + (s2code.size() + 1));
      code.addAll(s2code);
    }
    return code;
  }

  // Ast0.While ---
  // Ast0.Exp cond;
  // Ast0.Stmt s;
  //
  static List<String> gen(Ast0.While n) throws Exception {
    List<String> code = gen(n.cond);
    List<String> sCode = gen(n.s);

    code.add("IFZ +" + (sCode.size() + 1));
    code.addAll(sCode);
    code.add("GOTO -" + (code.size() + 1));

    return code;
  }
  
  // Ast0.Print ---
  // Ast0.Exp arg;
  //
  static List<String> gen(Ast0.Print n) throws Exception {
    List<String> code = gen(n.arg);

    code.add("PRINT");

    return code;
  }

  // EXPRESSIONS

  static List<String> gen(Ast0.Exp n) throws Exception {
    if (n instanceof Ast0.Binop)    return gen((Ast0.Binop) n);
    if (n instanceof Ast0.Unop)     return gen((Ast0.Unop) n);
    if (n instanceof Ast0.Id)	    return gen((Ast0.Id) n);
    if (n instanceof Ast0.IntLit)   return gen((Ast0.IntLit) n);
    if (n instanceof Ast0.BoolLit)  return gen((Ast0.BoolLit) n);
    throw new GenException("Unknown Exp node: " + n);
  }

  // Ast0.Binop ---
  // Ast0.BOP op;
  // Ast0.Exp e1,e2;
  //
  // Template:
  // 1. for arithematic and logic ops:
  //   code: e1.c + e2.c 
  //         + "<AOP>"
  // 2. for relational ops:
  //   code: e1.c + e2.c
  //         + "<CJUMP> +3"
  //         + "CONST 0"
  //         + "GOTO +2"
  //         + "CONST 1"
  //
  static List<String> gen(Ast0.Binop n) throws Exception {
    List<String> code = gen(n.e1);
    code.addAll(gen(n.e2)); 
    String op = gen(n.op);
    switch (n.op) {
    case ADD: case SUB: 
    case MUL: case DIV: 
    case AND: case OR:  // not short-circuit semantics
      code.add(op);
      break;
    case EQ: case NE: 
    case LT: case LE: 
    case GT: case GE:  
      code.add(op + " +3");
      code.add("CONST 0");
      code.add("GOTO +2");
      code.add("CONST 1");
      break;
    }
    return code;
  }

  // Ast0.Unop ---
  // Ast0.UOP op;
  // Ast0.Exp e;
  //
  static List<String> gen(Ast0.Unop n) throws Exception {
    List<String> code = gen(n.e);
    
    // If zero, jump 3 which will change it to 1
    // otherwise just go to next, to set to zero
    // and jump over the 1
    if(n.op == Ast0.UOP.NOT) {
      code.add("IFZ +3");
      code.add("CONST 0");
      code.add("GOTO +2");
      code.add("CONST 1");
    }
    else {
      code.add("NEG");
    }

    return code;
  }
  
  // Ast0.Id ---
  // String nm;
  //
  // Template:
  //   code: "LOAD <Id>.idx"
  //
  static List<String> gen(Ast0.Id n) throws Exception {
    List<String> code = new ArrayList<String>();
    int idx = vars.indexOf(n.nm);
    if (idx < 0)
      throw new GenException("Id is not defined: " + n.nm);
    code.add("LOAD " + idx);
    return code;
  }

  // Ast0.IntLit ---
  // int i;
  //
  static List<String> gen(Ast0.IntLit n) throws Exception {
    List<String> code = new ArrayList<String>(); 

    // Probably something along the lines of adding a ("CONST codePack.src)
    code.add("CONST " + n.i);

    return code;
  }

  // Ast0.BoolLit ---
  // boolean b;
  //
  static List<String> gen(Ast0.BoolLit n) {
    List<String> code = new ArrayList<String>();
    
    if(n.b)
      code.add("CONST 1");
    else
      code.add("CONST 0");


    return code;
  }

  // OPERATORS

  static String gen(Ast0.BOP op) {
    switch (op) {
    case ADD: return "ADD";
    case SUB: return "SUB";
    case MUL: return "MUL";
    case DIV: return "DIV";
    case AND: return "AND";
    case OR:  return "OR";
    case EQ:  return "IFEQ";
    case NE:  return "IFNE"; 
    case LT:  return "IFLT"; 
    case LE:  return "IFLE"; 
    case GT:  return "IFGT"; 
    case GE:  return "IFGE"; 
    }
    return null;
  }
   
}
