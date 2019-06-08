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
import toorla.symbolTable.symbolTableItem.SymbolTableItem;
import toorla.symbolTable.symbolTableItem.varItems.FieldSymbolTableItem;
import toorla.symbolTable.symbolTableItem.varItems.LocalVariableSymbolTableItem;
import toorla.symbolTable.symbolTableItem.varItems.VarSymbolTableItem;
import toorla.typeChecker.ExpressionTypeExtractor;
import toorla.types.Type;
import toorla.types.arrayType.ArrayType;
import toorla.types.singleType.BoolType;
import toorla.types.singleType.IntType;
import toorla.types.singleType.StringType;
import toorla.types.singleType.UserDefinedType;
import toorla.visitor.Visitor;
import java.io.File;
import java.io.IOException;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Stack;

public class CodeGenerator extends Visitor<Void> {
    private FileWriter fw;
    private String mainClass;
    private ArrayList<String> instructionList = new ArrayList<>();
    private int lableCounter = 0;
    private ExpressionTypeExtractor getType;
    private Stack <String> breaks;
    private Stack <String> continues;
    public CodeGenerator(ExpressionTypeExtractor getType) {
        this.getType = getType;
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
        String exit  = "EXIT_" + (lableCounter++);
        breaks.push(exit);
        continues.push(nStart);
        instructionList.add("goto " + nStart);

        instructionList.add(nStmt + ":");
        SymbolTable.pushFromQueue();
        whileStat.body.accept(this);
        SymbolTable.pop();

        instructionList.add(nStart + ":");
        whileStat.expr.accept(this);

        instructionList.add("ifneq " +  nStmt);
        instructionList.add(exit + ":");
        breaks.pop();
        continues.pop();
        return null;
    }

    @Override
    public Void visit(LocalVarDef localVarDef) {
        // initialization
        Assign assign = new Assign(localVarDef.getLocalVarName(), localVarDef.getInitialValue());

        SymbolTable.define();
        assign.accept(this);

        return null;
    }

    void writeInstructions() {
        try {
            for (String str : instructionList)
                fw.write(str + "\n");

        } catch (IOException exc) {
            System.out.println("IO Exception Occurred:");
            exc.printStackTrace();
        }
    }

    @Override
    public Void visit(ClassDeclaration classDeclaration) {
        SymbolTable.pushFromQueue();
        getType.setCurrentClass(classDeclaration);
        try {
//            System.out.println(classDeclaration.getName().getName());
            File file = new File("./src/toorla/artifact/" + classDeclaration.getName().getName() +".j");
            boolean fvar = file.createNewFile();
            if (fvar){
//                System.out.println("File has been created successfully");
            }
            else{
//                System.out.println(classDeclaration.getName().getName() );
//                System.out.println("File already present at the specified location");
            }
        } catch (IOException e) {
            System.out.println("Exception Occurred:");
            e.printStackTrace();
        }
        try{
            fw=new FileWriter("./src/toorla/artifact/" + classDeclaration.getName().getName() +".j");
            fw.write(".class public " + classDeclaration.getName().getName() + "\n");
            String father;
//            System.out.println(classDeclaration.getParentName());
            if(classDeclaration.getParentName().getName() == null) {
                father = "Any";
            }
            else
                father = classDeclaration.getParentName().getName();
            fw.write(".super " + father + "\n");
            //field
            for (ClassMemberDeclaration cmd : classDeclaration.getClassMembers())
                if (cmd instanceof FieldDeclaration)
                    cmd.accept(this);
            // constructor
            fw.write(".method public <init>()V \n");
            fw.write("aload_0\n");
            fw.write("invokespecial " + father +"/<init>()V\n");
            fw.write("return\n");
            fw.write(".end method\n");

        }catch(Exception e){

        }
        instructionList.clear();

        for (ClassMemberDeclaration cmd : classDeclaration.getClassMembers())
            if (cmd instanceof MethodDeclaration)
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
        mainClass = entryClassDeclaration.getName().getName();
        this.visit((ClassDeclaration) entryClassDeclaration);
        return null;
    }

