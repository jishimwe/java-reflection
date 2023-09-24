package main;

//import proxy.TP02;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;

public class Main {


    public static void main(String[] args) {
//        introspection.TP01.printResults();
//        proxy.TP02.printResults();
//        proxy.TP02.printResults2();
//        TP02.profilerTest(ArrayList.class, new File("./arrayListProfile.csv"));
        Point p = new Point(0, 1);
        Point p1 = new Point(100, 23);
        p.setColor(new Color(99999999));
        p1.setColor(new Color(11111124));
        System.out.println(p);
        System.out.println(p1);
    }
}
