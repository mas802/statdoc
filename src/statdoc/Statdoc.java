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
package statdoc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import statdoc.items.FileItem;
import statdoc.items.Item;
import statdoc.items.StatdocItemHub;
import statdoc.tasks.MatchingTask;
import statdoc.tasks.Task;
import statdoc.tasks.files.UpdateDirTask;
import statdoc.tasks.stata.StataAnalyseDtaFileTask;
import statdoc.tasks.stata.StataRunDoFileTask;
import statdoc.tasks.stata.StataUtils;
import statdoc.tasks.vm.GeneralVMTask;
import statdoc.tasks.vm.GenerateFileinfoTask;
import statdoc.tasks.vm.GenerateTokeninfoTask;
import statdoc.tasks.vm.GenerateVariableinfoTask;
import statdoc.utils.TemplateUtil;
import statdoc.utils.TopComparator;

/**
 * Main class of Statdoc and entry into the execution.
 * 
 * Should read the configuration and initiate the main tasks . Has control over
 * the dispensation of threads.
 * 
 * @author Markus Schaffner
 * 
 */
public class Statdoc {

    private final static String version = "v0.9.3-beta.4";

    // directory for all the files from initialroot
    private static final String[] dirs = new String[] { "overview", "files",
            "tokens", "variables", "templates", "derived" };
    private static final String[] files = new String[] { "stylesheet.css",
            "d3.min.js", "statdoc.js", "statdoc.properties" };
    private static final String[] templatefiles = new String[] { "index.vm",
            "overview-summary.vm", "overview-frame.vm", "help-doc.vm",
            "file-item.vm", "files-summary.vm", "files-frame.vm",
            "token-item.vm", "tokens-summary.vm", "tokens-frame.vm",
            "variable-item.vm", "variables-summary.vm", "variables-frame.vm",
            "item-header.vm", "item-footer.vm", "VM_global_library.vm",
            "compare.vm", "analyse-dta.do.vm", "dumpdata.vm" };

    // ThreadPoolExecutor taskQueue = new ArrayBlockingQueue<Task>(16000);
    ThreadPoolExecutor taskQueue;

