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
import java.nio.file.Path;
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

    private Path file;
    ThreadPoolExecutor taskList;
    private StatdocItemHub hub;
    private String type;

    public StataAnalyseDtaFileTask(Path file, String type,
            StatdocItemHub hub, ThreadPoolExecutor taskList) {
        this.file = file;
        this.taskList = taskList;
        this.type = type;
        this.hub = hub;
    }

    @Override
    public void run() {
        Thread.currentThread().setName(
                " Stata data File Task for File: " + file);

        FileItem dtaFileItem = hub.createFile(file, type);

        try {
            String cmd = null;

            String c = "use ";
            String t = "stata";
            if (file.toString().toLowerCase().endsWith("csv")) {
                dtaFileItem.put("_runCommand", "import delimited ");
                c = "import delimited ";
                t = "csv";
            } else if (file.toString().toLowerCase().endsWith("raw")) {
                dtaFileItem.put("_runCommand", "import delimited ");
                c = "import delimited ";
                t = "raw";
            } else if (file.toString().toLowerCase().endsWith("xls")
                    || file.toString().toLowerCase().endsWith("xlsx")) {
                dtaFileItem.put("_runCommand", "import excel ");
                c = "import excel ";
                t = "excel";
            }

            long id = file.toFile().lastModified();
            dtaFileItem.put("lastmodified", id);
            File output = new File(hub.outputDir.toFile(), "derived/analyse_dta_"
                    + file.getFileName() + "_" + id + ".smcl");

            // stop execution if no Stata exec
            if (hub.getStataPath() == null) {
                dtaFileItem.addWarning("Please provde a valid Stata "
                        + "executatble in statdoc.properties.");
                return;
            }

            if (!output.exists()) {

                cmd = c + " \"" + dtaFileItem.getPath().toAbsolutePath()
                        + "\"";

                // System.out.println( cmd );

                Map<String, Object> data = new HashMap<String, Object>();
                data.put("loadCommand", cmd);
                data.put(
                        "maxvarobs",
                        hub.getProp().getProperty("statdoc.stata.maxvarobs",
                                "" + Integer.MAX_VALUE));
                data.put(
                        "catcutoff",
                        hub.getProp().getProperty("statdoc.stata.catcutoff",
                                "100" ));
                StataUtils.runTemplate("analyse-dta", hub.getStataPath(), data,
                        output);
            }

            BufferedReader reader = new BufferedReader(new FileReader(output));

            StringBuilder sb = new StringBuilder();

            Item currentItem = dtaFileItem;

            String line;
            boolean innotes = false;
            String notes = "";
            while ((line = reader.readLine()) != null) {
                // System.out.println(line);
                String cleanline = StataUtils.smcl2plain(line, true).trim();
                if (cleanline.startsWith("=====")) {

                    currentItem.setContent(StataUtils.smcl2html(sb.toString(),
                            true));
                    currentItem.setSummary(notes);

                    // start a new item
                    innotes = false;
                    notes = "";
                    sb = new StringBuilder();
                    String var = cleanline.replaceAll("=====", "");
                    currentItem = hub.createVariable(var, "variable:" + t,
                            dtaFileItem);
                    dtaFileItem.addChild(currentItem);

                    // add a subsample warning
                    if (dtaFileItem.containsKey("_subsample")) {
                        currentItem.addWarning("These statistics are based on"
                                + " a random subsample of "
                                + dtaFileItem.get("_subsample")
                                + " observations.");
                    }

                } else if (cleanline.startsWith("_@NOTES")) {
                    innotes = true;
                } else if (innotes) {
                    notes += StataUtils.smcl2plain(line, true);
                    sb.append(line);
                    sb.append("\n");
                } else {

                    hub.checkAndAddMetadata(currentItem, cleanline);
                    sb.append(line);
                    sb.append("\n");
                }
            }

            currentItem.setContent(StataUtils.smcl2html(sb.toString(), true));

            if (dtaFileItem.containsKey("_subsample")) {
                dtaFileItem.addWarning("All statistics are based on a random"
                        + " subsample of " + dtaFileItem.get("_subsample")
                        + " observations. You can change this by increasing"
                        + " <code>statdoc.stata.maxvarobs</code> in"
                        + " <code>statdoc.properties</code> or"
                        + " by setting a <code>@statdoc.full</code> flag "
                        + " for the dataset with"
                        + " <code>notes _dta:@statdoc.full</code>.");
            }

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
