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
package statdoc.items;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import statdoc.utils.NaturalComparator;

/**
 * The Hub holds and creates all items and contains maintenance methods.
 * 
 * Everything has to be Thread-safe in here (synchronize all data
 * manipulations).
 * 
 * This would be the DAO in a MVC pattern (eventually).
 * 
 * @author Markus Schaffner
 * 
 */
public class StatdocItemHub {

    private static final Pattern BOUNDARYSPLIT = Pattern
            .compile("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)|\\b");

    /*
     * fields to hold main Item collections
     * TODO should be unified or even put into a superitem (or DB)
     */

    private TreeSet<FileItem> files = new TreeSet<FileItem>();
    private Item cmds = new Item();
    private Map<String, TokenItem> tokens = new TreeMap<String, TokenItem>();
    private Map<String, Item> vars = new TreeMap<String, Item>();
    private List<MatchItem> matches = new ArrayList<MatchItem>();

    private Properties prop = null;
    
    /*
     * fields with general information/resources
     */

    public File sourceDir;
    public File outputDir;
    public File templateDir;

    private Map<String, Object> globals = new TreeMap<String, Object>();
    private String stataPath;
    private Map<String, String[]> stataCmdTypes;

    /* 
     * GETTER / SETTERS and data access
     */
    public Properties getProp() {
	return prop;
    }

    public void setProp(Properties prop) {
	this.prop = prop;
    }
    
    public void setGlobal(String key, Object value) {
        globals.put(key, value);
    }

    public Map<String, Object> getGlobals() {
        return this.globals;
    }

    public TreeSet<FileItem> getFiles() {
        return files;
    }

    public Map<String, TokenItem> getTokens() {
        return tokens;
    }

    public Map<String, Item> getVariables() {
        return vars;
    }

    public void setStataPath(String stataPath) {
        this.stataPath = stataPath;
    }

    public String getStataPath() {
        return this.stataPath;
    }

    public void setStataCmdTypes(Map<String, String[]> stataCmd) {
        this.stataCmdTypes = stataCmd;
    }

    public Map<String, String[]> getStataCmdTypes() {
        return this.stataCmdTypes;
    }

    public Item getCmds() {
        return cmds;
    }

    /*
     * FILES
     */

    synchronized public FileItem createFile(File file, File rootDir, String type) {
        FileItem fileItem = new FileItem(file, rootDir, type);

        files.add(fileItem);

        fileItem.put("date", new Date(file.lastModified()));

        // create tokens from filename
        createTokens(fileItem, file.getName());

        return fileItem;
    }

    /*
     * VARIABLES
     */

    synchronized public VariableItem createVariable(String name, String type,
            FileItem source) {

        VariableItem item = null;
        Item varGroup;

        if (vars.containsKey(name)) {
            varGroup = vars.get(name);
        } else {
            varGroup = new Item(name, name, "variables/" + name + ".html",
                    "variable:group");
            vars.put(name, varGroup);
        }

        for (Item vi : varGroup.getChildrenBy("variable:")) {
            FileItem fi = ((VariableItem) vi).getFileItem();
            if (fi.equals(source)) {
                item = (VariableItem) vi;
            }
        }

        if (item == null) {
            item = new VariableItem(name, source, type);
            varGroup.addChild("variable:" + type, item);
            ;
        }

        // create tokens from file name
        createTokens(item, name);

        return item;
    }

    /*
     * TOKENS
     */

    synchronized public TokenItem createToken(String token, String type) {
        TokenItem ti;
        if (this.tokens.containsKey(token)) {
            ti = this.tokens.get(token);
        } else {
            ti = new TokenItem(token);
            ti.setType("token:" + type);
            this.tokens.put(token, ti);
        }
        return ti;
    }

    synchronized public void createTokens(Item source, String name) {
        String[] tokens = BOUNDARYSPLIT.split(name.replaceAll("[^a-zA-Z0-9 ]",
                " "));

        int ngrams = 2;
        int position = 1;
        String[] prev = new String[ngrams + 1];

        for (String tokenf : tokens) {
            if (tokenf.trim().length() > 1) {

                String token = tokenf.trim();
                int loop = Math.min(position, ngrams);

                for (int i = 0; i < loop; i++) {
                    if (token.length() >= (i + 1) * 3) {
                        TokenItem ti = createToken(token, "" + i);
                        ti.addChild(source);
                        source.addChild(ti);
                    }
                    token = (prev[i] + " " + token).trim();
                }

                // shift the prev and increase position
                for (int i = ngrams; i > 0; i--) {
                    prev[i] = prev[i - 1];
                }

                prev[0] = tokenf.trim();
                position++;
            }
        }
    }

    /*
     * CMDITEM
     */

