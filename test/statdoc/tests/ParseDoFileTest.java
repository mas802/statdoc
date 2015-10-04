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
package statdoc.tests;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;

import statdoc.items.Item;
import statdoc.items.StatdocItemHub;
import statdoc.tasks.files.TextFileTask;
import statdoc.tasks.stata.StataDoFileTask;
import statdoc.tasks.stata.StataUtils;

@RunWith(JUnit4.class)
public class ParseDoFileTest {

    @Test
    public void testDo() {

        File file = new File("test/statdoc/tests/testfile.do");
        // File file = new
        // File("/Users/mas/Dropbox/Public/cameron/t2_mcloop.do");

        StatdocItemHub hub = new StatdocItemHub();
        Map<String, String[]> map = new HashMap<String, String[]>();
        map.put("none", new String[] { "none" });
        hub.setStataCmdTypes(map);
        hub.sourceDir = new File("tests/statdoc/tests").toPath();

        TextFileTask t1 = new TextFileTask(file.toPath(), "file:script", hub,
                null);

        t1.run();

        /*
         * FileItem item = hub.getFiles().iterator().next(); StataDoFileTask
         * task = new StataDoFileTask(item, null);
         * 
         * task.run();
         * 
         * System.out.println(); System.out.println(item.getSummary());
         * 
         * for (Item i : item.getChildren()) { if (i instanceof CmdItem )
         * System.out.print( "###"+((CmdItem)i).getLine()+"\n" + i.getContent()
         * + "\n###\n" ); // System.out.print(i.getName()); //
         * System.out.print("\t" + i.getType()); // System.out.print( "\t" +
         * i.getFullName() ); // System.out.print("\t" + ((Item)
         * i).getContent()); // System.out.print("\t" + ((Item)
         * i).getSummary()); // System.out.print("\t" + ((Item) i).keySet()); //
         * System.out.println("\t" + ((Map<String, Object>) i).values()); }
         */
    }

