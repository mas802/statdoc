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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ThreadPoolExecutor;

import statdoc.items.CmdItem;
import statdoc.items.MatchItem;
import statdoc.items.FileItem;
import statdoc.items.StatdocItemHub;
import statdoc.tasks.Task;

/**
 * Task to parse a do file.
 * 
 * This task should go through the do file and make note of the overall
 * structure of the document. It recognizes comment and doc blocks and multiline
 * commands and classifies commands based on the Stata syntax.
 * 
 * @author Markus Schaffner
 * 
 */
public class StataDoFileTask implements Task {

    enum ReadMode {
        Commands, DocBlock, CommentBlock, MataBlock
    }

    private Path file;
    ThreadPoolExecutor taskList;
    private StatdocItemHub hub;
    private String type;

    private MatchItem currentIn;
    private MatchItem currentOut;

    public StataDoFileTask(Path file, String type, StatdocItemHub hub,
            ThreadPoolExecutor taskList) {
        this.file = file;
        this.taskList = taskList;
        this.type = type;
        this.hub = hub;
    }

    @Override
    public void run() {

        Thread.currentThread().setName(
                " Stata parse do file task for file: " + file);

        FileItem fileItem = hub.createFile(file, type);

        try {
            String content = new String(Files.readAllBytes(file));

            fileItem.setContent(content);
        } catch (Exception e) {
            System.err.println("Error for: " + file + " - "
                    + Thread.currentThread().getName());
            e.printStackTrace();

            fileItem.addWarning("There was and error processing this file: "
                    + e.getMessage());
        }

        currentIn = hub.createMatch("initIn", "file:data");
        currentOut = hub.createMatch("initOut", "file:data");

        ReadMode mode = ReadMode.Commands;

        boolean firstDoc = false;

        /* is the document delimited by ; */
        boolean delimsemi = false;

        // indent counter
        int indent = 0;

        String currentComment = null;
        String currentDoc = null;
        String currentCmd = null;
        String currentMata = null;

        /* are we in a mata block wit curly brackets */
        boolean matacurl = false;

        Integer[] docRange = new Integer[] { 0, 0 };
        Integer[] cmdRange = new Integer[] { 0, 0 };
        Integer[] mataRange = new Integer[] { 0, 0 };

        // loop through the file
        int lineNumber = 0;

        // split into lines and parse
        String lines[] = fileItem.getContent().split("[\r]?\n");
        for (String rawLine : lines) {
            lineNumber++;

            String trimLine = rawLine.trim();

            // change mode based on prefix
            if (mode.equals(ReadMode.MataBlock)) {
                // do not change while in mata mode until an end of block is
                // encountered
                // inside the mata block

            } else if (trimLine.equals("mata") || trimLine.equals("mata:")
                    || trimLine.startsWith("mata ")) {
                // start mata block
                mode = ReadMode.MataBlock;

                if (StataUtils.countChar(trimLine, "{") == 1) {
                    matacurl = true;
                } else {
                    matacurl = false;
                }
                currentMata = "";
                mataRange[0] = lineNumber;

            } else if (trimLine.equals("/**") || trimLine.startsWith("/** ")) {
                // start doc
                mode = ReadMode.DocBlock;

                // TODO check for exiting comment block
                currentDoc = "";
                docRange[0] = lineNumber;

            } else if (trimLine.startsWith("/*")) {
                // start comment
                mode = ReadMode.CommentBlock;

                // TODO check for exiting comment block
                currentComment = "";
                docRange[0] = lineNumber;

            } else if (trimLine.startsWith("#delimit ;")) {
                delimsemi = true;
            } else if (trimLine.startsWith("#delimit cr")) {
                delimsemi = false;
            }

            switch (mode) {
            case Commands:
                // System.out.println(lineNumber + ":C:" + l);

                if (currentCmd != null
                        && StataUtils.balanceChars(currentCmd, "/*", "*/") > 0) {
                    // we have a cmd with an open comment
                    trimLine = (currentCmd + "\n" + rawLine).trim();
                }

                if (trimLine.equals("")) {
                    // this is an empty line, so no item

                    if (currentComment != null) {
                        // if there is a comment, the comment has to be
                        // created as a stand-alone item

                        CmdItem cmdItem = hub
                                .createCmd(currentComment, "comment",
                                        "cmd:comment", fileItem, docRange,
                                        null, null, null,
                                        new HashMap<String, Object>());

                        cmdItem.put("comment", currentComment);

                        currentComment = null;
                    }

                    if (currentDoc != null) {
                        fileItem.addWarning("<b>Unassigned documentation comment lines "
                                + docRange[0]
                                + " to "
                                + docRange[1]
                                + " (document comment followed by an empty line):</b><br><pre>"
                                + currentDoc + "</pre>");

                        currentDoc = null;
                    }

                } else if (StataUtils.balanceChars(trimLine, "/*", "*/") > 0) {

                    // cmd is continued on the next line
                    if (currentCmd != null) {
                        currentCmd = currentCmd + "\n" + rawLine;
                    } else {
                        currentCmd = rawLine;
                        cmdRange[0] = lineNumber;
                    }

                } else if (trimLine.startsWith("//")
                        || trimLine.startsWith("*")) {
                    // a standalone comment
                    if (currentComment == null) {
                        currentComment = rawLine;
                        docRange[0] = lineNumber;
                        docRange[1] = lineNumber;
                    } else {
                        currentComment = currentComment + "\n" + rawLine;
                        docRange[1] = lineNumber;
                    }

                } else if (trimLine.endsWith("///")) {
                    // cmd is continued on the next line
                    if (currentCmd != null) {
                        currentCmd = currentCmd + "\n" + rawLine;
                    } else {
                        currentCmd = rawLine;
                        cmdRange[0] = lineNumber;
                    }

                } else if (delimsemi && !trimLine.contains(";")) {
                    // cmd is continued on the next line in delim mode
                    if (currentCmd != null) {
                        currentCmd = currentCmd + "\n" + rawLine;
                    } else {
                        currentCmd = rawLine;
                        cmdRange[0] = lineNumber;
                    }
                } else {
                    if (currentCmd != null) {
                        currentCmd = currentCmd + "\n" + rawLine;
                    } else {
                        currentCmd = rawLine;
                        cmdRange[0] = lineNumber;
                    }
                    cmdRange[1] = lineNumber;

                    if (delimsemi) {
                        currentCmd = currentCmd.replaceAll(";", "").trim();
                    }

                    // deal with indent reduction
                    if (StataUtils.balanceChars(trimLine, "{", "}") == -1) {
                        indent--;
                    }

                    // deal with indent for blocks
                    if (trimLine.startsWith("end ") || trimLine.equals("end")) {
                        indent--;
                    }

                    CmdItem cmdItem = parseLine(currentCmd, fileItem, cmdRange,
                            currentDoc, currentComment, docRange, indent);

                    // check index bounds
                    if (indent < 0) {
                        String warn = "Indent out of bounds on line "
                                + cmdRange[1] + " check the syntax.";
                        fileItem.addWarning(warn);
                        cmdItem.addWarning(warn);
                        indent = 0;
                    }

                    if (cmdItem.get("command").toString()
                            .matches("prog(r(a(m)?)?)?")) {
                        String p = cmdItem.get("parameters").toString().trim();
                        if (!p.startsWith("drop") && !p.startsWith("dir")
                                && !p.startsWith("list")
                                && !p.startsWith("plugin")) {
                            indent++;
                        }
                    }

                    // deal with indent increase
                    if (StataUtils.balanceChars(trimLine, "{", "}") == 1) {
                        indent++;
                    }

                    currentCmd = null;
                    currentDoc = null;
                    currentComment = null;
                }

                break;
            case DocBlock:
                // System.out.println(lineNumber + ":D:" + l);

                currentDoc = currentDoc + "\n" + rawLine;

                if (StataUtils.balanceChars(currentDoc, "/*", "*/") == 0) {

                    currentDoc = currentDoc.trim();
                    currentDoc = currentDoc.replaceAll("^[/]?[*]+", "");
                    currentDoc = currentDoc.replaceAll("[*][/]", "");
                    currentDoc = currentDoc.replaceAll("\n[\\s]*[*]", "\n");
                    mode = ReadMode.Commands;
                    docRange[1] = lineNumber;

                    // deal with it if it is the first doc
                    // TODO make the 10 a config issue
                    if (!firstDoc && docRange[0] < 10) {
                        hub.addDocToItem(fileItem, currentDoc, docRange);
                        firstDoc = true;
                        currentDoc = null;
                    }
                }
                break;
            case CommentBlock:
                // System.out.println(lineNumber + "://:" + l);

                currentComment = currentComment + "\n" + rawLine;

                if (StataUtils.balanceChars(currentComment, "/*", "*/") == 0) {
                    docRange[1] = lineNumber;
                    mode = ReadMode.Commands;
                }
                break;
            case MataBlock:
                // System.out.println(lineNumber + ":mata:" + l);

                currentMata = currentMata + "\n" + rawLine;

                if ((!matacurl && (trimLine.equals("end") || trimLine
                        .startsWith("end ")))
                        || (matacurl && StataUtils.balanceChars(currentMata,
                                "{", "}") == 0)) {
                    mataRange[1] = lineNumber;
                    mode = ReadMode.Commands;

                    CmdItem cmdItem = hub.createCmd(currentMata, "mata",
                            "cmd:mata", fileItem, mataRange, currentDoc,
                            currentComment, docRange,
                            new HashMap<String, Object>());

                    int mataLines = StataUtils.countChar(currentMata, "\n");

                    cmdItem.put("command", "mata");
                    cmdItem.put("comment", "block (" + mataLines + " lines)");

                    currentDoc = null;
                    currentComment = null;
                    currentMata = null;
                    matacurl = false;
                    mode = ReadMode.Commands;

                }
                break;
            default:
                fileItem.addWarning("there was a parsing mode error at line: "
                        + lineNumber);
                break;
            }

        }

        if (fileItem.containsKey("statdocrun")) {
            taskList.execute(new StataRunDoFileTask(fileItem, hub, taskList));
        }

        Thread.currentThread().setName(
                "Thread " + Thread.currentThread().getId());
    }

