.class public MainTest
.super Any
.method public <init>()V 
aload_0
invokespecial Any/<init>()V
return
.end method
.field public x I
.method public main()I
.limit stack 1000
.limit locals 1000
bipush 2
newarray int
astore 1
bipush 2
newarray int
astore 2
getstatic java/lang/System/out Ljava/io/PrintStream;
ldc "hello"
invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V
bipush 0
ireturn
.end method
