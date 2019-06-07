package toorla.codeGeneration;

import toorla.ast.Program;
import toorla.ast.declaration.classDecs.ClassDeclaration;
import toorla.ast.declaration.classDecs.EntryClassDeclaration;
import toorla.ast.declaration.classDecs.classMembersDecs.AccessModifier;
import toorla.ast.declaration.classDecs.classMembersDecs.ClassMemberDeclaration;
import toorla.ast.declaration.classDecs.classMembersDecs.FieldDeclaration;
import toorla.ast.declaration.classDecs.classMembersDecs.MethodDeclaration;
import toorla.ast.declaration.localVarDecs.ParameterDeclaration;
import toorla.ast.expression.*;
import toorla.ast.expression.binaryExpression.*;
import toorla.ast.expression.unaryExpression.Neg;
import toorla.ast.expression.unaryExpression.Not;
import toorla.ast.expression.value.BoolValue;
import toorla.ast.expression.value.IntValue;
import toorla.ast.expression.value.StringValue;
import toorla.ast.statement.*;
import toorla.ast.statement.localVarStats.LocalVarDef;
import toorla.ast.statement.localVarStats.LocalVarsDefinitions;
import toorla.ast.statement.returnStatement.Return;
import toorla.compileErrorException.nameErrors.*;
import toorla.symbolTable.SymbolTable;
import toorla.symbolTable.exceptions.ItemAlreadyExistsException;
import toorla.symbolTable.symbolTableItem.ClassSymbolTableItem;
import toorla.symbolTable.symbolTableItem.MethodSymbolTableItem;
import toorla.symbolTable.symbolTableItem.varItems.FieldSymbolTableItem;
import toorla.symbolTable.symbolTableItem.varItems.LocalVariableSymbolTableItem;
import toorla.types.Type;
import toorla.types.arrayType.ArrayType;
import toorla.types.singleType.BoolType;
import toorla.types.singleType.IntType;
import toorla.types.singleType.StringType;
import toorla.visitor.Visitor;
import java.io.File;
import java.io.IOException;

import java.io.FileWriter;
import java.util.ArrayList;

public class codeGeneration extends Visitor<Void> {
    private FileWriter fw;
    private ArrayList<String> instructionList = new ArrayList<>();
    private int lableCounter = 0;

    public codeGeneration() {

    }

    @Override
    public Void visit(Block block) {
        SymbolTable.pushFromQueue();
        for (Statement stmt : block.body)
            stmt.accept(this);
        SymbolTable.pop();
        return null;
    }

    @Override
    public Void visit(Conditional conditional) {
        String nElse = "ELSE_" + (lableCounter++);
        String nAfter = "AFTER_" + (lableCounter++);
        conditional.getCondition().accept(this);
        instructionList.add("ifeq " + nElse);
        SymbolTable.pushFromQueue();
        conditional.getThenStatement().accept(this);
        SymbolTable.pop();
        instructionList.add("goto " + nAfter);
        instructionList.add(nElse + ":");
        SymbolTable.pushFromQueue();
        conditional.getElseStatement().accept(this);
        SymbolTable.pop();
        instructionList.add(nAfter + ":");

        return null;

    }

    @Override
    public Void visit(While whileStat) {
        String nStmt = "STMT_" + (lableCounter++);
        String nStart = "START_" + (lableCounter++);
        instructionList.add("goto " + nStart);

        instructionList.add(nStmt + ":");
        SymbolTable.pushFromQueue();
        whileStat.body.accept(this);
        SymbolTable.pop();

        instructionList.add(nStart + ":");
        whileStat.expr.accept(this);

        instructionList.add("ifneq " +  nStmt);

        return null;
    }

    @Override
    public Void visit(LocalVarDef localVarDef) {
        return null;
    }

    void writeInstructions() {
        try {
            for (String str : instructionList)
                fw.write(str);
        } catch (IOException exc) {
            System.out.println("IO Exception Occurred:");
            exc.printStackTrace();
        }
    }