    /**
     * Main method to disect a stata command
     * 
     * @param currentCmd
     * @param fileItem
     * @param cmdRange
     * @param currentDoc
     * @param currentComment
     * @param docRange
     * @return the generated item for the line
     */
    private CmdItem parseLine(String currentCmd, FileItem fileItem,
            Integer[] cmdRange, String currentDoc, String currentComment,
            Integer[] docRange, int indent) {

        Map<String, Object> info = new TreeMap<String, Object>();

        info.put("indent", indent);

        currentCmd = currentCmd.replaceAll("/\\*(.*?)\\*/", "");
        // [prefix :] command [varlist] [=exp] [if] [in] [weight]
        // [using filename] [, options]

        // Disect the command
        String commandAndParameters;

        String[] parts = StataUtils.splitSave(currentCmd.trim(), "//");
        if (parts.length > 1) {
            info.put("comment", "//" + parts[1]);
        }
        parts = StataUtils.splitSave(parts[0].trim(), ",");
        if (parts.length > 1) {
            info.put("options", parts[1]);
        }
        parts = StataUtils.splitSave(parts[0], " using ");
        if (parts.length > 1) {
            info.put("using", parts[1]);
        }
        parts = StataUtils.splitSave(parts[0], " weight ");
        if (parts.length > 1) {
            info.put("weight", parts[1]);
        }
        parts = StataUtils.splitSave(parts[0], " in ");
        if (parts.length > 1) {
            info.put("in", parts[1]);
        }
        parts = StataUtils.splitSave(parts[0], " if ");
        if (parts.length > 1) {
            info.put("if", parts[1]);
        }

        commandAndParameters = parts[0];

        parts = commandAndParameters.split("\\s");
        String command = parts[0].trim();
        String parameters = commandAndParameters.substring(command.length())
                .trim();

        info.put("command", command);
        info.put("parameters", parameters);

        // determine type
        String type = commandType(command, hub.getStataCmdTypes());

        //
        // deal with special commands
        //
        while (type.equals("cmd:prefixcmd")) {
            String prefix = (info.containsKey("prefix")) ? info.get("prefix")
                    + " " : "";
            String afterPrefix = command + " " + parameters;
            String[] prefixparts = StataUtils.splitSave(afterPrefix, ":");
            if (prefixparts.length > 1) {
                prefix += prefixparts[0] + ":";
                afterPrefix = prefixparts[1].trim();
            } else {
                prefix += command;
                afterPrefix = parameters;
            }

            parts = afterPrefix.trim().split("\\s");
            command = parts[0].trim();
            parameters = afterPrefix.trim().substring(command.length());

            info.put("prefix", prefix);
            info.put("command", command);
            info.put("parameters", parameters);

            type = commandType(command, hub.getStataCmdTypes());
        }

        // double cmds
        if (type.equals("cmd:doublecmd")) {
            parts = parameters.trim().split("\\s");
            command = command + " " + parts[0];
            parameters = parameters.substring(parts[0].length()).trim();

            info.put("command", command);
            info.put("parameters", parameters);

            type = commandType(command.replaceAll(" ", "_"),
                    hub.getStataCmdTypes());
        }

        if (command.equals("log")) {
            if (info.containsKey("using")) {
                type = "cmd:outputcmd:log";
            }
        }

        if (currentDoc != null) {
            currentCmd = currentDoc + "\n" + currentCmd;
        }

        if (currentComment != null) {
            currentCmd = currentComment + "\n" + currentCmd;
        }

        CmdItem cmdItem = hub.createCmd(currentCmd, command, type, fileItem,
                cmdRange, currentDoc, currentComment, docRange, info);

        //
        // deal with matching files
        //
        if (command.equals("use")) {
            String cmdfile;
            if (info.containsKey("using")) {
                cmdfile = info.get("using").toString();
            } else {
                cmdfile = parameters;
            }
            // use, set that MatchItem accordingly
            currentIn = hub.createMatch(cmdItem.getLine(), "file:data");
            currentIn.put("term", cmdfile.replaceAll("\"", ""));
            currentIn.put("regex",
                    StataUtils.stataTokenToRegex(cmdfile.replaceAll("\"", "")));
            currentIn.put("relation", "use");
            currentIn.put("origin", cmdItem);
            fileItem.addChild("match:input:data", currentIn);
            cmdItem.addChild("match:input:data", currentIn);
        } else if (type.equals("cmd:importcmd")) {
            String cmdfile;
            if (info.containsKey("using")) {
                cmdfile = info.get("using").toString();
            } else {
                cmdfile = parameters;
            }

            MatchItem in = hub.createMatch(cmdItem.getLine(), "file:data");
            in.put("term", cmdfile.replaceAll("\"", ""));
            in.put("regex",
                    StataUtils.stataTokenToRegex(cmdfile.replaceAll("\"", "")));
            in.put("relation", "use");
            in.put("origin", cmdItem);

            fileItem.addChild("match:input:data", in);
            cmdItem.addChild("match:input:data", in);
        } else if (type.equals("cmd:outputcmd") && command.startsWith("save")) {
            // use, set that MatchItem accordingly
            // currentOut = hub.createMatch( "file:data" );
            String cmdfile;
            if (info.containsKey("using")) {
                cmdfile = info.get("using").toString();
            } else {
                cmdfile = parameters;
            }

            currentOut.put("term", cmdfile);
            currentOut.put("regex",
                    StataUtils.stataTokenToRegex(cmdfile.replaceAll("\"", "")));
            currentOut.put("relation", "produce");
            currentOut.put("origin", cmdItem);
            fileItem.addChild("match:output:data", currentOut);
            cmdItem.addChild("match:output:data", currentOut);
            currentOut = hub.createMatch(cmdItem.getLine(), "file:data");

            // TODO check whether we should change the currentIn to currentOut
            // somehow
        } else if (type.equals("cmd:outputcmd") || info.containsKey("using")) {

            String cmdfile;
            if (info.containsKey("using")) {
                cmdfile = info.get("using").toString();
            } else {
                cmdfile = parameters;
            }

            MatchItem out = hub.createMatch(cmdItem.getLine(), "file");
            out.put("term", cmdfile);
            out.put("regex",
                    StataUtils.stataTokenToRegex(cmdfile.replaceAll("\"", "")));
            out.put("relation", "produce");
            out.put("origin", cmdItem);
            fileItem.addChild("match:output:file", out);
            cmdItem.addChild("match:output:file:" + command, out);
        }

        if (type.equals("cmd:runcmd")) {

            String cmdfile;
            if (info.containsKey("using")) {
                cmdfile = info.get("using").toString();
            } else {
                cmdfile = parameters;
            }

            MatchItem run = hub.createMatch(cmdItem.getLine(), "file:script");
            run.put("term", cmdfile);
            run.put("regex",
                    StataUtils.stataTokenToRegex(cmdfile.replaceAll("\"", "")));
            run.put("relation", "call");
            run.put("origin", cmdItem);
            fileItem.addChild("match:run:file", run);
            cmdItem.addChild("match:run:file", run);
        }

        for (String field : new String[] { "prefix", "parameters", "if", "in",
                "weight", "options" }) {
            if (cmdItem.containsKey(field)) {
                Set<String> params = StataUtils.parametersCleanSplit(cmdItem
                        .get(field).toString());
                int counter = 0;
                for (String param : params) {
                    // check for content and not number
                    if (param.trim().length() > 0 && !param.matches("[\\d]+")) {
                        String regex = StataUtils.stataTokenToRegex(param
                                .replaceAll("\"", ""));
                        
                        // check if there is at least one character to match
                        if (regex.matches(".*[a-zA-Z].*")) {

                            counter++;
                            MatchItem var = hub.createMatch(cmdItem.getLine()
                                    + "-" + counter, "variable");
                            var.put("term", param);
                            var.put("regex", regex);
                            var.put("relation", type);
                            var.put("origin", cmdItem);
                            var.put("field", field);

                            cmdItem.addChild("match:input:file", currentIn);
                            cmdItem.addChild("match:output:file", currentOut);
                            cmdItem.addChild("match:var", var);
                        }
                    }
                }
            }
        }

        return cmdItem;
    }

    private static String commandType(String command, Map<String, String[]> map) {
        String type = "cmd:other";
        for (String r : map.keySet()) {
            for (String t : map.get(r)) {
                if (command.equals(t)) {
                    type = "cmd:" + r;
                }
            }
        }
        return type;
    }

}
