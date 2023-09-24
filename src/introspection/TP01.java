package introspection;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.lang.Class;
import java.util.*;


public class TP01
{

    public static void introspectionTest(Class cls)
    {
        Constructor[] constructors = cls.getDeclaredConstructors();
        Constructor[] extCtrs = cls.getConstructors();
        Method[] innerMethods = cls.getDeclaredMethods();
        Method[] allMethods = cls.getMethods();
        Class[] classes = cls.getClasses();
        Class[] innerClasses = cls.getDeclaredClasses();
        Field[] innerFields = cls.getDeclaredFields();
        Field[] fields = cls.getFields();
        Class[] interfaces = cls.getInterfaces();
        Annotation[] annotations = cls.getAnnotations();
        String clsName = cls.getName();

        System.out.println("Name : " + clsName);
        System.out.println("Super class : " + cls.getSuperclass());

        if (cls.isInterface())
            System.out.println(clsName + " is an interface");
        if (cls.isLocalClass())
            System.out.println(clsName + " is a local class");
        if (cls.isPrimitive())
            System.out.println(clsName + " is a primitive");
        if (cls.isMemberClass())
            System.out.println(clsName + " is a synthetic class");
        if (cls.isAnonymousClass())
            System.out.println(clsName + " is an anonymous class");

        System.out.println("Constructors : " + extCtrs.length);
        printArray(extCtrs);

        System.out.println("Declared Constructors : " + constructors.length);
        printArray(constructors);

        System.out.println("Implements : " + innerMethods.length);
        printArray(innerMethods);

        System.out.println("Classes : " + classes.length);
        printArray(classes);

        System.out.println("Inner Classes : " + innerClasses.length);
        printArray(innerClasses);

        System.out.println("Interfaces : " + interfaces.length);
        printArray(interfaces);

        System.out.println("Methods : " + allMethods.length);
        printArray(allMethods);

        System.out.println("Declared Methods : " + innerMethods.length);
        printArray(innerMethods);

        System.out.println("Declared Fields : " + innerFields.length);
        printArray(innerFields);

        System.out.println("Fields : " + fields.length);
        printArray(fields);

        System.out.println("Annotations : " + annotations.length);
        printArray(annotations);

    }

    private static void printArray(Object[] arr)
    {
        if (arr.length == 0) {
            System.out.println("\tArray is empty");
            return;
        }
        for (Object o : arr)
        {
            System.out.print("\t");
            System.out.println(o);
        }
    }

    public static void intercessionTest(String className) {
        try {
            Class cls = Class.forName(className);
            Constructor c = cls.getConstructor();
            Object objClass = c.newInstance();
            Method myAdd = cls.getMethod("add", Object.class);
            Random rand = new Random();
            for (int i = 0; i < 10; i++) {
                myAdd.invoke(objClass, rand.nextInt());
            }

            Class compClass = Class.forName("java.util.Comparator");
            Class intClass = Class.forName("java.lang.Integer");

            Object compObj = c.newInstance();
            Method natComp = compClass.getMethod("naturalOrder");
            Method revComp = compClass.getMethod("reverseOrder");
            Method listSort = cls.getMethod("sort", Comparator.class);

            System.out.println("\n Sorted in reverse order");
            Object natOrd = natComp.invoke(compObj);
            listSort.invoke(objClass, natOrd);
            System.out.println(objClass);

            System.out.println("\n Sorted in reverse order");
            Object revOrd = revComp.invoke(compObj);
            listSort.invoke(objClass, revOrd);
            System.out.println(objClass);
        }
        catch (Exception e) {
            System.err.println("Error in intercessionTest "+ e);
        }
    }

    public static void reflectionRec(Class c, Tree classTree) {
        if (c == null)
            return;
        Class s = c.getSuperclass();
        if (s != null) {
            classTree.addClassTree(s, c);
        }
        reflectionRec(s, classTree);
    }

    public static Tree reflectionTest(String packageName)
    {
        try {
            Set<Class> classes = ClassFinder.getClassesInPackage(packageName);
            Tree classTree = new Tree();
            Class obj = Class.forName("java.lang.Object");
            for (Class s : classes) {
                Class superClass = s.getSuperclass();
                if (superClass == null) { // if it has no superclass, (should never happen ...)
                    continue;
                }
                if (superClass.getName().contains(packageName)) { // If the class is in the package
                    reflectionRec(s, classTree);
                    classTree.addClassTree(superClass, s);
                }
                else { // When we reach a class outside the package
                    classTree.addClassTree(superClass, s);
                    if (!superClass.getName().equals(obj.getName())) // if the first class outside the package isn't Object, we add it the tree as a subclass of Object
                        classTree.addClassTree(obj, superClass);
                }
            }
            return classTree;
        }
        catch (Exception e) {
            System.err.println("Error in reflectionInterface " + e);
        }
        return null;
    }

    public static void printResults(String classIntrospection, String classInterssetion, String packageName) throws ClassNotFoundException {
        System.out.println("introspection.TP01");
        System.out.println("Part 01 - Introspection (Get class structure)");
        TP01.introspectionTest(Class.forName(classIntrospection));
        System.out.println("|______________________________________________________________________|\n");

        System.out.println("Part 02 - Intercession (Manipulate objects at runtime)");
        TP01.intercessionTest(classInterssetion);
        System.out.println("|______________________________________________________________________|\n");

        System.out.println("Part 03 - Reflection (Class hierarchy in a package)");
        Tree classTree = TP01.reflectionTest(packageName);
        classTree.printTree();
        System.out.println("|______________________________________________________________________|\n");
    }

    public static void main(String[] args) {
        try {
            if (args.length == 1) {
                Tree classTree = reflectionTest(args[0]);
                classTree.printTree();
            }
            if (args.length == 3) {
                printResults(args[0], args[1], args[2]);
            } else
                printResults("java.util.LinkedList", "java.util.LinkedList", "java.util.concurrent.locks");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
