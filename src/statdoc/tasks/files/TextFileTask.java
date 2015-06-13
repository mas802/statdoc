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

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.concurrent.ThreadPoolExecutor;

import statdoc.items.FileItem;
import statdoc.items.StatdocItemHub;
import statdoc.tasks.Task;

/**
 * Read in a basic text File.
 * 
 * TODO could cap the file if too large (e.g. 1000 lines)
 * 
 * @author Markus Schaffner
 * 
 */
public class TextFileTask implements Task {

    private Path file;
    ThreadPoolExecutor taskList;
    private StatdocItemHub hub;
    private String type;

    // TODO, make this a config property
    private int maxlines = 1000;

    public TextFileTask(Path file, String type,
            StatdocItemHub hub, ThreadPoolExecutor taskList) {
        this.file = file;
        this.taskList = taskList;
        this.type = type;
        this.hub = hub;
    }

    @Override
    public void run() {
        Thread.currentThread().setName(
                " Text File Task for File: " + file + " - "
                        + Thread.currentThread().getName());

        FileItem fi = hub.createFile(file, type);

        try {
            BufferedReader reader = new BufferedReader(new FileReader(file.toFile()));

            StringBuilder sb = new StringBuilder();

            int counter = 0;

            String line;
            while ((line = reader.readLine()) != null && counter < maxlines) {
                // hub.checkAndAddMetadata(fi, line);
                sb.append(line);
                sb.append("\n");
                counter++;
            }

            reader.close();

            fi.setContent(sb.toString());

            if (counter == maxlines) {
                fi.addWarning("The reading of this file has been clipped at 1000 lines.");
            }

        } catch (Exception e) {
            System.err.println("Error for: " + file + " - "
                    + Thread.currentThread().getName());
            e.printStackTrace();

            fi.addWarning("There was and error processing this file: "
                    + e.getMessage());
        }

        Thread.currentThread().setName(
                "Thread " + Thread.currentThread().getId());
    }
}
