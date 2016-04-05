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
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

import statdoc.items.FileItem;
import statdoc.items.Item;
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

    public StataRunDoFileTask(FileItem fileItem, StatdocItemHub hub,
            ThreadPoolExecutor taskList) {
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

            File output = new File(hub.outputDir.toFile(),
                    "derived/analyse_run_" + file.getFileName() + "_" + id
                            + ".smcl");

            // stop execution if no Stata exec
            if (hub.getStataPath() == null) {
                fileItem.addWarning("Please provide a valid Stata "
                        + "executable in statdoc.properties.");
                return;
            }

            if (!output.exists()) {

                String str = file.getFileName().toString();
                StataUtils.runDoFile(str.substring(0, str.lastIndexOf('.')),
                        file.toAbsolutePath().toString(), hub.getStataPath(),
                        output.toPath());
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(output), "x-MacRoman"));

            StringBuilder sb = new StringBuilder();

            Item tempItem = new Item("result_" + fileItem.getName(), "result_"
                    + fileItem.getName(), "result:");

            fileItem.addChild("result:", tempItem);

            boolean incomment = false;
            StringBuilder currentcomment = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
                // System.out.println(line);
                String cleanline = StataUtils.smcl2plain(line, false).trim();
                if (cleanline.startsWith(">>>>>")) {

                    // produce a new FileItem from info
                    // currentItem = hub.createVariable(var, "variable:" + t,
                    // dtaFileItem);
                    Map<String, Object> map = new HashMap<String, Object>();
                    map.put("data", tempItem);

                    String[] param = cleanline.substring(5).split("->");

                    String templatefilename = param[0].trim();

                    // read template from command comment if no file name is
                    // given
                    if (templatefilename.equals("")) {
                        templatefilename = Math.abs(this.hashCode()) + ".vm";
                        File tf = new File(TemplateUtil.getInstance().getDir()
                                , templatefilename);
                        
                        System.out.println( tf.getAbsolutePath());
                        // TODO tf.deleteOnExit();

                        FileWriter fw = new FileWriter(tf);
                        fw.write(currentcomment.toString());
                        fw.close();
                    }

                    TemplateUtil.getInstance().evalVMtoFile(
                            new File(hub.sourceDir.toFile(), param[1].trim()),
                            templatefilename, map);

                    // reset temp vars
                    // sb = new StringBuilder();
                    // FIXME here we could at copy tempItem = new Item("", "",
                    // "");

                    // } else if (cleanline.startsWith("_@NOTES")) {
                    // innotes = true;
                    // } else if (innotes) {
                    // notes += StataUtils.smcl2plain(line, true);
                    // sb.append(line);
                    // sb.append("\n");
                } else {

                    hub.checkAndAddMetadata(tempItem, cleanline);

                    // handle comment storage
                    if ( incomment ) {
                        String add = cleanline;
                        if (cleanline.contains("*/")) {
                            add = cleanline.split("\\*\\/")[0];
                            incomment = false;
                        }
                        add = add.replaceAll("^[>]*[ ]?", "");
                        currentcomment.append(add+"\n");
                    } else if (cleanline.contains("/*")) {
                        int ind = 
                                cleanline.indexOf("/*") + 2;
                        currentcomment = new StringBuilder(cleanline.substring(ind)+"\n");
                        incomment = true;
                    }

                }
                sb.append(line);
                sb.append("\n");
            }

            tempItem.setContent(StataUtils.smcl2plain(sb.toString(), true));

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