    @Override
    public Void visit(ClassDeclaration classDeclaration) {
        SymbolTable.pushFromQueue();
        try {
            File file = new File("../artifact" + classDeclaration.getName().getName() +".j");
            boolean fvar = file.createNewFile();
            if (fvar){
                System.out.println("File has been created successfully");
            }
            else{
                System.out.println("File already present at the specified location");
            }
        } catch (IOException e) {
            System.out.println("Exception Occurred:");
            e.printStackTrace();
        }
        try{
            fw=new FileWriter("../artifact" + classDeclaration.getName().getName() +".j");
            fw.write(".class public" + classDeclaration.getName().getName());
            String father;
            if(classDeclaration.getParentName() == null) {
                father = "Any";
            }
            else
                father = classDeclaration.getParentName().getName();
            fw.write(".super "); ///////////need work
            fw.write(father);
            // constructor
            fw.write(".method public <init>()V");
            fw.write("aload_0");
            fw.write("invokespecial " + father +"/<init>()V");
            fw.write("return");
            fw.write(".end method");

        }catch(Exception e){

        }
        instructionList.clear();
        for (ClassMemberDeclaration cmd : classDeclaration.getClassMembers())
                cmd.accept(this);
        writeInstructions();
        try{
            fw.close();
        }catch (IOException e){

        }
        SymbolTable.pop();
        return null;
    }

    @Override
    public Void visit(EntryClassDeclaration entryClassDeclaration) {
        this.visit((ClassDeclaration) entryClassDeclaration);
        return null;
    }

    public String convertFieldType(Type type)
    {
        if (type instanceof ArrayType)
            return "[" + convertFieldType(((ArrayType) type).getSingleType());
        if (type instanceof BoolType)
            return "Z";
        if (type instanceof IntType)
            return "I";
        if (type instanceof StringType)
            return "Ljava/lang/String;";
        return "";
    }

    @Override
    public Void visit(FieldDeclaration fieldDeclaration) {
        try {
            fw.write(".field public ");
            fw.write(fieldDeclaration.getIdentifier().getName());
            fw.write(convertFieldType(fieldDeclaration.getType()));
        } catch (IOException e){

        }
        return null;
    }

    @Override
    public Void visit(ParameterDeclaration parameterDeclaration) {
        return null;
    }


    @Override
    public Void visit(LocalVarsDefinitions localVarsDefinitions) {
        for (LocalVarDef lvd : localVarsDefinitions.getVarDefinitions()) {
            lvd.accept(this);
        }
        return null;
    }

    void makeAnyClass() {
        try {
            File file = new File("../artifact/Any.j");
            boolean fvar = file.createNewFile();
            if (fvar){
                System.out.println("File has been created successfully");
            }
            else{
                System.out.println("File already present at the specified location");
            }
        } catch (IOException e) {
            System.out.println("Exception Occurred:");
            e.printStackTrace();
        }
        try{
            fw = new FileWriter("../artifact/Any.j");
            fw.write(".class public Any");
            fw.write(".super java/lang/Object");
            // constructor
            fw.write(".method public <init>()V");
            fw.write("aload_0");
            fw.write("invokespecial java/lang/Object/<init>()V");
            fw.write("return");
            fw.write(".end method");

        }catch(Exception e){

        }
    }

    @Override
    public Void visit(Program program) {
        makeAnyClass();
        for (ClassDeclaration cd : program.getClasses()) {
            cd.accept(this);
        }
        SymbolTable.pop();
        return null;
    }

    public Void visit(Plus plusExpr) {
        plusExpr.getLhs().accept(this);
        plusExpr.getRhs().accept(this);
        instructionList.add("iadd");
        return null;
    }

    public Void visit(Minus minusExpr) {
        minusExpr.getLhs().accept(this);
        minusExpr.getRhs().accept(this);
        instructionList.add("isub");
        return null;
    }

    public Void visit(Times timesExpr) {
        timesExpr.getLhs().accept(this);
        timesExpr.getRhs().accept(this);
        instructionList.add("imul");
        return null;
    }

    public Void visit(Division divExpr) {
        divExpr.getLhs().accept(this);
        divExpr.getRhs().accept(this);
        instructionList.add("idiv");
        return null;
    }

    public Void visit(Modulo moduloExpr) {
        moduloExpr.getLhs().accept(this);
        moduloExpr.getRhs().accept(this);
        instructionList.add("irem");
        return null;
    }

    public Void visit(Equals equalsExpr) {

        return null;
    }

