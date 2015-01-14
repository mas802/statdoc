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
import java.io.FileFilter;
import java.lang.reflect.InvocationTargetException;
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
 * Main entry point into the directory tree parser, sets of additional tasks
 * for each file found.
 * 
 * TODO maybe resolve the File Task assignment in a config file.
 * 
 * @author Markus Schaffner
 *
 */
public class UpdateDirTask implements Task {

    private File rootDir = null;
    private ThreadPoolExecutor taskQueue;
    private StatdocItemHub hub;
    private List<Pattern> exclude = new ArrayList<Pattern>();

    private class TaskEntry {
        String type;
        Class<Task> taskClass;
    }
    
    Map<String, TaskEntry> taskMap = new HashMap<String, UpdateDirTask.TaskEntry>();
    
    @SuppressWarnings("unchecked")
    public UpdateDirTask(File dir, Properties properties, 
            StatdocItemHub hub, ThreadPoolExecutor taskQueue) {
        this.rootDir = dir;
        this.hub = hub;

        /*
         * set excludes
         */
        String[] exclude = properties.getProperty("statdoc.files.exclude", "")
                .split("[\\s]*,[\\s]*");
        
        for ( String s : exclude ) {
            Pattern p = Pattern.compile(".*"+s+".*");
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
                    String name = param.substring(param.lastIndexOf('.')+1);
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
	Thread.currentThread().setName("Run " + this.getClass() );
	
        List<File> files = new ArrayList<File>();

        getAllFiles(rootDir, files);

        for (File f : files) {
            // Figure out what to run
            String filename = f.getAbsolutePath().toLowerCase();

            // System.out.println(filename);
            
            int i = filename.lastIndexOf(".");
            String suffix = filename.substring(i+1);
            
            // System.out.println(suffix);
            
            if ( taskMap.containsKey(suffix) ) {
                TaskEntry te = taskMap.get(suffix);
                    Task task;
                    try {
                        task = te.taskClass.getConstructor(File.class, File.class, String.class, hub.getClass(), taskQueue.getClass()).newInstance(rootDir,f,te.type,hub,taskQueue);
                        taskQueue.execute(task);
                    } catch (InstantiationException | IllegalAccessException
                            | IllegalArgumentException
                            | InvocationTargetException | NoSuchMethodException
                            | SecurityException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
            } else {
                taskQueue.execute(new OtherFileTask( rootDir, f, "file:general", hub, taskQueue));
            }
        }
    	Thread.currentThread().setName("Thread " + Thread.currentThread().getId() );

    }

    public void getAllFiles(File sDir, List<File> result) {
        File[] faFiles = sDir.listFiles(new FileFilter() {

            @Override
            public boolean accept(File name) {
                boolean accept = true;
                
                for ( Pattern p:exclude ) {
                    accept = accept && !p.matcher(name.getAbsolutePath()).matches();
                }
                return accept;
            }
        });
        // System.out.println(sDir);
        for (File file : faFiles) {
            if (file.isDirectory() ) {
                getAllFiles(file, result);
            } else {
                result.add(file);
            }
        }
    }
}