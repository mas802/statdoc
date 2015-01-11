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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ThreadPoolExecutor;

import statdoc.items.FileItem;
import statdoc.items.StatdocItemHub;
import statdoc.items.VariableItem;
import statdoc.tasks.Task;

/**
 * Processes a StataVar for further information.
 * 
 * @author Markus Schaffner
 *
 */
@Deprecated
public class StataVarTask implements Task {

    ThreadPoolExecutor taskList;

    VariableItem var;
    FileItem dtaFile;
    
    boolean force;

    File rootDir;

    public StataVarTask(File rootDir, VariableItem var, StataVar sv,
            FileItem dtaFile, boolean force, ThreadPoolExecutor taskList) {
        this.var = var;
        this.dtaFile = dtaFile;
        this.taskList = taskList;
        this.rootDir = rootDir;
        this.force = force;

        var.put("type", sv.getType());
        var.put("valueLabel", sv.getValueLabel());
        var.put("format", sv.getFormat());
        var.put("label", sv.getLabel());

    }

    @Override
    public void run() {
        Thread.currentThread().setName(
                "Run " + this.getClass() + " stata var: " + var.getName()
                        + " in " + var.getFullName());

        StatdocItemHub hub = StatdocItemHub.getInstance();

        
        File dofile = new File(hub.outputDir, "templates/single-var.do");

        File output = new File(hub.outputDir, "derived/single-var_"
                + var.getFullName() + ".smcl");

        if (force || !output.exists()) {
            StataRunner sr = hub.getEngine( dtaFile );
            synchronized (sr) {

                String dostring = null;
                try {
                    dostring = "do \"" + dofile.getAbsolutePath() + "\" "
                            + var.getName() + " \"" + output.getAbsolutePath()
                            + "\"";
                    sr.dorun(dostring);

                 } catch (IOException e) {
                     System.err.println( dostring );
                    e.printStackTrace();
                    var.addWarning("There was and error processing this variable: " + e.getMessage() );
                }

            }

        }

        try {
            String content = new String(Files.readAllBytes(Paths.get(output
                    .toURI())));
            
            String c = StataUtils.smcl2html(content, true);
            var.setContent(c);
            
            String p = StataUtils.smcl2plain(content, false);

            String[] lines = p.trim().split("\n");
            for ( String line:lines ) {
                hub.checkAndAddMetadata(var, line);
            }
        } catch (IOException e) {
            e.printStackTrace();
            var.addWarning("There was and error processing this variable: " + e.getMessage() );
        }

        Thread.currentThread().setName(
                "Thread " + Thread.currentThread().getId());
    }

}
