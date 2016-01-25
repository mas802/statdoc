package statdoc.tests.tasks.r;

import java.io.*;
import java.awt.Frame;
import java.util.Enumeration;

import org.rosuda.JRI.Rengine;
import org.rosuda.JRI.REXP;
import org.rosuda.JRI.RVector;
import org.rosuda.JRI.RMainLoopCallbacks;

public class StandaloneRTest {

    //Right under your R class
    static {
     System.loadLibrary("jri");      
    }

    public static void main(String[] args) {
        System.out.println("Creating Rengine (with arguments)");
        String[] Rargs = { "--vanilla" };
        Rengine re = new Rengine(Rargs, false, null);
        System.out.println("Rengine created, waiting for R");
        if (!re.waitForR()) {
            System.out.println("Cannot load R");
            return;
        }
        
        REXP x;
        re.eval("print(1:10/3)");
        System.out.println(x=re.eval("iris"));
        RVector v = x.asVector();
        if (v.getNames()!=null) {
            System.out.println("has names:");
            for (Enumeration e = v.getNames().elements() ; e.hasMoreElements() ;) {
                System.out.println(e.nextElement());
            }
        }
        
        System.out.println("Done.");
    }
    
}