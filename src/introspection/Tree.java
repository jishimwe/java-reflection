package introspection;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class Tree {

    private Node root;

    private class Node {
        private Class c;
        private Set<Node> subNodes;

        private Node(Class c) {
            this.c = c;
            this.subNodes = new HashSet<Node>();
        }

        private Node() {
            this.c = null;
            this.subNodes = null;
        }

        public Class getNode() { return this.c; }

        public boolean inSubClass(Class c) {
            assert c != null;
            if (this.subNodes.isEmpty())
                return false;
            for (Node nc : this.subNodes) {
                if (nc.getNode() == c)
                    return true;
            }
            return false;
        }

        public LinkedList<Class> getSubNodes() {
            if (subNodes.isEmpty())
                return null;
            LinkedList<Class> classList = new LinkedList<Class>();
            for (Node nc : subNodes) {
                classList.add(nc.getNode());
            }
            return classList;
        }

        public Set<Node> getSubClassesNode() { return this.subNodes; }

        public boolean addClass(Class superC, Class c) {
            Node newNode = new Node(c);
            if (this.c == c) { // Already in the tree?
                return true;
            } else if (this.c == superC) { // We're at the super class level
                if (this.inSubClass(c))
                    return true;
                this.addToSubNodes(newNode);
                return true;
            }
            boolean done;
            for (Node nc : this.subNodes) {
                done = nc.addClass(superC, c);
                if (done)
                    return true;
            }
            return false;
        }

        public void addToSubNodes(Node n) {
            for (Node nc : subNodes) {
                if (nc.getNode() == n.getNode())
                    return;
            }
            subNodes.add(n);
        }
    }

    public Tree() {
        this.root = null;
    }

    public Node getRoot() {
        return root;
    }

    public Class getRootClass() {
        return root.getNode();
    }

    public boolean addClassTree(Class superC, Class c) {
//        System.out.println("Adding a class to the tree");
        Node newNode = new Node(c);
        if (this.root == null) { // The tree was not initialized
            this.root = new Node(superC);
            this.root.subNodes.add(newNode);
            return true;
        }
        if (this.root.getNode() == c) { // Already in the tree
            Node thisNode = this.root;
            this.root = new Node(superC);
            this.root.addToSubNodes(thisNode);
            return true;
        }
        if (this.root.getNode() == superC) { // We're at super class level
            if (this.root.inSubClass(c))
                return true;
            this.root.subNodes.add(newNode);
            return true;
        }
        boolean done;
        for (Node nc : this.root.subNodes) { // We look through the subclasses
            done = nc.addClass(superC, c);
            if (done)
                return true;
        }
        return false;
    }

    private void printTreeRec(String tab, Node n) {
//        System.out.println("In Print introspection.Tree Rec");
        if (n == null)
            return;
        String s = n.c.getName().substring(n.c.getName().lastIndexOf('.') + 1);
        System.out.println(tab+"| " + s);
//        System.out.println("--| " + n.getNode().getName());
        int i = 0;
        for (Node nc : n.getSubClassesNode()) {
            printTreeRec(tab.concat("__"), nc);
            i++;
        }
//        System.out.println(tab + " >>" + i + " Subclasses");
    }

    public void printTree() {
//        System.out.println("In print tree");
        if (this.root == null) {
            System.out.println("introspection.Tree was null");
            return;
        }
//        System.out.println(root.getNode().getPackageName());
        int i = 0;
        System.out.println(root.getNode().getName());
        for (Node nc : this.root.subNodes) {
            printTreeRec("__", nc);
            i++;
        }
//        System.out.println(i + " Subclasses");
    }
}