    void if_cmpXX(BinaryExpression exp, String op) {
        String L1 = "TRUE_" + (lableCounter++);
        String L2 = "FALSE_" + (lableCounter++);
        exp.getLhs().accept(this);
        exp.getRhs().accept(this);
        instructionList.add(op + " " + L1);
        instructionList.add("iconst_0");
        instructionList.add("goto " + L2);
        instructionList.add(L1 + ":");
        instructionList.add("iconst_1");
        instructionList.add(L2 + ":");
    }

    public Void visit(GreaterThan gtExpr) {
        if_cmpXX(gtExpr, "if_cmpgt");
        return null;
    }

    public Void visit(LessThan lessThanExpr) {
        if_cmpXX(lessThanExpr, "if_cmplt");
        return null;
    }

    public Void visit(And andExpr) { // short circuit
        String nElse = "ELSE_" + (lableCounter++);
        String nAfter = "AFTER_" + (lableCounter++);
        andExpr.getLhs().accept(this);
        instructionList.add("ifeq " + nElse);
        andExpr.getRhs().accept(this);
        instructionList.add("goto " + nAfter);

        instructionList.add(nElse + ":");
        instructionList.add("iconst_0");

        instructionList.add(nAfter + ":");

        return null;
    }

    public Void visit(Or orExpr) { // short circuit
        String nElse = "ELSE_" + (lableCounter++);
        String nAfter = "AFTER_" + (lableCounter++);
        orExpr.getLhs().accept(this);
        instructionList.add("ifeq " + nElse);
        instructionList.add("iconst_1");
        instructionList.add("goto " + nAfter);

        instructionList.add(nElse + ":");
        orExpr.getRhs().accept(this);

        instructionList.add(nAfter + ":");

        return null;
    }

    public Void visit(Neg negExpr) {
        negExpr.accept(this);
        instructionList.add("ineg");
        return null;
    }

    public Void visit(Not notExpr) {
        notExpr.accept(this);
        String L1 = "TRUE_" + (lableCounter++);
        String L2 = "FALSE_" + (lableCounter++);
        instructionList.add("ifeq" + L1);
        instructionList.add("icont_0");
        instructionList.add("goto" + L2);
        instructionList.add(L1 + ":");
        instructionList.add("iconst_1");
        instructionList.add(L2 + ":");
        return null;
    }

    public Void visit(MethodCall methodCall) {
        for (Expression ex : methodCall.getArgs())
            ex.accept(this);
//        methodCall.getInstance().


        return null;
    }

    @Override
    public Void visit(MethodDeclaration methodDeclaration) {
        SymbolTable.pushFromQueue();
        String access,paramType= "",returnType;
        if (methodDeclaration.getAccessModifier() == AccessModifier.ACCESS_MODIFIER_PRIVATE )
            access = "praivate";
        else
            access = "public";
        for (ParameterDeclaration arg : methodDeclaration.getArgs())
            paramType += convertFieldType(arg.getType());
        returnType = convertFieldType(methodDeclaration.getReturnType());
        instructionList.add(".method " + access + " " + methodDeclaration.getName().getName() + "(" + paramType +")" + returnType );
        instructionList.add(".limit stack 1000");
        instructionList.add(".limit locals 1000");
        for (Statement stmt : methodDeclaration.getBody())
            stmt.accept(this);
        instructionList.add(".end method");
        SymbolTable.pop();
        return null;
    }

    public Void visit(Identifier identifier) {
        return null;
    }

    public Void visit(Self self) {
        return null;
    }

    public Void visit(IntValue intValue) {

        return null;
    }

    public Void visit(NewArray newArray) {

        return null;
    }

    public Void visit(BoolValue booleanValue) {

        return null;
    }

    public Void visit(StringValue stringValue) {
        return null;
    }

    public Void visit(NewClassInstance newClassInstance)
    {
        return null;
    }

    public Void visit(FieldCall fieldCall) {
        return null;
    }

    public Void visit(ArrayCall arrayCall) {
        return null;
    }

    public Void visit(NotEquals notEquals) {

        return null;
    }

    public Void visit(PrintLine printStat) {
        return null;
    }

    public Void visit(Assign assignStat) {

        return null;
    }




    public Void visit(Return returnStat) {

        return null;
    }

    public Void visit(Break breakStat) {
        return null;
    }

    public Void visit(Continue continueStat) {
        return null;
    }

    public Void visit(Skip skip) {
        return null;
    }


    public Void visit(IncStatement incStatement) {
        return null;
    }

    public Void visit(DecStatement decStatement) {
        return null;
    }


}
