package proxy;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class TP02 {

    enum LogLevel {NONE, LOW, HIGH, FULL}

    private static class GenericInvocationHandler implements InvocationHandler {

        PrintWriter out;
        HashMap<String, List<Long>> data;
        LogLevel level;
        Object obj;

        public GenericInvocationHandler(OutputStream out, LogLevel l, Object obj) {
            this.out = new PrintWriter(out);
            this.level = l;
            this.obj = obj;
            this.data = null;
        }

        public GenericInvocationHandler(HashMap<String, List<Long>> data, LogLevel l, Object obj, PrintWriter out) {
            this.data = data;
            this.out = out;
            this.obj = obj;
            this.level = l;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            long begin = System.nanoTime();
            Object res = method.invoke(this.obj, args);
            long end = System.nanoTime();

            if (out != null) { // If an output for the logging has been specified
                printLog(out, method, (end - begin), args);
            }

            if (data != null) { // If a data output has been specified
                String key = method.getName();
                long t = end - begin;
                if (this.data.isEmpty() || !this.data.containsKey(key))
                    this.data.put(key, new LinkedList<>());
                data.get(key).add(t);
            }
            return res;
        }

        public void printLog(PrintWriter pw, Method m, long time, Object[] args) {
//            pw.println("");
            String s = "[PROXY] Method invoked: ";
            String s1 = "[PROXY] Execution time: ";
            switch (level) {
                case LOW ->
                    pw.println(s + m.getName());
                case HIGH -> {
                    pw.println(s + m.getName());
                    pw.println(s1 + time);
                }
                case FULL -> {
                    String sign = getSignature(m, args);
                    pw.println(s + sign);
                    pw.println(s1 + time);
                }
            }
        }

        public String getSignature(Method m, Object[] args) {
            String sig;
            try {
                Field field = Method.class.getDeclaredField("signature");
                field.setAccessible(true);
                sig = (String) field.get(m);
                if(sig != null)
                    return sig;
            } catch (NoSuchFieldException | IllegalAccessException ignored) {  }

            StringBuilder sb = new StringBuilder(m.getReturnType().getSimpleName());
            sb.append(" ").append(m.getName()).append("(");
            int i = 0;
            int to = m.getParameterCount();
            for (Class<?> c : m.getParameterTypes()) {
                sb.append(c.getSimpleName()).append(" ").append(args[i].toString());
                if (i < to-1)
                    sb.append(", ");
                i++;
            }
            sb.append(")");
            return sb.toString();
        }
    }

    private static class CompareLong implements Comparator<Long> {

        @Override
        public int compare(Long o1, Long o2) {
            return Long.compare(o1, o2);
        }
    }
    public static Object newProxy(Class c, LogLevel l, HashMap<String, List<Long>> data, PrintWriter out) {
        try {
            Constructor cons = c.getConstructor();
            Object obj = cons.newInstance();
            InvocationHandler h = new GenericInvocationHandler(data, l, obj, out);
            return Proxy.newProxyInstance(c.getClassLoader(), c.getInterfaces(), h);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Class<?>[] getInterfaces(Class<?> c) {
        List<Class<?>> result = new LinkedList<Class<?>>();
        if (c.isInterface()) {
            result.add(c);
        } else {
            do {
                addInterfaces(c, result);
                c = c.getSuperclass();
            } while(c != null);
        }
        for (int i = 0; i < result.size(); ++i) {
            addInterfaces(result.get(i), result);
        }
        Set<Class<?>> intfSet = new HashSet<Class<?>>(result);
        return intfSet.toArray(new Class<?>[intfSet.size()]);
    }

    public static void addInterfaces(Class<?> c, List<Class<?>> list) {
        list.addAll(Arrays.asList(c.getInterfaces()));
    }

    public static void printResults(Class c, File f, String output, LogLevel l) {
        try {
            PrintWriter out;
            if (f != null)
                out = new PrintWriter(new FileOutputStream(f));
            else
                if (output.equals("err"))
                    out = new PrintWriter(System.err);
                else
                    out = new PrintWriter(System.out);
            List<Integer> lProxy = (List<Integer>) newProxy(c, l, null, out);
            Random r = new Random();
            boolean done;
            for (int i = 0; i < 10; i++) {
                done = lProxy.add(r.nextInt());
                if(!done)
                    System.out.println(i +  " not added to the list");
            }
        }
        catch (Exception e) {
            System.out.println("What is in the proxy? " + e.getMessage());
            e.printStackTrace();
        }

    }

    public static void printResults2() {
        try {
            File file = new File("./proxy.log");
            PrintWriter pw = new PrintWriter(file);
            List<Integer> lProxy = (List<Integer>) newProxy(LinkedList.class, LogLevel.HIGH, null, pw);
            Random r = new Random();
            for (int i = 0; i < 10; i++) {
                lProxy.add(r.nextInt());
            }
            System.out.println("[Proxy] " + lProxy);
        }
        catch (Exception e) { e.printStackTrace(); }
    }

    public static void profilerTest(Class c, File f, LogLevel l, OutputStream out) {
        try {
            HashMap<String, List<Long>> data = new HashMap<String, List<Long>>();
            PrintWriter output = null;
            if (out != null)
                output = new PrintWriter(out);
            for (int i = 0; i <= 100; i++) {
                List<Long> listProxy = (List<Long>) newProxy(c, l, data, output);
                Random rand = new Random();
                for (int j = 0; j < 1000; j++) {
                    listProxy.add(rand.nextLong());
                }
                int lSize = listProxy.size();
                System.out.println("List size : " + lSize);
                listProxy.removeIf(n -> n < 0);
                lSize = listProxy.size();
                System.out.println("List size : " + lSize);
                Comparator<Long> comp = new CompareLong();
                listProxy.sort(comp);
                long toAdd = -1;
                for (int j = 0; j < lSize; j += 10) {
                    listProxy.add(j, toAdd);
                }
                lSize = listProxy.size();
                System.out.println("List size : " + lSize);
                for (Long pr: listProxy) { System.out.println(pr); }
            }
            if (output != null) {
                output.flush();
                output.close();
            }
            if (f == null)
                return;
            PrintWriter pw = new PrintWriter(f);
            for (Map.Entry<String, List<Long>> entry : data.entrySet()) {
                pw.print(entry.getKey());
                pw.print(":");
                pw.println(entry.getValue());
            }
            pw.flush();
            pw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void profilerTest(Class c, File f) {
        profilerTest(c, f, LogLevel.HIGH, null);
    }

    public static void main(String[] args) {
        File dir = new File("proxy_stats");
        if (!dir.exists()){
            if (!dir.mkdir())
                System.exit(-1);
        }
        if(args.length == 0) { // Default profiling
            profilerTest(ArrayList.class, new File(dir, "ArrayList.csv"));
            profilerTest(CopyOnWriteArrayList.class, new File(dir, "CopyOnWriteArrayList.csv"));
            profilerTest(LinkedList.class, new File(dir, "LinkedList.csv"));
            profilerTest(Vector.class, new File(dir, "Vector.csv"));
            try {
                profilerTest(Class.forName("java.util.Stack"), new File(dir, "Stack.csv"));
            } catch (Exception e) { e.printStackTrace(); }
            return;
        }
        try {
            if (args[0].equals("p") && args.length > 1) { // Just logging
                LogLevel l = LogLevel.valueOf(args[args.length-1]);
                if (args[1].equals("out")) // Printing to a standard output
                {
                    profilerTest(Vector.class, new File(args[2]), l, System.out);
                } else if (args[1].equals("err")) { // Printing to error output
                    profilerTest(Class.forName(args[3]), new File(dir, args[2]), l, System.err);
                } else { // Printing log to file
                    profilerTest(Class.forName(args[3]), new File(dir, args[2]), l, new FileOutputStream(args[1]));
                }
                return;
            }
            for(String className : args) { // Profiling classes given in args
                    String s = className.substring(className.lastIndexOf('.')) + ".csv";
                    profilerTest(Class.forName(className), new File(dir, s));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}