    /**
     * This method will only complete after all jobs in the queue have completed
     * . TODO there seems to be an issue that the tasks are not finished
     * 
     * @param msg
     *            Message to display when running this set of tasks .
     * @param tasks
     *            Task to run and complete before this function returns .
     */
    public void workQueue(String msg, Task... tasks) {
        for (Task t : tasks) {
            taskQueue.execute(t);
        }

        System.out.println(msg + ": Threads active: "
                + taskQueue.getActiveCount() + " remaining: "
                + taskQueue.getQueue().size());
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        while (taskQueue.getActiveCount() > 0 && taskQueue.getTaskCount() > 0) {

            System.out.println(msg + ": Thread active: "
                    + taskQueue.getActiveCount() + " remaining: "
                    + taskQueue.getQueue().size());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Single entry into Statdoc from script and command line .
     * 
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

        long starttime = System.currentTimeMillis();

        // default values, current directory, output into statdoc
        // do not initialise
        Path sourceDir = (new File(".")).toPath().toAbsolutePath();
        Path outputDir = (new File("statdoc")).toPath().toAbsolutePath();
        Path statdocrunFile = null;
        Path singleDataFile = null;
        boolean initialise = false;
        boolean derivedClear = false;
        boolean clear = false;

        String versionCheck = "";

        boolean ok = true;
        for (int i = 0; i < args.length; i++) {

            if (args[i].equals("-o") || args[i].equals("--output")) {
                if (args.length > i + 1) {
                    i++;
                    outputDir = (new File(args[i])).toPath();
                } else {
                    System.err.println("Error with option --output ###" + args
                            + "###");
                    ok = false;
                }
            } else if (args[i].equals("-s") || args[i].equals("--source")) {
                if (args.length > i + 1) {
                    i++;
                    sourceDir = (new File(args[i])).toPath();
                } else {
                    System.err.println("Error with option --source ###" + args
                            + "###");
                    ok = false;
                }
            } else if (args[i].equals("-r") || args[i].equals("--statdocrun")) {
                if (args.length > i + 1) {
                    i++;
                    statdocrunFile = (new File(args[i])).toPath();
                } else {
                    System.err.println("Error with option --statdocrun ###"
                            + args + "###");
                    ok = false;
                }
            } else if (args[i].equals("-a") || args[i].equals("--analyse-data")) {
                if (args.length > i + 1) {
                    i++;
                    singleDataFile = (new File(args[i])).toPath();
                } else {
                    System.err.println("Error with option --analyse-data ###"
                            + args + "###");
                    ok = false;
                }
            } else if (args[i].equals("-vc")
                    || args[i].equals("--version-check")) {
                if (args.length > i + 1) {
                    i++;
                    versionCheck = args[i];
                } else {
                    System.err.println("Error with option --version-check ###"
                            + args + "###");
                    ok = false;
                }
            } else if (args[i].equals("-i") || args[i].equals("--initialise")) {
                initialise = true;
            } else if (args[i].equals("-d")
                    || args[i].equals("--derived-clear")) {
                derivedClear = true;
            } else if (args[i].equals("-c") || args[i].equals("--clear")) {
                clear = true;
            } else if (args[i].contains("=")) {
                // parse later when properties are set up
            } else if (!args[i].trim().equals("")) {
                // report an error if another argument is encountered
                // (unless it is empty)
                System.err.println("Error with option ###" + args[i] + "###");
                ok = false;
            }
        }

        // TODO, maybe this should not fail, but just produce an error
        if (!ok) {
            System.err.println("Error with cmd options:");
            System.err.println(Arrays.toString(args));
            System.err.println("");
            return;
        }

        if (versionCheck != "" && !versionCheck.equals(version)) {
            System.err.println("WARNING version check failed, caller expects "
                    + versionCheck + " but this is " + version);
            System.err.println("");
        }

        if (!sourceDir.toFile().exists() || !sourceDir.toFile().isDirectory()) {
            System.out.println("ERROR, source directory "
                    + sourceDir.toAbsolutePath()
                    + " does not exist or is not a directory.");
            return;
        }

        if (outputDir.toFile().exists() && !outputDir.toFile().isDirectory()) {
            System.out.println("ERROR, output directory "
                    + outputDir.toAbsolutePath()
                    + " exists and is not a directory.");
            return;
        }

        /*
         * Setup the hub that handles all items from now on.
         */
        if (statdocrunFile == null) {
            System.out
                    .println("Statdoc generates automagical documentation for ");
        } else {
            System.out.println("Statdocrun for the following do file ");
            System.out.println(statdocrunFile.toAbsolutePath());

            if (!statdocrunFile.toFile().exists()
                    || statdocrunFile.toFile().isDirectory()) {
                System.out
                        .println("ERROR, file does not exist or is directory "
                                + statdocrunFile.toAbsolutePath()
                                + " exists and is not a directory.");
                return;
            }
        }
        System.out.println("input: " + sourceDir.toAbsolutePath());
        System.out.println("output: " + outputDir.toAbsolutePath());
        System.out.println("Version " + version);
        System.out.println("Please be patient...");
        System.out.println(" ");
        System.out.println("STATDOC: Copyright 2014-2015, Markus Schaffner");
        System.out.println("Apache License, Version 2.0");
        System.out.println(" ");

        final StatdocItemHub hub = setupHub(sourceDir, outputDir, clear,
                derivedClear, initialise, args);

        if (statdocrunFile != null) {

            FileItem fi = hub.createFile(statdocrunFile, "file:script:do");
            StataRunDoFileTask srdfTask = new StataRunDoFileTask(fi, hub, null);
            srdfTask.run();

            System.out.println(fi.getChildrenBy("result:").iterator().next()
                    .getContent());
            System.out.println(" ");
            System.out.println("ran do file");
            System.out.println(" ");

        } else if (singleDataFile != null) {

            StataAnalyseDtaFileTask task = new StataAnalyseDtaFileTask(
                    singleDataFile, "file:data", hub, null);
            task.run();

            FileItem fi = (FileItem) hub.getFiles().getChildren().iterator()
                    .next();

            Map<String, Object> data = new TreeMap<String, Object>(
                    hub.getGlobals());
            data.put("item", fi);
            data.put("section", "files");

            if (fi.getType().startsWith("file:data")) {
                Collection<Item> grouped = StatdocItemHub.groupMap(
                        fi.getChildrenBy("variable:"), 3, 3, "group");
                data.put("groupedVars", grouped);
            }

            File f = new File(hub.outputDir.toFile(), fi.getLink() + "");
            GeneralVMTask vmtask = new GeneralVMTask("file-item.vm", f, data);
            vmtask.run();

            System.out.println(" ");
            System.out
                    .println("Done, copy the following URL into your browser:");
            System.out.println("file://" + f.getAbsolutePath());
            System.out.println(" ");

        } else {
            // initialise the main class.
            Statdoc me = new Statdoc();

            // setup the taskQueue
            me.taskQueue = new ThreadPoolExecutor(4, 1000, 60,
                    TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(20000));

            // start of the first round of just reading in all files
            // and processing all files
            me.workQueue("Stage 1 (reading files and data)", new UpdateDirTask(
                    sourceDir, hub.getProp(), hub, me.taskQueue));

            /*
             * // work through second stage file producing
             * me.workQueue("Stage 2a (parsing script files)", new
             * InitiateScriptFilesParsingTask( me.taskQueue));
             */

            // resolve all matchings
            me.workQueue("Stage 2b (resolve matching)", new MatchingTask(hub,
                    me.taskQueue));

            // set up map for overview files
            Item overview = new Item("overview", "overview",
                    "overview/overview-summary.html", "overview:summary");

            TreeSet<Item> topimg = new TreeSet<Item>(TopComparator.INSTANCE);
            topimg.addAll(hub.getFiles().getChildrenBy("file:image"));

            TreeSet<Item> topvar = new TreeSet<Item>(TopComparator.INSTANCE);
            topvar.addAll(hub.getVariables().values());

            Map<String, Object> data = new TreeMap<String, Object>(
                    hub.getGlobals());
            data.put("cmds", hub.getCmds());
            data.put("topvar", topvar);
            data.put("topimg", topimg);
            data.put("section", "overview");
            data.put("item", overview);

            // build general index file
            me.taskQueue.execute(new GeneralVMTask(new File(hub.outputDir
                    .toFile(), "index.html"), "index.vm", data));

            // build the comapre file
            me.taskQueue.execute(new GeneralVMTask(new File(hub.outputDir
                    .toFile(), "compare.html"), "compare.vm", data));

            // build overview files
            me.taskQueue.execute(new GeneralVMTask(new File(hub.outputDir
                    .toFile(), "overview/overview-frame.html"),
                    "overview-frame.vm", data));
            me.taskQueue.execute(new GeneralVMTask(new File(hub.outputDir
                    .toFile(), "overview/overview-summary.html"),
                    "overview-summary.vm", data));

            Item help = new Item("help", "help", "overview/help-doc.html",
                    "help");
            Map<String, Object> datah = new TreeMap<String, Object>(
                    hub.getGlobals());
            datah.put("section", "help");
            datah.put("item", help);
            me.taskQueue
                    .execute(new GeneralVMTask(new File(hub.outputDir.toFile(),
                            "overview/help-doc.html"), "help-doc.vm", datah));

            // build the rest
            me.taskQueue.execute(new GenerateFileinfoTask(hub.outputDir
                    .toFile(), hub, me.taskQueue));
            me.taskQueue.execute(new GenerateTokeninfoTask(hub.outputDir
                    .toFile(), hub, me.taskQueue));
            me.taskQueue
                    .execute(new GenerateVariableinfoTask(hub, me.taskQueue));

            // work through second stage file producing
            me.workQueue("Stage 3 (templates)");

            System.out.println("Process complete in: "
                    + ((System.currentTimeMillis() - starttime) / 1000)
                    + " seconds.");

            me.taskQueue.shutdown();

            System.out.println(" ");
            System.out.println(hub.stats());
            System.out.println(" ");
            System.out
                    .println("All done, copy the following URL into your browser:");
            System.out.println("file://" + hub.outputDir.toAbsolutePath()
                    + "/index.html");
            System.out.println(" ");
        }
    }

    private static StatdocItemHub setupHub(Path sourceDir, Path outputDir,
            boolean clear, boolean derivedClear, boolean initialise,
            String[] args) throws IOException {
        StatdocItemHub hub = new StatdocItemHub();

        hub.sourceDir = sourceDir;
        hub.outputDir = outputDir;

        SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        SimpleDateFormat dts = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();

        hub.setGlobal("date", dt.format(date));
        hub.setGlobal("date_short", dts.format(date));
        hub.setGlobal("version", version);

        // copy initial root and make file structure if needed
        hub.outputDir.toFile().mkdirs();
        for (String file : dirs) {
            File dir = new File(hub.outputDir.toFile(), file);
            dir.mkdir();
            if (clear) {
                for (File files : dir.listFiles())
                    files.delete();
            }
        }

        if (derivedClear) {
            File d = new File(hub.outputDir.toFile(), "derived");
            if (d.exists()) {
                for (File file : d.listFiles()) {
                    file.delete();
                }
            } else {
                System.err
                        .println("Cannot empty non-existing derived directory: "
                                + d.toString());
            }
        }

        for (String file : files) {
            InputStream f = Statdoc.class.getResourceAsStream("/initialroot/"
                    + file);
            File target = new File(hub.outputDir.toFile(), file);
            if (!target.isDirectory() && (initialise || !target.exists())) {
                Files.copy(f, target.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        }

        for (String file : templatefiles) {
            InputStream f = Statdoc.class
                    .getResourceAsStream("/initialroot/templates/" + file);
            File target = new File(hub.outputDir.toFile(), "templates/" + file);
            if (initialise || !target.exists()) {
                Files.copy(f, target.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        }

        // initialise template util
        File templateDir = new File(hub.outputDir.toFile(), "templates");
        new TemplateUtil(templateDir.getAbsolutePath());

        Properties generalProp = new Properties();
        generalProp.load(new FileInputStream(new File(outputDir.toFile(),
                "statdoc.properties")));

        // add commandline properties
        for (String a : args) {
            if (a.contains("=")) {
                String[] sp = a.split("=");
                generalProp.put(sp[0], sp[1]);
            }
        }

        hub.setGlobal(
                "project",
                generalProp.getProperty("statdoc.project", sourceDir.toFile()
                        .getCanonicalFile().getName()));

        hub.setProp(generalProp);

        /*
         * Set the stata command
         */
        String[] stataProgs = generalProp.getProperty("statdoc.stata.path",
                "stata").split("[\\s]*,[\\s]*");
        File stataPath = StataUtils.resolveStataPath(stataProgs,
                System.getProperty("os.name", "generic"));
        if (!stataPath.canExecute() || stataPath.isDirectory()) {
            System.err.println(" ");
            System.err.println("No installations of Stata found, please edit");
            System.err.println("the statadoc.properties file and add the path");
            System.err.println("of a Stata 13/14 executable on this system to");
            System.err.println("the statdoc.stata.path property.");
            System.err.println(" ");
        } else {
            hub.setStataPath(stataPath.getAbsolutePath());
        }

        /*
         * set commands for stata
         */
        Map<String, String[]> stataCmd = new HashMap<String, String[]>();
        for (Object o : generalProp.keySet()) {
            String key = o.toString();
            if (key.startsWith("statdoc.cmd.stata.")) {
                stataCmd.put(key.substring(18), generalProp.getProperty(key)
                        .split("[\\s]*,[\\s]*"));
            }
        }
        hub.setStataCmdTypes(stataCmd);
        return hub;
    }

}
