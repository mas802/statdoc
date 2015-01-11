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
 * Task to process a Stata data file and set off StataVarTask for each variable found.
 * 
 * @author Markus Schaffner
 *
 * 
 */
@Deprecated
public class StataDtaFileTask implements Task {

    File file;
    File rootDir;
    ThreadPoolExecutor taskList;
    String type;

    public StataDtaFileTask(File rootDir, File file, String type,
            ThreadPoolExecutor taskList) {
        this.file = file;
        this.rootDir = rootDir;
        this.taskList = taskList;
        this.type = type;
    }

    @Override
    public void run() {
        Thread.currentThread()
                .setName(" Stata dta File Task for File: " + file);

        StatdocItemHub hub = StatdocItemHub.getInstance();

        long id = file.lastModified();
        
        FileItem dtaFileItem = hub.createFile(file, rootDir, type);

        if (file.getName().endsWith("csv")) {
            dtaFileItem.put("_runCommand", "import delimited ");
        }

        File dofile = new File(hub.outputDir, "templates/dta.do");

        File output = new File(hub.outputDir, "derived/dta_" + file.getName()
                + "_" + id + ".smcl");

        String oldcontent = "";
        if (output.exists()) {
            try {
                oldcontent = new String(Files.readAllBytes(Paths.get(output
                        .toURI())));
            } catch (IOException e) {
                dtaFileItem.addWarning("There was and error processing this file: " + e.getMessage() );
                e.printStackTrace();
            }
        }

        StataRunner sr = hub.getEngine(dtaFileItem);
        synchronized (sr) {

            try {
                sr.dorun("do \"" + dofile.getAbsolutePath() + "\" \""
                        + output.getAbsolutePath() + "\"");

            } catch (IOException e) {
                dtaFileItem.addWarning("There was and error processing this file: " + e.getMessage() );
                e.printStackTrace();
            }

            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                dtaFileItem.addWarning("There was and error processing this file: " + e.getMessage() );
                e.printStackTrace();
            }

            // variable to check whether something has changed
            boolean force = true;

            try {
                String content = new String(Files.readAllBytes(Paths.get(output
                        .toURI())));

                force = !content.equals(oldcontent);

                String c = StataUtils.smcl2html(content, true);
                String p = StataUtils.smcl2plain(content, false);

                dtaFileItem.setContent(c);

                String[] lines = p.trim().split("\n");
                for (String line : lines) {
                    hub.checkAndAddMetadata(dtaFileItem, line);
                }

            } catch (IOException e) {
                dtaFileItem.addWarning("There was and error processing this file: " + e.getMessage() );
                e.printStackTrace();
            }

            String describe = null;
            try {

                // here we could opt to write a smcl log file and read that (?)
                // also we could have a do file in templates to run
                describe = sr.run("describe, fullnames");

                assert (!describe.trim().equals(""));

                if (describe != null) {
                    // System.out.println(describe);
                    String r = describe;

                    // remove stata line breaks
                    r = r.replaceAll("[\r|\n]+> ", "");

                    // cut off top info
                    r = r.replaceAll(
                            ".*?[\r|\n]+Con.*?[\r|\n]+  obs.*?[\r|\n]+.*?[\r|\n]+.*?[\r|\n]+",
                            "");
                    r = r.replaceAll(".*?[\r|\n]+variable name.*?[\r|\n]+", "");

                    // remove lines
                    r = r.replaceAll(".*?---[\r|\n]+", "");

                    // bring long variable names into one line
                    r = r.replaceAll("[\r|\n]+                ", "   ");

                    // cut off bottom info
                    int i = r.indexOf("Sorted by:");
                    r = r.substring(0, i);
                    r = r.trim();

                    String lines[] = r.split("[\r|\n]+");

                    for (String l : lines) {

                        if (l.length() > 8) {
                            StataVar stataVar = new StataVar(l);
                            String type = stataVar.getType();
                            if (type.startsWith("str")) {
                                type = "str";
                            }
                            VariableItem varItem = hub.createVariable(
                                    stataVar.getName(), type, dtaFileItem);

                            dtaFileItem.addChild(varItem);

                            if (taskList != null) {
                                taskList.execute(new StataVarTask(rootDir,
                                        varItem, stataVar, dtaFileItem, force,
                                        taskList));
                            }
                        }
                    }

                    describe += "\n" + sr.run("list * in 1/5");

                }

            } catch (Exception ex) {
                dtaFileItem
                        .addWarning("There was a problem with running Stata: "
                                + ex.getMessage());
                System.err.println(file.toString());
                System.err.println(describe);

                ex.printStackTrace();
            } finally {
                Thread.currentThread().setName(
                        "Thread " + Thread.currentThread().getId());
            }
        }
    }

}
