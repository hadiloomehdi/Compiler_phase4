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
import toorla.symbolTable.exceptions.ItemNotFoundException;
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
    private Stack <String> breaks = new Stack<>();
    private Stack <String> continues = new Stack<>();
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
        instructionList.add("nop");

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

        instructionList.add("ifne " +  nStmt);
        instructionList.add(exit + ":");
        instructionList.add("nop");
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
            File file = new File("./src/toorla/artifact/" + classDeclaration.getName().getName() +".j");
            boolean fvar = file.createNewFile();
            if (fvar){
//                System.out.println("File has been created successfully");
            }
            else{
            }
        } catch (IOException e) {
            System.out.println("Exception Occurred:");
            e.printStackTrace();
        }
        try {
            fw = new FileWriter("./src/toorla/artifact/" + classDeclaration.getName().getName() + ".j");
        }catch (Exception e) {
        }
        instructionList.add(".class public " + classDeclaration.getName().getName() );
        String father;
//            System.out.println(classDeclaration.getParentName());
        if(classDeclaration.getParentName().getName() == null) {
            father = "Any";
        }
        else
            father = classDeclaration.getParentName().getName();
        instructionList.add(".super " + father );
        //field
        for (ClassMemberDeclaration cmd : classDeclaration.getClassMembers())
            if (cmd instanceof FieldDeclaration)
                cmd.accept(this);
        // constructor
        instructionList.add(".method public <init>()V");
        instructionList.add("aload_0");
        instructionList.add("invokespecial " + father +"/<init>()V");
        for (ClassMemberDeclaration cmd : classDeclaration.getClassMembers())
            if (cmd instanceof FieldDeclaration && ((FieldDeclaration)cmd).getType() instanceof StringType) {
                instructionList.add("aload_0");
                instructionList.add("ldc \"\" ");
                instructionList.add("putfield " + classDeclaration.getName().getName() + "/" + ((FieldDeclaration)cmd).getIdentifier().getName() + " " + "Ljava/lang/String;");
            }
        instructionList.add("return");
        instructionList.add(".end method");

        //method
        for (ClassMemberDeclaration cmd : classDeclaration.getClassMembers())
            if (cmd instanceof MethodDeclaration)
                cmd.accept(this);

        writeInstructions();
        instructionList.clear();
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
        System.out.println("1111");
        equalsExpr.getLhs().accept(this);
        equalsExpr.getRhs().accept(this);
        Type lSideType = equalsExpr.getLhs().accept(getType);
        SymbolTable.top().print();
        if (lSideType instanceof IntType || lSideType instanceof BoolType){
            String L1 = "TRUE_" + (lableCounter++);
            String L2 = "FALSE_" + (lableCounter++);
            instructionList.add("if_icmpeq " + L1);
            instructionList.add("iconst_0");
            instructionList.add("goto " + L2);
            instructionList.add(L1 + ":");
            instructionList.add("iconst_1");
            instructionList.add(L2 + ":");
        }
        else if (lSideType instanceof StringType){
            instructionList.add("invokevirtual java/lang/String/equals(Ljava/lang/Object;)Z");
        }
        else if (lSideType instanceof UserDefinedType){
            instructionList.add("invokevirtual java/lang/Object/equals(Ljava/lang/Object;)Z");
        }
        else {//////equal instancceof ArrayType


            Type singleType = ((ArrayType) lSideType).getSingleType();
            if (singleType instanceof IntType || singleType instanceof BoolType)
                instructionList.add("invokevirtual java/util/Arrays/equals([I[I)Z");
            else if (singleType instanceof StringType)
                instructionList.add("invokevirtual java/util/Arrays/equals([java/lang/String;[java/lang/String;)Z");
            else if (singleType instanceof UserDefinedType) {
                instructionList.add("invokevirtual java/util/Arrays/equals([Ljava/lang/Object;[Ljava/lang/Object;)Z");
                System.out.println("aaaa");
            }
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
        if_cmpXX(gtExpr, "if_icmpgt");
        return null;
    }

    public Void visit(LessThan lessThanExpr) {
        if_cmpXX(lessThanExpr, "if_icmplt");
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
        notExpr.getExpr().accept(this);
        String L1 = "TRUE_" + (lableCounter++);
        String L2 = "FALSE_" + (lableCounter++);
        instructionList.add("ifeq " + L1);
        instructionList.add("iconst_0");
        instructionList.add("goto " + L2);
        instructionList.add(L1 + ":");
        instructionList.add("iconst_1");
        instructionList.add(L2 + ":");
        return null;
    }

    public Void visit(MethodCall methodCall) { /////////////////////////////////////////////need checke
        methodCall.getInstance().accept(this);
        for (Expression e : methodCall.getArgs())
            e.accept(this);
        String args = "";
        Type type = methodCall.getInstance().accept(getType);
        try {
            SymbolTable symbolT = ((ClassSymbolTableItem) SymbolTable.top().get("class_" + ((UserDefinedType)type).getClassDeclaration().getName().getName())).getSymbolTable();
            SymbolTableItem symbolTI = symbolT.get("method_" + methodCall.getMethodName().getName());

            for (Type t : ((MethodSymbolTableItem)symbolTI).getArgumentsTypes()) {
//                t.accept(this);
                args +=  convertType(t);
            }
        }catch (ItemNotFoundException e){

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
        for (ParameterDeclaration arg : methodDeclaration.getArgs())
            arg.accept(this);
        String access,paramType= "",returnType;
        if (methodDeclaration.getAccessModifier() == AccessModifier.ACCESS_MODIFIER_PRIVATE )
            access = "private";
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
            SymbolTableItem item = SymbolTable.top().get("var_" + varName);
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
            SymbolTableItem item = SymbolTable.top().get("var_" + localVarName);
            return item.getDefinitionNumber();
        } catch (Exception exc) {
            exc.printStackTrace();
        }
        return -1;
    }

    public Void visit(Identifier identifier) {///////////////need work
        if (identifierIsField(identifier.getName())) {
            try {
                instructionList.add("aload 0");
                Type fieldType = ((FieldSymbolTableItem) SymbolTable.top().get("var_" + identifier.getName())).getFieldType();
                String fieldTypeName = convertType(fieldType);
                instructionList.add("getfield " + getType.currentClass.getName().getName() + "/" + identifier.getName() + " " + fieldTypeName);
            } catch (Exception exp) {
                // dont occur
            }
        }
        else {
            int index = getIndexLocalVar(identifier.getName());
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
        if (intValue.getConstant()<=5)
            instructionList.add("iconst_" + intValue.getConstant());
        else
            instructionList.add("ldc " + intValue.getConstant());
        return null;
    }

    public String getTypeName(Type type) {
        if (type instanceof IntType)
            return "int";
        if (type instanceof BoolType)
            return "boolean";
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
        String type =  convertType(fieldCall.accept(getType));

        instructionList.add("getfield " + obj + "/" + fieldName + ' '+ type);
        return null;
    }

    public Void visit(ArrayCall arrayCall) {
        Type insType = arrayCall.getInstance().accept(getType);
        Type singleType = ((ArrayType)insType).getSingleType();
        arrayCall.getInstance().accept(this);
        arrayCall.getIndex().accept(this);
        if (hasRefrence(singleType))
            instructionList.add("aaload");
        else
            instructionList.add("iaload");
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
        if (printType instanceof ArrayType)
        {
            instructionList.add("invokestatic java/util/Arrays/toString([" +
                    convertType(((ArrayType) printType).getSingleType()) +  ")Ljava/lang/String;");
            instructionList.add("invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V");
        }
        else
            instructionList.add("invokevirtual java/io/PrintStream/println(" + convertType(printType) + ")V");
        return null;
    }

    public boolean hasRefrence(Type type) {
        return !(type instanceof IntType || type instanceof BoolType);
    }

    public Void visit(Assign assignStat) {
        if (assignStat.getLvalue() instanceof FieldCall) {
            FieldCall fieldCall = (FieldCall)assignStat.getLvalue();
            fieldCall.getInstance().accept(this);
            assignStat.getRvalue().accept(this);
            Type objType = ((FieldCall)assignStat.getLvalue()).getInstance().accept(getType);
            String objName = getTypeName(objType);
            String fieldName = ((FieldCall)assignStat.getLvalue()).getField().getName();
            String fieldType = convertType(assignStat.getLvalue().accept(getType));
            instructionList.add("putfield " + objName + "/" + fieldName + " " + fieldType);
        }
        else if ((assignStat.getLvalue() instanceof Identifier) && identifierIsField(((Identifier)assignStat.getLvalue()).getName())) {
            instructionList.add("aload 0");
            assignStat.getRvalue().accept(this);
            String objName = getType.currentClass.getName().getName();
            String fieldName = ((Identifier)assignStat.getLvalue()).getName();
            String fieldType = convertType(assignStat.getLvalue().accept(getType));
            instructionList.add("putfield " + objName + "/" + fieldName + " " + fieldType);
        }
        else {
            Type RType = assignStat.getRvalue().accept(getType);
            if (assignStat.getLvalue() instanceof ArrayCall) {
                ArrayCall arrayCall = (ArrayCall)assignStat.getLvalue();
                arrayCall.getInstance().accept(this);
                arrayCall.getIndex().accept(this);
                assignStat.getRvalue().accept(this);
                if (hasRefrence(RType))
                    instructionList.add("aastore");
                else
                    instructionList.add("iastore");
            }
            else {
                assignStat.getRvalue().accept(this);
                int index = getIndexLocalVar(((Identifier)assignStat.getLvalue()).getName());
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
        instructionList.add("goto " + breaks.peek());
        return null;
    }

    public Void visit(Continue continueStat) {
        instructionList.add("goto " + continues.peek());
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
