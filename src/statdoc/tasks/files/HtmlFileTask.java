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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ThreadPoolExecutor;

import statdoc.items.FileItem;
import statdoc.items.StatdocItemHub;
import statdoc.tasks.Task;

/**
 * A task to read html files, it is very similar to TextFileTask
 * except that it replaces all < and > with there html entity counterpart
 * 
 * @author Markus Schaffner
 *
 */
public class HtmlFileTask implements Task {

    File file;
    File rootDir;
    ThreadPoolExecutor taskList;
    String type;

    public HtmlFileTask(File rootDir, File file, String type,
            ThreadPoolExecutor taskList) {
        this.file = file;
        this.rootDir = rootDir;
        this.taskList = taskList;
        this.type = type;
    }

    @Override
    public void run() {
        Thread.currentThread().setName(
                " Text File Task for File: " + file + " - "
                        + Thread.currentThread().getName());

        StatdocItemHub itemHolder = StatdocItemHub.getInstance();

        FileItem fi = itemHolder.createFile(file, rootDir, type);

        try {
            String content = new String(Files.readAllBytes(Paths.get(file
                    .toURI())));
            
            // TODO think if this is good or bad
            content = content.replaceAll("<", "&lt;");
            content = content.replaceAll(">", "&gt;");
            
            fi.setContent(content);
        } catch (Exception e) {
            System.err.println("Error for: " + file + " - "
                    + Thread.currentThread().getName());
            e.printStackTrace();

            fi.addWarning("There was and error processing this file: " + e.getMessage() );
        }

        Thread.currentThread().setName(
                "Thread " + Thread.currentThread().getId());
    }
}
