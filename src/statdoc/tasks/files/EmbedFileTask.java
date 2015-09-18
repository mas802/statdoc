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
package statdoc.tasks.files;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ThreadPoolExecutor;

import statdoc.items.FileItem;
import statdoc.items.StatdocItemHub;
import statdoc.tasks.Task;

/**
 * Process a Image file, currently provides a link to the orginal file as
 * content.
 * 
 * @author Markus Schaffner
 * 
 */
public class EmbedFileTask implements Task {

    private Path file;
    ThreadPoolExecutor taskList;
    private StatdocItemHub hub;
    private String type;

    public EmbedFileTask(Path file, String type, StatdocItemHub hub,
            ThreadPoolExecutor taskList) {
        this.file = file;
        this.taskList = taskList;
        this.hub = hub;
        this.type = type;
    }

    @Override
    public void run() {
        Thread.currentThread().setName("Image File Task for File: " + file);

        FileItem fi = hub.createFile(file, type);

        // TODO make this configurable
        boolean copy = true;
        if (copy) {
            long id = file.toFile().lastModified();

            Path target = new File(hub.outputDir.toFile(), "derived/"
                    + file.getFileName()).toPath();
            try {
                long targettimestamp = target.toFile().lastModified();

                if (targettimestamp < id) {
                    Files.copy(file, target,
                            StandardCopyOption.REPLACE_EXISTING);
                }
                
                String ref = "../" + hub.outputDir.relativize(target).toString();
                
                fi.setContent("<object src='" + ref + "' width='100%' height='500px'> <embed src='" + ref + "' width='100%' height='500px'></embed></object>");
            } catch (IOException e) {
                e.printStackTrace();
                fi.setContent("<img src=\"../" + fi.getFileLink()
                        + "\" width=\"90%\">");
                fi.addWarning("error while copying file: " + e.getMessage());
            }
        } else {
            fi.setContent("<img src=\"../" + fi.getFileLink()
                    + "\" width=\"90%\">");
        }
        Thread.currentThread().setName(
                "Thread " + Thread.currentThread().getId());
    }
}
