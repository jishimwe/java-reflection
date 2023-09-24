package instrumentation;

import java.io.PrintStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.Set;

import javassist.*;
import introspection.ClassFinder;

public class MyTransformer implements ClassFileTransformer {

    private static Set<String> sysClass;
    PrintStream filepath;
    PrintStream console = System.out;


    public MyTransformer(String path){
        try {
            sysClass = ClassFinder.getClassNamesInSystemObjects();
            if (path == null) {
                filepath = System.out;
                return;
            }
            if(path.equals("OUT")) {
                filepath = System.out;
            } else if (path.equals("ERR")) {
                filepath = System.err;
            } else {
                filepath = new PrintStream(path);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void premain(String options, Instrumentation ins) {
//        System.err.println(options);
        ins.addTransformer(new MyTransformer(options));
    }

    public byte[] transform(ClassLoader loader, String className, Class<?> cBR, ProtectionDomain pd, byte[] classfileBuffer) throws IllegalClassFormatException {
        try {
            if(isSystemClass(className))
                return null;

            ClassPool pool = ClassPool.getDefault();
            pool.importPackage("java.io");
            CtClass cc = pool.get(className.replaceAll("/", "."));
            CtMethod[] allMethods = cc.getDeclaredMethods();

            System.out.flush();
            System.setOut(console);
            for (CtMethod method : allMethods) {
                try {
                    if (isNativeOrAbstract(method) || method.isEmpty()) { // Didn't find a way to profile abstract or
                        // empty methods
//                        System.err.println(method.getName() +  " is either a native or abstract method");
                        return null;
                    }
                    String path = "System.out.";
                    StringBuilder sb = new StringBuilder("");
                    if (this.filepath != null) {
                        System.setOut(filepath);
                    }
                    method.addLocalVariable("beginTime", CtClass.longType);
                    sb.append("beginTime = System.nanoTime();");
                    method.insertBefore(sb.toString());

                    StringBuilder endBlock = new StringBuilder();
                    method.addLocalVariable("endTime", CtClass.longType);
                    method.addLocalVariable("execTime", CtClass.longType);
                    endBlock.append("endTime = System.nanoTime();\n");
                    endBlock.append("execTime = (endTime - beginTime);\n");
                    endBlock.append(path);
                    endBlock.append("println(\"").append(getSignature(method));
                    endBlock.append(" -- Execution time : \" + execTime);");
                    method.insertAfter(endBlock.toString());

                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
            return  cc.toBytecode();
        } catch (Throwable exc) {
            System.err.println(exc);
        }
        return null;
    }

    public String getSignature(CtMethod m) {
        String sig;
        try {
            StringBuilder sb = new StringBuilder(m.getReturnType().getSimpleName());
            sb.append(" ").append(m.getName()).append("(");
            int i = 0;
            CtClass[] ct = m.getParameterTypes();
            int to = ct.length;

            for (CtClass c : ct) {
                sb.append(c.getSimpleName()).append(" ");
                if (i < to-1)
                    sb.append(", ");
                i++;
            }
            sb.append(")");
            return sb.toString();
        } catch (Exception e) { e.printStackTrace();}
        return null;
    }

    public static boolean isSystemClass(String name) {
        return sysClass.contains(name);
    }

    public static boolean isNativeOrAbstract(CtMethod m) {
        return Modifier.isNative(m.getModifiers()) || Modifier.isAbstract(m.getModifiers());
    }

    public static void trace() {
        Throwable thw = new Throwable();
        if (thw.getStackTrace().length > 2) {
            StackTraceElement to = thw.getStackTrace()[1];
            StackTraceElement from = thw.getStackTrace()[2];
            if(!isSystemClass(from.getClassName())) {
                System.out.println(from.getClassName() + "." + from.getMethodName() + " --> " + to.getClassName() + "." + to.getMethodName());
            }
        }
    }
}