    /**
     * Main method to deal with a generalised command line/artefact.
     * 
     * Should deal with matching up the found inputs/output to other items. And
     * embed the CmdItem into the fabric of the project.
     * 
     * @return a freshly created CmdItem
     */
    synchronized public CmdItem createCmd(String currentCmd, String command,
            String type, FileItem fileItem, Integer[] range, String currentDoc,
            String currentComment, Integer[] docRange, Map<String, Object> info) {

        CmdItem cmdItem = new CmdItem(currentCmd, command, type, fileItem,
                range);

        cmdItem.putAll(info);

        cmds.addChild(cmdItem);

        // check whether there is a previous comment/doc to be
        // applied
        if (currentDoc != null) {
            addDocToItem(cmdItem, currentDoc, docRange);
        } else if (currentComment != null) {
            addDocToItem(cmdItem, currentComment, docRange);
        }

        fileItem.addChild(cmdItem);

        return cmdItem;
    }

    public void addDocToItem(Item item, String doc, Integer[] range) {
        String[] lines = doc.trim().split("\n");
        if (lines.length > 0) {
            item.setDescription(lines[0]);
        }

        /*
        item.put("_docFirstLine", range[0]);
        item.put("_docLastLine", range[1]);
        */

        boolean inSummary = true;
        String summary = "";
        for (String line : lines) {
            if (checkAndAddMetadata(item, line)) {
                inSummary = false;
            } else if (line.trim().equals("")) {
                inSummary = false;
            } else if (inSummary) {
                summary = summary + " " + line.trim();
            }
        }

        item.setSummary(summary);

    }

    /* 
     * MATCHES
     */

    synchronized public MatchItem createMatch(String name, String type) {
        MatchItem m = new MatchItem(name, type);

        matches.add(m);
        return m;
    }

    synchronized public void resolveMatches() {
        for (MatchItem m : matches) {
            resolveMatch(m);
        }
    }

    public void resolveMatch(MatchItem m) {

        if (m.isResolved())
            return;

        if (m != null && m.containsKey("regex")) {
            String regex = m.get("regex").toString().trim();
            String relation = m.get("relation").toString();

            Item origin = (Item) m.get("origin");

            String targetType = m.get("targetType").toString();

            List<Item> matchList = new ArrayList<Item>();

            // only resolve regex if it contains info
            if (regex.length() > 2) {
                /*
                 * check data files first
                 */
                if (targetType.startsWith("file") && regex.matches("\\.\\*\\\\.[a-zA-Z].*") ) {
                    Pattern pattern = Pattern.compile(".*" + regex + ".*");
                    for (FileItem fi : files) {
                        if (fi.getType().startsWith(targetType)) {
                            String matchee = fi.getFile().getAbsolutePath();
                            if (pattern.matcher(matchee).matches()) {
                                matchList.add(fi);
                                MatchItem mo = new MatchItem("back_of"
                                        + m.getName(), fi.getType());
                                mo.addChild(origin);
                                mo.put("term", origin.getName());
                                fi.addChild("match:" + relation, mo);
                            }
                        }
                    }
                }
                /*
                 * check variables second
                 */
                else if (targetType.startsWith("variable")) {
                    Set<Item> dataFiles = new HashSet<Item>();
                    Collection<Item> dataMatch = origin.getChildrenBy(
                            "match:output", "match:input");
                    for (Item mi : dataMatch) {
                        resolveMatch((MatchItem) mi);
                        dataFiles.addAll(mi.getChildrenBy("file"));
                    }
                    Pattern pattern = Pattern.compile(regex);

                    for (String varname : vars.keySet()) {
                        if (pattern.matcher(varname).matches()) {
                            boolean hasExactMatch = false;

                            for (Item vii : vars.get(varname).getChildrenBy(
                                    "variable:")) {
                                VariableItem vi = (VariableItem) vii;
                                for (Item data : dataFiles) {
                                    if (data.equals(vi.getFileItem())) {
                                        // we have an exact match
                                        hasExactMatch = true;
                                        matchList.add(vi);
                                        MatchItem mo = new MatchItem("back_of"
                                                + m.getName(), vi.getType());
                                        mo.addChild(origin);
                                        mo.put("term", origin.getName());
                                        vi.addChild("match:" + relation, mo);
                                    }
                                }
                            }

                            if (!hasExactMatch) {
                                if (vars.get(varname)
                                        .getChildrenBy("variable:").size() == 1) {
                                    for (Item vi : vars.get(varname)
                                            .getChildrenBy("variable:")) {
                                        matchList.add(vi);
                                        MatchItem mo = new MatchItem("back_of"
                                                + m.getName(), vi.getType());
                                        mo.addChild(origin);
                                        mo.put("term", origin.getName());
                                        vi.addChild("match:" + relation, mo);
                                    }
                                } else {
                                    matchList.add(vars.get(varname));
                                }
                            }

                            MatchItem mo = new MatchItem("general_back_of"
                                    + m.getName(), vars.get(varname).getType());
                            mo.addChild(origin);
                            mo.put("term", origin.getName());
                            vars.get(varname).addChild("match:" + relation, mo);
                        }

                    }
                }
            }

            m.setAsResolved();
            if (matchList.size() > 0) {
                for (Item i : matchList) {
                    m.addChild(i);
                }
            } else {
                m.addWarning("item could not be resolved");
                if (m != null && m.containsKey("term")) {
                    String stripterm = m.get("term").toString()
                            .replaceAll("[^a-zA-Z0-9_]", "").trim();
                    TokenItem ti = createToken(stripterm, "match");
                    ti.addChild(origin);
                    m.addChild(ti);
                    // origin.addChild(ti);
                }
            }
        } else {
            if (m != null) {
                m.addWarning("item could not be resolved");
            }
        }
    }

