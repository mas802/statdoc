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

import java.nio.file.Path;
import java.util.concurrent.ThreadPoolExecutor;

import statdoc.items.FileItem;
import statdoc.items.StatdocItemHub;
import statdoc.tasks.Task;

/**
 * Catch all class for files not otherwise processed.
 * Adds a warning to the file to this end.
 * 
 * TODO the warning could be optional, some files are just not meant to be processed
 * 
 * @author Markus Schaffner
 *
 */
public class OtherFileTask implements Task {

    private Path file;
    ThreadPoolExecutor taskList;
    private StatdocItemHub hub;
    private String type;

    public OtherFileTask(Path file, String type,
            StatdocItemHub hub, ThreadPoolExecutor taskList) {
        this.file = file;
        this.taskList = taskList;
        this.hub = hub;
        this.type = type;
    }

    @Override
    public void run() {
        Thread.currentThread().setName("Other File Task for File: " + file);

        FileItem fi = hub.createFile(file, type);

        if ( type.equals("file:general") ) {
            // set tokens, probably not much to do here
            fi.addWarning("This file type is not recognised, "
                    + "you can add an appropriate statdoc.file... entry to "
                    + "statdoc.properties to change this.");
        }
        
        Thread.currentThread().setName(
                "Thread " + Thread.currentThread().getId());
    }
}
