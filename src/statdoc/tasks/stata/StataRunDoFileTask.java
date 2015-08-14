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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ThreadPoolExecutor;

import statdoc.items.CmdItem;
import statdoc.items.Item;
import statdoc.items.MatchItem;
import statdoc.items.FileItem;
import statdoc.items.StatdocItemHub;
import statdoc.tasks.Task;
import statdoc.utils.TemplateUtil;

/**
 * Task to (auto) run a do file
 * 
 * This task should should run a do file if it contains the @statdocrun
 * annotation. It spins of a vm task if an instruction for it is found and
 * should add the resulting file to the index.
 * 
 * @author Markus Schaffner
 * 
 */
public class StataRunDoFileTask implements Task {

    enum ReadMode {
        Commands, DocBlock, CommentBlock, MataBlock
    }

    private FileItem fileItem;
    ThreadPoolExecutor taskList;
    private StatdocItemHub hub;

    public StataRunDoFileTask(FileItem fileItem, StatdocItemHub hub, ThreadPoolExecutor taskList) {
        this.fileItem = fileItem;
        this.taskList = taskList;
        this.hub = hub;
    }

    @Override
    public void run() {

        Thread.currentThread().setName(
                " Stata run do file task for file: " + fileItem);

        /* do stuff */
        
        Path file = fileItem.getPath();
        
        try {
            long id = file.toFile().lastModified();

            File output = new File(hub.outputDir.toFile(), "derived/analyse_run_"
                    + file.getFileName() + "_" + id + ".smcl");

            // stop execution if no Stata exec
            if (hub.getStataPath() == null) {
                // TODO dtaFileItem.addWarning("Please provide a valid Stata "
                //        + "executable in statdoc.properties.");
                return;
            }

            if (!output.exists()) {

                String str = file.getFileName().toString();
                StataUtils.runDoFile( str.substring(0, str.lastIndexOf('.')), 
                        file.toAbsolutePath().toString() , hub.getStataPath(), 
                        output.toPath());
            }

            BufferedReader reader = new BufferedReader(new FileReader(output));

            StringBuilder sb = new StringBuilder();

            Item tempItem = new Item("", "", "");

            String line;
            while ((line = reader.readLine()) != null) {
                // System.out.println(line);
                String cleanline = StataUtils.smcl2plain(line, true).trim();
                if (cleanline.startsWith(">>>>>")) {

                    // produce a new FileItem from info
                    // currentItem = hub.createVariable(var, "variable:" + t,
                    //        dtaFileItem);
                    Map<String, Object> map = new HashMap<String, Object>();
                    map.put( "data", tempItem );
                    
                    String[] param = cleanline.substring(5).split("->");
                    
                    TemplateUtil.getInstance().evalVMtoFile(
                            new File(hub.sourceDir.toFile(), param[1].trim()),
                            param[0].trim(), map);                 
                    
                    // reset temp vars
                    sb = new StringBuilder();
                    tempItem = new Item("", "", "");
                    
//                } else if (cleanline.startsWith("_@NOTES")) {
//                    innotes = true;
//                } else if (innotes) {
//                    notes += StataUtils.smcl2plain(line, true);
//                    sb.append(line);
//                    sb.append("\n");
                } else {

                    hub.checkAndAddMetadata(tempItem, cleanline);
                    sb.append(line);
                    sb.append("\n");
                }
            }

            tempItem.setContent(StataUtils.smcl2html(sb.toString(), true));

            reader.close();
        } catch (Exception ex) {
            fileItem.addWarning("There was a problem with @statdocrun "
                    + ex.getMessage());
            ex.printStackTrace();
        } finally {
        
        Thread.currentThread().setName(
                "Thread " + Thread.currentThread().getId());
        }
    }
}