    /*
     * ITEM UTILITIES
     */

    /**
     * check a String if it contains information to be added to the Item and
     * adds it to the Item if found
     * 
     * TODO this should allow for matching and linking as well
     * 
     * @param item
     *            the target item
     * @param line
     *            the line with potential information
     * 
     * @return true if the line is metadata
     */
    public boolean checkAndAddMetadata(Item item, String line) {
        boolean isMeta = false;
        if (line.trim().startsWith("@")) {
            try {
                isMeta = true;
                String[] tokens = line.trim().split(" ");
                if (line.trim().length() > tokens[0].length() + 1) {
                    String key = tokens[0].substring(1);
                    String value = line.trim()
                            .substring(tokens[0].length() + 1);
                    if (key.equals("summary")) {
                        item.setSummary(value);
                    } else if (key.equals("token")) {
                        createTokens(item, value);
                    } else {
                        item.put(key, value);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Problematic line " + line);
                item.addWarning("error while parsing line " + line + ": <br>"
                        + e.getMessage());
            }
        }
        return isMeta;
    }

    /**
     * utility method to put a collection of items into a typed TreeMap
     * (sorted).
     * 
     * @param items
     *            a collection of items
     * @return a map with items by type
     */
    public static Map<String, Collection<Item>> getItemTypeMap(
            Collection<? extends Item> items) {
        Map<String, Collection<Item>> result = new TreeMap<String, Collection<Item>>();

        for (Item item : items) {
            String type = item.getType();
            if (result.containsKey(type)) {
                result.get(type).add(item);
            } else {
                TreeSet<Item> ts = new TreeSet<Item>(NaturalComparator.INSTANCE);
                ts.add(item);
                result.put(type, ts);
            }
        }

        return result;
    }

    /**
     * utility method to put a collection of items into a grouped Collection.
     * 
     * @param items
     *            a collection of items
     * @param number
     *            the number of items needed to form a group
     * @param minlength
     *            the minimum length of teh string to group over (usually >=3)
     * @return a collection with grouped items
     */
    public static Collection<Item> groupMap(Collection<Item> items, int number,
            int minlength, String type) {

        String m = "notset";
        int counter = 0;

        TreeSet<Item> set = new TreeSet<Item>(new Comparator<Item>() {

            @Override
            public int compare(Item o1, Item o2) {
                if (o1.getName().equals(o2.getName())) {
                    return o1.compareTo(o2);
                } else {
                    return o1.getName().compareTo(o2.getName());
                }
            }
        });

        set.addAll(items);

        Collection<String> tmp = new TreeSet<String>();

        for (Item item : set) {
            String n = item.getName();

            // check if it matches
            if ((counter > 0) && (n.length() >= m.length())
                    && n.substring(0, m.length()).equals(m)
                    && n.length() >= minlength) {
                counter++;
                if (counter > number) {
                    tmp.add(m);
                }
            } else if (n.length() < minlength) {
                m = "notset";
                counter = 0;
            } else {
                if (counter > number) {
                    tmp.add(m);
                }

                // check if a shorter string will match
                int shorter = 0;
                for (int i = 0; i < n.length(); i++) {
                    if (n.charAt(i) == m.charAt(i)) {
                        shorter++;
                    } else {
                        break;
                    }
                }
                if (shorter > minlength) {
                    tmp.remove(m);
                    m = n.substring(0, shorter);
                    if (counter > number) {
                        tmp.add(m);
                    }
                    counter++;
                } else {
                    counter = 1;
                    m = n;
                }

            }
        }

        if (counter > number) {
            tmp.add(m);
        }

        TreeSet<Item> outter = new TreeSet<Item>(new Comparator<Item>() {

            @Override
            public int compare(Item o1, Item o2) {
                if (o1.getName().equals(o2.getName())) {
                    return o1.compareTo(o2);
                } else {
                    return o1.getName().compareTo(o2.getName());
                }
            }
        });
        outter.addAll(set);

        String[] iter = tmp.toArray(new String[] {});

        if (tmp.size() > 0) {
            int i = 0;
            String current = iter[i];
            Item group = new Item(current, current, "#" + current);
            ;
            group.setType(type);

            for (Item item : set) {
                String n = item.getName();
                if (!n.startsWith(current) && n.compareTo(current) > 0) {
                    i++;
                    if (tmp.size() > (i)) {
                        current = iter[i];
                        group = new Item(current, current, "#" + current);
                        ;
                        group.setType(type);
                    }
                }
                if (n.startsWith(current)) {
                    group.addChild(item);
                    outter.remove(item);
                    outter.add(group);
                }
            }
        }

        return outter;
    }

    public String stats() {
        return "Variables: " + vars.size() + "  | Files: " + files.size()
                + "  | Tokens: " + tokens.size();
    }

}
