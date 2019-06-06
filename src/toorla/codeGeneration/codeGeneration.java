package toorla.codeGeneration;

import toorla.ast.Program;
import toorla.ast.declaration.classDecs.ClassDeclaration;
import toorla.ast.declaration.classDecs.EntryClassDeclaration;
import toorla.ast.declaration.classDecs.classMembersDecs.AccessModifier;
import toorla.ast.declaration.classDecs.classMembersDecs.ClassMemberDeclaration;
import toorla.ast.declaration.classDecs.classMembersDecs.FieldDeclaration;
import toorla.ast.declaration.classDecs.classMembersDecs.MethodDeclaration;
import toorla.ast.declaration.localVarDecs.ParameterDeclaration;
import toorla.ast.expression.MethodCall;
import toorla.ast.statement.Block;
import toorla.ast.statement.Conditional;
import toorla.ast.statement.Statement;
import toorla.ast.statement.While;
import toorla.ast.statement.localVarStats.LocalVarDef;
import toorla.ast.statement.localVarStats.LocalVarsDefinitions;
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

    @Override
    public Void visit(Block block) {
        SymbolTable.push(new SymbolTable(SymbolTable.top()));
        for (Statement stmt : block.body)
            stmt.accept(this);
        SymbolTable.pop();
        return null;
    }

    @Override
    public Void visit(Conditional conditional) {
        SymbolTable.push(new SymbolTable(SymbolTable.top()));
        conditional.getThenStatement().accept(this);
        SymbolTable.pop();
        SymbolTable.push(new SymbolTable(SymbolTable.top()));
        conditional.getElseStatement().accept(this);
        SymbolTable.pop();
        return null;

    }

    @Override
    public Void visit(While whileStat) {
        SymbolTable.push(new SymbolTable(SymbolTable.top()));
        whileStat.body.accept(this);
        SymbolTable.pop();
        return null;
    }

    @Override
    public Void visit(LocalVarDef localVarDef) {
        try {
            SymbolTable.top()
                    .put(new LocalVariableSymbolTableItem(localVarDef.getLocalVarName().getName(), newLocalVarIndex));
        } catch (ItemAlreadyExistsException e) {
            LocalVarRedefinitionException ee = new LocalVarRedefinitionException(
                    localVarDef.getLocalVarName().getName(), localVarDef.line, localVarDef.col);
            localVarDef.addError(ee);
        }
        newLocalVarIndex++;
        return null;
    }

    @Override
    public Void visit(ClassDeclaration classDeclaration) {
        SymbolTable.pushFromQueue();
        try {
            File file = new File("../artifact" + classDeclaration.getName().getName() +".j");/////////need work
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
                father = "java/lang/Object";
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
        for (ClassMemberDeclaration cmd : classDeclaration.getClassMembers())
            if (cmd instanceof MethodCall)
                cmd.accept(this);
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
        try {
            LocalVariableSymbolTableItem paramItem = new LocalVariableSymbolTableItem(parameterDeclaration.getIdentifier().getName(), newLocalVarIndex);
            paramItem.setVarType(parameterDeclaration.getType());
            SymbolTable.top().put(
                    paramItem
            );

        } catch (ItemAlreadyExistsException e) {
            LocalVarRedefinitionException ee = new LocalVarRedefinitionException(
                    parameterDeclaration.getIdentifier().getName(), parameterDeclaration.line,
                    parameterDeclaration.col);
            parameterDeclaration.addError(ee);
        }
        newLocalVarIndex++;
        return null;
    }

    @Override
    public Void visit(MethodDeclaration methodDeclaration) {/////////////////main doesnt handle
        SymbolTable.pushFromQueue();
        try{
            String access,paramType= "",returnType;
            if (methodDeclaration.getAccessModifier() == AccessModifier.ACCESS_MODIFIER_PRIVATE )
                access = "praivate";
            else
                access = "public";
            for (ParameterDeclaration arg : methodDeclaration.getArgs())
                paramType += convertFieldType(arg.getType());
            returnType = convertFieldType(methodDeclaration.getReturnType());
            fw.write(".method " + access + " " + methodDeclaration.getName().getName() + "(" + paramType +")" + returnType );
        SymbolTable.pop();


        }catch (IOException e){}
//        SymbolTable.push(new SymbolTable(SymbolTable.top()));
//        for (ParameterDeclaration pd : methodDeclaration.getArgs())
//            pd.accept(this);
//        for (Statement stmt : methodDeclaration.getBody())
//            stmt.accept(this);
//        SymbolTable.pop();
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
            File file = new File("../artifact/Any.j");/////////need work
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

}
