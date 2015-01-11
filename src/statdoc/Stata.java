/*
 *   Copyright 2014-2015 Markus Schaffner
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package statdoc;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import statdoc.items.StatdocItemHub;

import com.stata.sfi.SFIToolkit;

/**
 * Main class of Statdoc and entry into the execution .
 * 
 * Should read the configuration and initiate the main tasks . Has control over
 * the dispensation of threads.
 * 
 * @author Markus Schaffner
 * 
 */
public class Stata {

    private static class Interceptor extends PrintStream
    {
        public Interceptor(OutputStream out)
        {
            super(out, true);
        }
        
        @Override
        public void print(String s)
        {
            SFIToolkit.displayln(s);
            int i = SFIToolkit.pollnow();
            if ( i != 0 ) {
                throw new RuntimeException("canceled");
            }
            super.print(i + ":" + s);
        }
    }

    private static class ErrorInterceptor extends PrintStream
    {
        public ErrorInterceptor(OutputStream out)
        {
            super(out, true);
        }
        
        @Override
        public void print(String s)
        {
            SFIToolkit.errorln(s);
            int i = SFIToolkit.pollnow();
            if ( i != 0 ) {
                throw new RuntimeException("canceled");
            }
            super.print(i + ":" + s);
        }
    }
    
    public static int run(String [] args) {
        SFIToolkit.pollstd();
        
        PrintStream origOut = System.out;
        PrintStream interceptor = new Interceptor(origOut);
        System.setOut(interceptor);// just add the interceptor
       
        PrintStream origOutE = System.err;
        PrintStream interceptorE = new ErrorInterceptor(origOutE);
        System.setErr(interceptorE);// just add the interceptor
       
        /*
        String[] s = new String[] {
                "-o", "/Users/mas/statdoc/",
                "-s", "/Users/mas/Dropbox/Public/cameron/"
        };
        */
        
        try {
            Statdoc.main(args);
            SFIToolkit.pollnow();
        } catch (IOException e) {
            SFIToolkit.errorln("error" + e.getMessage() );
            e.printStackTrace();
        }
        SFIToolkit.displayln("test " + StatdocItemHub.getInstance().stats());
        return 0;
    }
    
    // javacall statdoc.Stata run
}
