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
package statdoc.tasks.stata;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
 
import statdoc.items.FileItem;

/**
 *
 * Class to run commands of a data file.
 * 
 * @author Markus Schaffner
 *
 */
@Deprecated
public class StataRunner {

    private Process ps;
    private BufferedReader in;
    private BufferedWriter out;

    boolean isbusy = false;
    
    /**
     * Class to run single commands of a data file.
     * 
     * This process is still a bit fragile, so only one datafile allowed
     * 
     * @param dataFileItem
     * @throws IOException
     */
    public StataRunner(String stataPath, FileItem dataFileItem ) throws IOException {
        startProcess(stataPath);
        
        String command = "use ";
        String options = "";
        if ( dataFileItem.containsKey("_runCommand") ) {
            command = dataFileItem.get( "_runCommand" ).toString();
        }
        if ( dataFileItem.containsKey("_runOptions") ) {
            options = ", " +dataFileItem.get( "_runOptions" ).toString();
        }
        run( command +" \"" + dataFileItem.getFile().getAbsolutePath() + "\"" + options );
    }



    public static String singleProcess(String statacmd, File output) throws IOException, InterruptedException {

        Runtime rt = Runtime.getRuntime();

        Process ps;
        // BufferedReader in;
        // BufferedWriter out;

        // System.out.println( statacmd );
        
        Path tempDir = Files.createTempDirectory("statdoc" );
        
        ps = rt.exec( statacmd, null, tempDir.toFile() );
        ps.waitFor();
        
        Files.copy( tempDir.resolve("analyse-dta.smcl"), output.toPath(), StandardCopyOption.REPLACE_EXISTING);
        
        return "";
    }

    
    private void startProcess(String stataPath) throws IOException {

        Runtime rt = Runtime.getRuntime();

        ps = rt.exec( stataPath );
        in = new BufferedReader(new InputStreamReader(ps.getInputStream()));
        out = new BufferedWriter(new OutputStreamWriter(ps.getOutputStream()));

        char[] buf = new char[10];

        // TODO suppressing here as I do not know how to do this differently
        @SuppressWarnings("unused")
        String str;
        while ((str = in.readLine()) != null) {
            // String line = str;
            // System.out.println("read: " + line);
            if (in.ready()) {
                in.mark(5);
                int a = in.read(buf);
                // System.out.println("num:" + a);
                if (a == 2 && buf[0] == '.' && buf[1] == 32) {
                    break;
                }
                in.reset();
            }
        }
    }

    public String run(String cmd) throws IOException {
        isbusy = true;
        StringBuffer content = new StringBuffer();

        out.append(cmd + "\n");
        out.flush();

        char[] buf = new char[10];

        String str;
        while ((str = in.readLine()) != null) {
            String line = str;
            // System.out.println("read: " + line);
            content.append(line + "\n");
            if (in.ready()) {
                in.mark(10);
                int a = in.read(buf);
                // System.out.println("num:" + a);
                if (a == 2 && buf[0] == '.' && buf[1] == 32) {
                    break;
                }
                try {
                    in.reset();
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println(content.toString());
                }
            }
            ;
        }

        return content.toString();
    }


    // NON-NESTED DO FILE ONLY SO FAR
    public String dorun(String cmd) throws IOException {
        isbusy = true;
        StringBuffer content = new StringBuffer();

        out.append(cmd + "\n");
        out.flush();

        // char[] buf = new char[10];

        String str;
        while ((str = in.readLine()) != null) {
            String line = str;
            // System.out.println("read: " + line);
            content.append(line + "\n");
            if (line.trim().startsWith("end of do-file")) {
                    break;
            }
            if (line.trim().startsWith("r(")) {
                throw new RuntimeException("Error executing do file " + cmd + " number: " + line + " content: " + content.toString());
            }
        }

        // FIXME crude implementation of waiting for the prompt (wait for a space to appear)
        while ( in.read() !=32 ) {
            // System.out.println( x );
        }
        
        return content.toString();
    }
}