    public String convertType(Type type)
    {
        if (type instanceof ArrayType)
            return "[" + convertType(((ArrayType) type).getSingleType());
        if (type instanceof BoolType)
            return "Z";
        if (type instanceof IntType)
            return "I";
        if (type instanceof StringType)
            return "Ljava/lang/String;";
        if (type instanceof UserDefinedType)
            return "L" + ((UserDefinedType)type).getClassDeclaration().getName().getName() + ";";
        return "";
    }

    @Override
    public Void visit(FieldDeclaration fieldDeclaration) {
        instructionList.add(".field public " + fieldDeclaration.getIdentifier().getName() + " " + convertType(fieldDeclaration.getType()));
        return null;

    }

    @Override
    public Void visit(ParameterDeclaration parameterDeclaration) {
        SymbolTable.define();
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
            File file = new File("./src/toorla/artifact/Any.j");
            boolean fvar = file.createNewFile();
            if (fvar){
//                System.out.println("File has been created successfully");
            }
            else{
//                System.out.println("File already present at the specified location");
            }
        } catch (IOException e) {
            System.out.println("Exception Occurred:");
            e.printStackTrace();
        }
        try{
            fw = new FileWriter("./src/toorla/artifact/Any.j");
            fw.write(".class public Any\n");
            fw.write(".super java/lang/Object\n");
            // constructor
            fw.write(".method public <init>()V\n");
            fw.write("aload_0\n");
            fw.write("invokespecial java/lang/Object/<init>()V\n");
            fw.write("return\n");
            fw.write(".end method\n");
            fw.close();
        }catch(Exception e){

        }
    }

    public void makeRunner(){
        try {
            File file = new File("./src/toorla/artifact/Runner.j");
            boolean fvar = file.createNewFile();
//            if (fvar){
//                System.out.println("File has been created successfully");
//            }
//            else{
//                System.out.println("File already present at the specified location");
//            }
            fw = new FileWriter("./src/toorla/artifact/Runner.j");
            fw.write(".class public Runner\n");
            fw.write(".super java/lang/Object\n");
            fw.write(".method public <init>()V\n");
            fw.write("aload_0\n");
            fw.write("invokespecial java/lang/Object/<init>()V\n");
            fw.write("return\n");
            fw.write(".end method\n");
            fw.write(".method public static main([Ljava/lang/String;)V\n");
            fw.write(".limit stack 1000\n");
            fw.write(".limit locals 100\n");
            fw.write("new " + mainClass + "\n");
            fw.write("dup\n");
            fw.write("invokespecial " + mainClass+"/<init>()V\n");
            fw.write("invokevirtual "+ mainClass+"/main()I\n");
            fw.write("istore 1\n");
            fw.write("return\n");
            fw.write(".end method\n");
            fw.close();
        }catch (IOException e){

        }
    }

    @Override
    public Void visit(Program program) {
        SymbolTable.pushFromQueue();
        makeAnyClass();
        for (ClassDeclaration cd : program.getClasses()) {
            cd.accept(this);
        }
        makeRunner();
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
        equalsExpr.getLhs().accept(this);
        equalsExpr.getRhs().accept(this);
        Type equal = equalsExpr.getLhs().accept(getType);
        if (equal instanceof IntType || equal instanceof BoolType){
            String L1 = "TRUE_" + (lableCounter++);
            String L2 = "FALSE_" + (lableCounter++);
            instructionList.add("ifeq" + L1);
            instructionList.add("icont_0");
            instructionList.add("goto" + L2);
            instructionList.add(L1 + ":");
            instructionList.add("iconst_1");
            instructionList.add(L2 + ":");
        }
        else if (equal instanceof StringType){
            instructionList.add("invokevirtual java/lang/String.equals(Ljava/lang/String;)Z");
        }
        else if (equal instanceof UserDefinedType){
            instructionList.add("invokevirtual java/lang/Object.equals(Ljava/lang/Object;)Z");
        }
        else {//////equal instancceof ArrayType
            instructionList.add("invokevirtual java/util/Arrays.equals([Ljava/util/Arrays;[Ljava/util/Arrays)Z");
        }


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
        negExpr.getExpr().accept(this);
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
        methodCall.getInstance().accept(this);
        String args = "";
        for (Expression ex : methodCall.getArgs()) {
            ex.accept(this);
            args +=  convertType(ex.accept(getType));
        }
        String methodName = methodCall.getMethodName().getName();
        String obj = ((UserDefinedType)methodCall.getInstance().accept(getType)).getClassDeclaration().getName().getName();
        String returnTupe = convertType(methodCall.accept(getType));
        instructionList.add("invokevirtual " +obj + "/" + methodName + "(" + args + ")" + returnTupe);
        return null;
    }

    @Override
    public Void visit(MethodDeclaration methodDeclaration) {
        SymbolTable.reset();
        SymbolTable.pushFromQueue();
        String access,paramType= "",returnType;
        if (methodDeclaration.getAccessModifier() == AccessModifier.ACCESS_MODIFIER_PRIVATE )
            access = "praivate";
        else
            access = "public";
        for (ParameterDeclaration arg : methodDeclaration.getArgs())
            paramType += convertType(arg.getType());
        returnType = convertType(methodDeclaration.getReturnType());
        instructionList.add(".method " + access + " " + methodDeclaration.getName().getName() + "(" + paramType +")" + returnType );
        instructionList.add(".limit stack 1000");
        instructionList.add(".limit locals 1000");
        for (Statement stmt : methodDeclaration.getBody())
            stmt.accept(this);
        instructionList.add(".end method");
        SymbolTable.pop();
        return null;
    }

    public boolean identifierIsField(String varName) {
        try {
            SymbolTableItem item = SymbolTable.top().get(varName);
            if (item instanceof FieldSymbolTableItem)
                return true;
            else
                return false;
        } catch (Exception exc) {
            // dont occur
        }
        return false;
    }

    public int getIndexLocalVar(String localVarName) {
        try {
//            SymbolTable.top().print();
            SymbolTableItem item = SymbolTable.top().get("var_" + localVarName);
            return item.getDefinitionNumber();
        } catch (Exception exc) {
//            exc.printStackTrace();
        }
        return -1;
    }

    public Void visit(Identifier identifier) {///////////////need work
        if (identifierIsField(identifier.getName())) {
            try {
                Type fieldType = ((FieldSymbolTableItem) SymbolTable.top().get(identifier.getName())).getFieldType();
                String fieldTypeName = convertType(fieldType);
                instructionList.add("getfield " + getType.currentClass.getName().getName() + "/" + identifier.getName() + " " + fieldTypeName);
            } catch (Exception exp) {
                // dont occur
            }
        }
        else {
            int index = getIndexLocalVar(identifier.getName());
//            System.out.println(index);
            Type type = identifier.accept(getType);
            if (hasRefrence(type))
                instructionList.add("aload " + index);
            else
                instructionList.add("iload " + index);
        }
        return null;
    }

    public Void visit(Self self) {
        instructionList.add("aload 0");
        return null;
    }

    public Void visit(IntValue intValue) {
        instructionList.add("bipush " + intValue.getConstant());
        return null;
    }

    public String getTypeName(Type type) {
        if (type instanceof IntType)
            return "int";
        if (type instanceof BoolType)
            return "bool";
        if (type instanceof UserDefinedType)
            return ((UserDefinedType)type).getClassDeclaration().getName().getName();
        if (type instanceof StringType)
            return "java/lang/String";
        return "";
    }

    public Void visit(NewArray newArray) {
        newArray.getLength().accept(this);
        if (hasRefrence(newArray.getType()))
            instructionList.add("anewarray " + getTypeName(newArray.getType()));
        else
            instructionList.add("newarray " + getTypeName(newArray.getType()));
        return null;
    }

    public Void visit(BoolValue booleanValue) {
        if (booleanValue.isConstant())
            instructionList.add("iconst_1");
        else
            instructionList.add("iconst_0");
        return null;
    }

    public Void visit(StringValue stringValue) {
//        instructionList.add("ldc " + "\"" + stringValue.getConstant() + "\"");
        instructionList.add("ldc " + stringValue.getConstant() );
        return null;
    }

    public Void visit(NewClassInstance newClassInstance)
    {
        String objName = newClassInstance.getClassName().getName();
        instructionList.add("new " + objName);
        instructionList.add("dup");
        instructionList.add("invokespecial " + objName + "/<init>()V");
        return null;
    }

    public Void visit(FieldCall fieldCall) {
        fieldCall.getInstance().accept(this );
        if (fieldCall.getField().getName().equals("length")){
            instructionList.add("arraylength");
            return null;
        }
        String fieldName = fieldCall.getField().getName();
        String obj = ((UserDefinedType)fieldCall.getInstance().accept(getType)).getClassDeclaration().getName().getName();
        String type =  convertType(fieldCall.getInstance().accept(getType));

        instructionList.add("getfield " + obj + "/" + fieldName + ' '+ type);
        return null;
    }

    public Void visit(ArrayCall arrayCall) {
        arrayCall.getInstance().accept(this);
        arrayCall.getIndex().accept(this);
        return null;
    }

    public Void visit(NotEquals notEquals) {
        Not not = new Not(new Equals(notEquals.getLhs(),notEquals.getRhs()));
        not.accept(this);
        return null;
    }

    public Void visit(PrintLine printStat) {
        instructionList.add("getstatic java/lang/System/out Ljava/io/PrintStream;");
        printStat.getArg().accept(this);
        Type printType = printStat.getArg().accept(getType);
        instructionList.add("invokevirtual java/io/PrintStream/println(" + convertType(printType) + ")V");
        return null;
    }

    public boolean hasRefrence(Type type) {
        return !(type instanceof IntType || type instanceof BoolType);
    }

    public Void visit(Assign assignStat) {
        if (assignStat.getLvalue() instanceof FieldCall) {
            Type objType = ((FieldCall)assignStat.getLvalue()).getInstance().accept(getType);
            String objName = getTypeName(objType);
            String fieldName = ((FieldCall)assignStat.getLvalue()).getField().getName();
            String fieldType = convertType(assignStat.getLvalue().accept(getType));
            instructionList.add("putfield " + objName + "/" + fieldName + " " + fieldType);
        }
        else if ((assignStat.getLvalue() instanceof Identifier) && identifierIsField(((Identifier)assignStat.getLvalue()).getName())) {
            String objName = getType.currentClass.getName().getName();
            String fieldName = ((Identifier)assignStat.getLvalue()).getName();
            String fieldType = convertType(assignStat.getLvalue().accept(getType));
            instructionList.add("putfield " + objName + "/" + fieldName + " " + fieldType);
        }
        else {
//            assignStat.getLvalue().accept(this);
            assignStat.getRvalue().accept(this);
            Type RType = assignStat.getRvalue().accept(getType);
            if (assignStat.getLvalue() instanceof ArrayCall) {
                if (hasRefrence(RType))
                    instructionList.add("aastore");
                else
                    instructionList.add("iastore");
            }
            else {
//                instructionList.add("Debug");
                int index = getIndexLocalVar(((Identifier)assignStat.getLvalue()).getName());
//                System.out.println(index);
                if (hasRefrence(RType))
                    instructionList.add("astore " + index);
                else
                    instructionList.add("istore " + index);
            }
        }
        return null;
    }

    public Void visit(Return returnStat) {
        returnStat.getReturnedExpr().accept(this);
        Type returnType = returnStat.getReturnedExpr().accept(getType);
        if (hasRefrence(returnType))
            instructionList.add("areturn");
        else
            instructionList.add("ireturn");
        return null;
    }

    public Void visit(Break breakStat) {
        instructionList.add("goto" + breaks.peek());
        return null;
    }

    public Void visit(Continue continueStat) {
        instructionList.add("goto" + continues.peek());
        return null;
    }

    public Void visit(Skip skip) {
        return null;
    }


    public Void visit(IncStatement incStatement) {
        Plus plus = new Plus(incStatement.getOperand(),new IntValue(1));
        Assign assign = new Assign(incStatement.getOperand(),plus);
        assign.accept(this);
        return null;
    }

    public Void visit(DecStatement decStatement) {
        Minus minus = new Minus(decStatement.getOperand(),new IntValue(1));
        Assign assign = new Assign(decStatement.getOperand(),minus);
        assign.accept(this);
        return null;
    }


}
