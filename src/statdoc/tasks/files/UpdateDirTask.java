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
import java.lang.reflect.InvocationTargetException;
import java.nio.file.FileVisitResult;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.regex.Pattern;

import statdoc.items.StatdocItemHub;
import statdoc.tasks.Task;

/**
 * Main entry point into the directory tree parser, sets of additional tasks for
 * each file found.
 * 
 * TODO maybe resolve the File Task assignment in a config file.
 * 
 * @author Markus Schaffner
 * 
 */
public class UpdateDirTask implements Task {

    private Path rootDir = null;
    private ThreadPoolExecutor taskQueue;
    private StatdocItemHub hub;
    private List<Pattern> exclude = new ArrayList<Pattern>();

    private class TaskEntry {
        String type;
        Class<Task> taskClass;
    }

    Map<String, TaskEntry> taskMap = new HashMap<String, UpdateDirTask.TaskEntry>();

    @SuppressWarnings("unchecked")
    public UpdateDirTask(Path dir, Properties properties, StatdocItemHub hub,
            ThreadPoolExecutor taskQueue) {
        this.rootDir = dir;
        this.hub = hub;

        /*
         * set excludes
         */
        String[] exclude = properties.getProperty("statdoc.files.exclude", "")
                .split("[\\s]*,[\\s]*");

        for (String s : exclude) {
            Pattern p = Pattern.compile(".*" + s + ".*");
            this.exclude.add(p);
        }

        // setup services from properties file
        ClassLoader cl;
        cl = UpdateDirTask.class.getClassLoader();
        try {
            for (Map.Entry<Object, Object> p : properties.entrySet()) {
                String key = p.getKey().toString();
                if (key.startsWith("statdoc.file.")) {

                    String param = key.substring(8);
                    String name = param.substring(param.lastIndexOf('.') + 1);
                    String classstr = p.getValue().toString();

                    TaskEntry te = new TaskEntry();
                    te.type = param.replaceAll("\\.", ":");
                    te.taskClass = (Class<Task>) cl.loadClass(classstr);
                    taskMap.put(name, te);
                }
            }
        } catch (ClassNotFoundException e1) {
            e1.printStackTrace();
        }
        this.taskQueue = taskQueue;
    }

    @Override
    public void run() {
        Thread.currentThread().setName("Run " + this.getClass());

      
        try {
            Files.walkFileTree(rootDir, new SimpleFileVisitor<Path>() {
                
                @Override
                public FileVisitResult visitFile(Path file,
                        BasicFileAttributes attrs) throws IOException {
                    String filename = file.toAbsolutePath().toString()
                            .toLowerCase();

                    boolean accept = true;

                    for (Pattern p : exclude) {
                        accept = accept && !p.matcher(filename).matches();
                    }

                    if (accept) {

                        int i = filename.lastIndexOf(".");
                        String suffix = filename.substring(i + 1);

                        if (taskMap.containsKey(suffix)) {
                            TaskEntry te = taskMap.get(suffix);
                            Task task;
                            try {
                                task = te.taskClass.getConstructor(File.class,
                                        File.class, String.class,
                                        hub.getClass(), taskQueue.getClass())
                                        .newInstance(rootDir.toFile(),
                                                file.toFile(), te.type, hub,
                                                taskQueue);
                                taskQueue.execute(task);
                            } catch (InstantiationException
                                    | IllegalAccessException
                                    | IllegalArgumentException
                                    | InvocationTargetException
                                    | NoSuchMethodException | SecurityException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        } else {
                            taskQueue.execute(new OtherFileTask(rootDir
                                    .toFile(), file.toFile(), "file:general",
                                    hub, taskQueue));
                        }
                    } 
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public FileVisitResult preVisitDirectory(Path dir,
                        BasicFileAttributes attrs) throws IOException {

                    String filename = dir.toAbsolutePath().toString()
                            .toLowerCase();

                    boolean accept = true;

                    for (Pattern p : exclude) {
                        accept = accept && !p.matcher(filename).matches();
                    }
                    
                    if ( accept ) {
                    return super.preVisitDirectory(dir, attrs);
                    } else {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                }
            });
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        Thread.currentThread().setName(
                "Thread " + Thread.currentThread().getId());

    }
}