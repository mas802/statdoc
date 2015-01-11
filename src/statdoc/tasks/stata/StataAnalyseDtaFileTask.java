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
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

import statdoc.items.FileItem;
import statdoc.items.Item;
import statdoc.items.StatdocItemHub;
import statdoc.tasks.Task;

/**
 * Task to process a Stata data file and gather information about its content.
 * 
 * @author Markus Schaffner
 * 
 */
public class StataAnalyseDtaFileTask implements Task {

    File file;
    File rootDir;
    ThreadPoolExecutor taskList;
    String type;

    public StataAnalyseDtaFileTask(File rootDir, File file, String type,
            ThreadPoolExecutor taskList) {
        this.file = file;
        this.rootDir = rootDir;
        this.taskList = taskList;
        this.type = type;
    }

    @Override
    public void run() {
        Thread.currentThread().setName(
                " Stata data File Task for File: " + file);

        StatdocItemHub hub = StatdocItemHub.getInstance();

        FileItem dtaFileItem = hub.createFile(file, rootDir, type);

        try {
            String cmd = null;
            
            String c = "use ";
            String t = "stata";
            if (file.getName().endsWith("csv")) {
                dtaFileItem.put("_runCommand", "import delimited ");
                c = "import delimited ";
                t = "csv";
            }
            if (file.getName().endsWith("raw")) {
                dtaFileItem.put("_runCommand", "import delimited ");
                c = "import delimited ";
                t = "raw";
            }

            long id = file.lastModified();
            dtaFileItem.put("lastmodified", id);
            File output = new File(hub.outputDir, "derived/analyse_dta_"
                    + file.getName() + "_" + id +".smcl");

            if (!output.exists()) {

                cmd =  c + " \""
                        + dtaFileItem.getFile().getAbsolutePath() + "\"";

                // System.out.println( cmd );
                
                Map<String, Object> data = new HashMap<String, Object>();
                data.put("loadCommand", cmd);
                StataUtils.runTemplate("analyse-dta", hub.getStataPath(), data, output);
            }

            BufferedReader reader = new BufferedReader(new FileReader(output));

            StringBuilder sb = new StringBuilder();

            Item currentItem = dtaFileItem;

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("=====")) {

                    currentItem.setContent(StataUtils.smcl2html(sb.toString(),
                            true));

                    sb = new StringBuilder();
                    String var = line.replaceAll("=====", "");
                    currentItem = hub.createVariable(var, "variable:" + t,
                            dtaFileItem);
                    dtaFileItem.addChild(currentItem);
                } else {

                    hub.checkAndAddMetadata(currentItem,
                            StataUtils.smcl2plain(line, true));
                    sb.append(line);
                    sb.append("\n");
                }
            }

            currentItem.setContent(StataUtils.smcl2html(sb.toString(),
                    true));

            reader.close();
        } catch (Exception ex) {
            dtaFileItem.addWarning("There was a problem with running Stata: "
                    + ex.getMessage());
            ex.printStackTrace();
        } finally {
            Thread.currentThread().setName(
                    "Thread " + Thread.currentThread().getId());
        }
    }
}
