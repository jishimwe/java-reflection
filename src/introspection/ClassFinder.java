package introspection;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClassFinder {

    public static void main(String[] args) throws Exception {
        for(Class c : getClassesInPackage("java.util.concurrent.locks")) {
            System.out.println(c.toGenericString());
        }
    }

    public static Set<Class> getClassesInPackage(String packageName) throws Exception {
        return getClassesFromNames(getClassNamesInPackage(packageName));
    }

    public static Set<Class> getClassesFromNames(Set<String> classNames) throws Exception {
        return classNames
                .stream()
                .map(name -> getClass(name))
                .collect(Collectors.toSet());
    }

    public static Class getClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            System.err.println(e.getMessage());;
        }
        return null;
    }

    public static Set<String> getClassNamesInPackage(String packageName) throws Exception {
        return getAllClassNames()
                .stream()
                .filter(name -> name.startsWith(packageName))
                .collect(Collectors.toSet());
    }

    public static Set<String> getAllClassNames() throws Exception {
        Set<String> classNames = getClassNamesInClassPath();
        classNames.addAll(getClassNamesInSystemObjects());
        return classNames;
    }

    public static Set<String> getClassNamesInSystemObjects() throws Exception {
        Set<String> classNames = new HashSet<>();
        FileSystem fs = FileSystems.getFileSystem(URI.create("jrt:/"));
        Path top = fs.getPath("/");
        Stream<Path> stream = Files.walk(top);
        stream.forEach(path -> {
            try {
                String classStr = path.toString();
                if(classStr.endsWith(".class")) {
                    String className = classStr.substring("/modules/".length(), classStr.lastIndexOf('.'));
                    className = className.substring(className.indexOf('/') + 1);
                    className = className.replace("/", ".");
                    if(!className.equals("module-info")) {
                        classNames.add(className);
//						System.out.println(className);
                    }
                }
            } catch(Throwable t) {
                System.err.println(t.getMessage());
            }
        });
        stream.close();
        return classNames;
    }

    public static Set<String> getClassNamesInClassPath() throws Exception {
        Set<String> classNames = new HashSet<>();
        for (String classpathEntry : System.getProperty("java.class.path").split(System.getProperty("path.separator"))) {
            if (classpathEntry.endsWith(".jar")) {
                File jar = new File(classpathEntry);
                JarInputStream jis = new JarInputStream(new FileInputStream(jar));
                JarEntry entry;
                while( (entry = jis.getNextJarEntry()) != null) {
                    if(entry.getName().endsWith(".class")) {
                        String className = entry.getName().replace("/", ".").substring(0, entry.getName().lastIndexOf('.'));
                        classNames.add(className);
//		            	System.out.println(className);
                    }
                }
                jis.close();
            } else {
                File dir = new File(classpathEntry);
                Path dirEntry = dir.toPath();
                Stream<Path> stream = Files.walk(dirEntry);
                stream.forEach(path -> {
                    try {
                        String classStr = path.toString().replace(File.separator, ".").substring(1);
                        if(classStr.endsWith(".class")) {
                            String className = classStr.substring(classpathEntry.length(), classStr.lastIndexOf('.'));
                            classNames.add(className);
//							System.out.println(className);
                        }
                    } catch(Throwable t) {
                        System.err.println(t.getMessage());
                    }
                });
                stream.close();
            }
        }
        return classNames;
    }

}