    @Test
    public void testSameLineComment() {

        File file;
        try {
            file = File.createTempFile("test", ".do");

            FileWriter fw = new FileWriter(file);
            fw.write("// 1. Provide summary and descriptive statistics \n"
                    + "sum			// The -summarize- command can be abbreviated to -su-\n");
            fw.close();

            StatdocItemHub hub = new StatdocItemHub();
            Map<String, String[]> map = new HashMap<String, String[]>();
            map.put("desc", new String[] { "sum" });
            hub.setStataCmdTypes(map);
            hub.sourceDir = new File("/").toPath();

            StataDoFileTask t2 = new StataDoFileTask(file.toPath(),
                    "cmd:stata:do", hub, null);

            t2.run();

            System.out.println(hub.getCmds().getChildren().size());

            org.junit.Assert
                    .assertEquals(1, hub.getCmds().getChildren().size());

            Item i = hub.getCmds().getChildren().iterator().next();

            org.junit.Assert.assertTrue(i.containsKey("command"));
            org.junit.Assert.assertTrue(i.containsKey("comment"));

            org.junit.Assert.assertEquals("sum", i.get("command"));
            org.junit.Assert.assertEquals(
                    "// 1. Provide summary and descriptive statistics",
                    i.getDescription());
            org.junit.Assert.assertEquals(
                    "// The -summarize- command can be abbreviated to -su-",
                    i.get("comment"));

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        /*
         * FileItem item = hub.getFiles().iterator().next(); StataDoFileTask
         * task = new StataDoFileTask(item, null);
         * 
         * task.run();
         * 
         * System.out.println(); System.out.println(item.getSummary());
         * 
         * for (Item i : item.getChildren()) { if (i instanceof CmdItem )
         * System.out.print( "###"+((CmdItem)i).getLine()+"\n" + i.getContent()
         * + "\n###\n" ); // System.out.print(i.getName()); //
         * System.out.print("\t" + i.getType()); // System.out.print( "\t" +
         * i.getFullName() ); // System.out.print("\t" + ((Item)
         * i).getContent()); // System.out.print("\t" + ((Item)
         * i).getSummary()); // System.out.print("\t" + ((Item) i).keySet()); //
         * System.out.println("\t" + ((Map<String, Object>) i).values()); }
         */
    }

    /**
     * test the following line
     * 
     * if substr("${JK_s`i'}",1,2)=="S_" {
     */
    @Test
    public void testIndentProblem1() {

        String content = "if substr(\"${JK_s`i'}\",1,2)==\"S_\" {" + "\n"
                + "indented line" + "\n" + "}";

        Map<String, String[]> map = new HashMap<String, String[]>();
        map.put("desc", new String[] { "sum" });

        Item i = runDoParser(content, map);

        // System.out.println( i.getContent() );

        for (Item c : i.getChildrenBy("cmd:")) {
            System.out.println(c.get("indent") + " : " + c.getContent());
        }

        Collection<Item> cmds = i.getChildrenBy("cmd:");
        Iterator<Item> it = cmds.iterator();

        assertEquals(0, it.next().get("indent"));
        assertEquals(1, it.next().get("indent"));
        assertEquals(0, it.next().get("indent"));

    }

    /**
     * test the following line
     * 
     * sum distid *`i'
     * 
     * should match nothing as plain wildcard
     */
    @Test
    public void testMatchingProblem1() {

        String content = "sum a bd distid *`i' * a*";

        Map<String, String[]> map = new HashMap<String, String[]>();
        map.put("desc", new String[] { "sum" });

        Item i = runDoParser(content, map);

        // System.out.println( i.getContent() );

        for (Item c : i.getChildrenBy("cmd:")) {
            System.out.println(c.get("indent") + " : " + c.getContent());
        }

        Collection<Item> cmds = i.getChildrenBy("cmd:");
        Iterator<Item> it = cmds.iterator();

        Item line = it.next();

        assertEquals(0, line.get("indent"));

        System.out.println(line.getChildren());

        Collection<Item> matches = line.getChildrenBy("match:");
        Iterator<Item> itm = matches.iterator();

        for( Item match: matches ) {
            System.out.println(match.get("regex"));
        }
        
        assertEquals(5, matches.size());
        
    }

    /**
     * test the following line
     * 
     * sum abc, ///
     * option
     * 
     * should match nothing as plain wildcard
     */
    @Test
    public void testTripleSlash() {

        String content = "sum a b c, ///\noption";

        Map<String, String[]> map = new HashMap<String, String[]>();
        map.put("desc", new String[] { "sum" });

        Item i = runDoParser(content, map);

        // System.out.println( i.getContent() );

        for (Item c : i.getChildrenBy("cmd:")) {
            System.out.println(c.get("indent") + " : " + c.getContent());
        }

        Collection<Item> cmds = i.getChildrenBy("cmd:");
        Iterator<Item> it = cmds.iterator();

        Item line = it.next();
        assertEquals(1, cmds.size());

        assertEquals(0, line.get("indent"));

        System.out.println(line.getContent());

        
    }

    /*
     * helper method to run do file
     */
    private Item runDoParser(String content, Map<String, String[]> cmdTypes) {

        Item i = null;
        File file;
        try {
            file = File.createTempFile("test", ".do");

            FileWriter fw = new FileWriter(file);
            fw.write(content);
            fw.close();

            StatdocItemHub hub = new StatdocItemHub();
            hub.setStataCmdTypes(cmdTypes);
            hub.sourceDir = new File("/").toPath();

            StataDoFileTask t2 = new StataDoFileTask(file.toPath(),
                    "cmd:stata:do", hub, null);

            t2.run();

            org.junit.Assert.assertEquals(1, hub.getFiles().size());

            i = hub.getFiles().iterator().next();

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return i;
    }

    @Test
    public void testSplit() {

        String[] result;
        result = StataUtils
                .splitSave(
                        "syntax anything [if] [in] [aweight fweight iweight pweight /], Cluster(string) [NOEIGenfix *]",
                        ",");
        System.out.print(":::" + result[0] + ":::");
        if (result.length > 1) {
            System.out.print(":::" + result[1] + ":::");
        }
        System.out.println();

        String s = "while ( regexm(\"`options'\",\"robust\")==1 ) {";
        result = StataUtils.splitSave(s, ",");
        System.out.print(":::" + result[0] + ":::");
        if (result.length > 1) {
            System.out.print(":::" + result[1] + ":::");
        }
        System.out.println();

    }

    @Test
    public void testParametersCleanSplit() {

        Set<String> results;
        results = StataUtils
                .parametersCleanSplit("xtreg lnwage policy age age2 yrseduc , re vce(bootstrap , reps(399) seed(10101) )");
        // .parametersCleanSplit("syntax any`th`in''g [if] [in] [aw${ei$_xxx9798g}ht fweight iweight pweight /], Cluster(string) [NOEIGenfix *]");
        for (String result : results) {
            System.out.print(":::" + result + ":::");
        }
        System.out.println();

    }

    @Test
    public void testStataTokenToRegex() {

        String[] tests = new String[] { "", "test", "test`with_local'",
                "test*with wildcard", "$test with global",
                "${global${nested}with$test} both cases",
                "simple bracket ${global_var}" };

        for (String test : tests) {
            String result;
            System.out.println(test);
            result = StataUtils.stataTokenToRegex(test);
            System.out.print(":::" + result + ":::");
            System.out.println();
            System.out.println();
        }

    }

    @Test
    public void testMatch() {
        String a = "/Users/schaffne/Dropbox/projects/striker bias/DataAnalysis2006_11/dta/matchValues.dta";
        String b = "dta.matchValues\\.dta";

        Pattern pattern = Pattern.compile(".*" + b + ".*");

        System.out.println(pattern.matcher(a).matches());
    }

    @Test
    public void testBalance() {
        System.out.println(StataUtils.balanceChars("\n/*", "/*", "*/"));
        System.out.println(StataUtils.balanceChars("   /*  \n */", "/*", "*/"));
        System.out.println(StataUtils.balanceChars("   */   ", "/*", "*/"));

        System.out.println(StataUtils.balanceChars("   */   ", "/", "*"));
        System.out.println(StataUtils.balanceChars(" //  */   ", "/", "*"));
        System.out.println(StataUtils.balanceChars("   */ /*  ", "/", "*"));

    }

    @Test
    public void testSCML2txt() {

        String text = "  an output1\n{com}. some command\n{txt} 2{com}. a looped command\nan output1\n{txt} 1232{com}. a long number looped command\nan output1\n";

        System.out.println(StataUtils.smcl2hidden(text, true));

    }

}
