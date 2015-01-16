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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import statdoc.utils.TemplateUtil;

/**
 * general utils to deal with Stata
 * 
 * in particular this class allows to convert smcl into various formats like txt
 * and html.
 * 
 * Futhermore it allows tokenising of Stata specific Strings
 * 
 * @author Markus Schaffner
 * 
 */
public class StataUtils {

    public static final char M0 = (char) 28; // text 
    public static final char M1 = (char) 29; // input/com
    public static final char M2 = (char) 30; // error
    public static final char M3 = (char) 31; // result

    public static final char M4 = (char) 17; // bold 
    public static final char M5 = (char) 18; // italic
    public static final char M6 = (char) 19; // unassigned
    public static final char M7 = (char) 20; // unassigned

    static String[][] rules = new String[][] {
            // new String[] { "\\{txt\\}end of do-file[\r|\n]+", "" },
            // new String[] { "end of do-file[\r|\n]+", "" },

            new String[] { "\\{smcl\\}[\r|\n]+", "" + M0 },
            new String[] { "\\{ul (on)?(off)?\\}", "" + M0 },

            new String[] { "\\{sf\\}", "" + M0 },
            new String[] { "\\{bf\\}", "" + M0 + M4 },
            new String[] { "\\{it\\}", "" + M0 + M5 },

            new String[] { "\\{sf:(.*?)\\}", M0 + "$1" + M0 },
            new String[] { "\\{bf:(.*?)\\}", M4 + "$1" + M0 },
            new String[] { "\\{it:(.*?)\\}", M5 + "$1" + M0 },

            new String[] { "\\{inp(ut)?\\}", "" + M0 + M1 },
            new String[] { "\\{t[e]?xt\\}", "" + M0 },
            new String[] { "\\{res(ult)?\\}", "" + M0 + M3 },
            new String[] { "\\{err(or)?\\}", "" + M0 + M2 },

            new String[] { "\\{inp(ut)?:(.*?)\\}", M1 + "$1" + M0 },
            new String[] { "\\{t[e]?xt:(.*?)\\}", M0 + "$1" + M0 },
            new String[] { "\\{res(ult)?:(.*?)\\}", M3 + "$1" + M0 },
            new String[] { "\\{err(or)?:(.*?)\\}", M2 + "$1" + M0 },

            new String[] { "\\{com(mand)?\\}", "" + M0 + M1 },

            new String[] { "\\{search (.*?)\\}", M1 + "$1" + M0 },

            // TODO, does this exist?
            new String[] { "\\{bt\\}", "" + M0 + M1 },

            new String[] { "\\{c(har)? -\\(\\}", "{" }, // +(char)196},
            new String[] { "\\{c(har)? \\)-\\}", "}" }, // +(char)196},
            new String[] { "\\{c(har)? -\\}", "-" }, // +(char)196},
            new String[] { "\\{c(har)? \\|\\}", "|" }, // +(char)179},
            new String[] { "\\{c(har)? \\+\\}", "+" }, // +(char)197},
            new String[] { "\\{c(har)? TT\\}", "-" }, // +(char)194},
            new String[] { "\\{c(har)? BT\\}", "-" }, // +(char)193},
            new String[] { "\\{c(har)? LT\\}", "-" }, // +(char)195},
            new String[] { "\\{c(har)? RT\\}", "-" }, // +(char)180},
            new String[] { "\\{c(har)? TLC\\}", "-" }, // +(char)218},
            new String[] { "\\{c(har)? TRC\\}", "-" }, // +(char)191},
            new String[] { "\\{c(har)? BRC\\}", "-" }, // +(char)217},
            new String[] { "\\{c(har)? BLC\\}", "-" }, // +(char)192},

            new String[] { "\\{p(.*?)\\}", "" },
    // new String[]
    // {"(\\{com\\}\\. )?[\r|\n]+\\{txt\\}end of do-file[\r|\n]+\\{smcl\\}[\r|\n]+",
    // ""},
    };

    private static Map<Pattern, String> map = new HashMap<Pattern, String>();

    synchronized static Map<Pattern, String> getPatternMap() {

        if (map.isEmpty()) {
            for (String[] s : rules) {
                map.put(Pattern.compile(s[0]), s[1]);
            }

            // hline and space
            String s = "";
            String sp = "";
            for (int i = 1; i < 80; i++) {
                s = s + "-";
                map.put(Pattern.compile("\\{hline " + i + "\\}"), s);
                sp = sp + " ";
                map.put(Pattern.compile("\\{space " + i + "\\}"), sp);
                // c = c.replaceAll("\\{col "+i+"\\}","");
            }
            map.put(Pattern.compile("\\{hline\\}"), s);
            map.put(Pattern.compile("\\{\\.-\\}"), s);
        }

        return map;
    }

    private static final Pattern pright = Pattern.compile("\\{right:(.*?)\\}");
    private static final Pattern pcol = Pattern.compile("\\{col ([0-9]*?)\\}");

    // TODO ralign
    //private static final Pattern pralign = Pattern.compile("\\{ralign:(.*?)\\}");

    /**
     * utility method to replace scml commands with their hidden counterparts
     * 
     * @param content
     *            A smcl string
     * @param ignoreCommands
     *            a boolean whether to process the commands given or not
     * 
     * @return the smcl string with all formating replaced by hidden tokens
     *         (M0,M1,M2,M3)
     */
    static public String smcl2hidden(String content, boolean ignoreCommands) {
        StringBuilder r = new StringBuilder();

        for (Map.Entry<Pattern, String> e : getPatternMap().entrySet()) {
            //            content = StringUtils.replace( content, s[0], s[1]);
            content = e.getKey().matcher(content).replaceAll(e.getValue());
        }

        for (String c : content.split("\n")) {

            // break if line starts with a dot an flag is set
            if (ignoreCommands
                    && ((c.startsWith(". ") || (c.length() > 3
                            && (c.charAt(0) == 28 && c.charAt(1) == 29 && c
                                    .charAt(2) == '.') || c.matches("^[" + M0
                            + "][ ]*[\\d]+[" + M0 + "][" + M1 + "]\\. .*"))))) {
                // do nothing, i.e. do not process command lines
            } else {

                // fix columns
                Matcher m = pcol.matcher(c);

                while (m.find()) {
                    String match = m.group(0);
                    int n = Integer.parseInt(m.group(1)) - 1;

                    // System.out.println(n);

                    int x = c.indexOf(match);
                    int charCount = c
                            .substring(0, x)
                            .replaceAll(
                                    "[^" + M0 + M1 + M2 + M3 + M4 + M5 + "]",
                                    "").length();
                    x = x - charCount;

                    String replace = "";
                    if (x < n) {
                        for (; x < n; x++) {
                            replace = replace + " ";
                        }
                    }
                    c = c.replace(match, replace);
                }

                // fix RIGHT
                Matcher mright = pright.matcher(c);

                while (mright.find()) {
                    String match = mright.group(0);
                    String txt = mright.group(1);
                    int n = txt.length();

                    // System.out.println(n);

                    int x = c.indexOf(match);
                    int charCount = c
                            .substring(0, x)
                            .replaceAll(
                                    "[^" + M0 + M1 + M2 + M3 + M4 + M5 + "]",
                                    "").length();
                    x = x - charCount;
                    n = 80 - charCount - n + 1;

                    String replace = "";
                    if (x < n) {
                        for (; x < n; x++) {
                            replace = replace + " ";
                        }
                    }
                    c = c.replace(match, replace);
                    c = c + txt;
                }

                r.append(c);
                r.append("\n");
            }
        }
        return r.toString();

    }

    /**
     * a utility method to replace smcl formating with html tags
     * 
     * will use smcl2hidden as a workhorse
     * 
     * @param content
     *            the smcl string
     * @param hideCommands
     *            boolean of whether to process command lines
     * @return a html formated string
     */
    static public String smcl2html(String content, boolean hideCommands) {
        String result = "<span>" + smcl2hidden(content, hideCommands);

        result = result.replaceAll("" + M0 + M0,
                "</span><span class=\"st_txt\">");
        result = result.replaceAll("" + M0 + M1,
                "</span><span class=\"st_inp\">");
        result = result.replaceAll("" + M0 + M2,
                "</span><span class=\"st_err\">");
        result = result.replaceAll("" + M0 + M3,
                "</span><span class=\"st_res\">");
        result = result.replaceAll("" + M0 + M4,
                "</span><span class=\"st_bf\">");
        result = result.replaceAll("" + M0 + M5,
                "</span><span class=\"st_it\">");

        result = result.replaceAll("" + M0, "</span><span class=\"st_txt\">");
        result = result.replaceAll("" + M1, "</span><span class=\"st_inp\">");
        result = result.replaceAll("" + M2, "</span><span class=\"st_err\">");
        result = result.replaceAll("" + M3, "</span><span class=\"st_res\">");
        result = result.replaceAll("" + M4, "</span><span class=\"st_bf\">");
        result = result.replaceAll("" + M5, "</span><span class=\"st_it\">");

        result = result + "</span>";

        return result;
    }

    /**
     * a utility method to remoce smcl formating
     * 
     * will use smcl2hidden as a workhorse
     * 
     * @param content
     *            the smcl string
     * @param hideCommands
     *            boolean of whether to process command lines
     * @return plain txt string
     */
    public static String smcl2plain(String content, boolean hideCommands) {
        String result = smcl2hidden(content, hideCommands);

        result = result.replaceAll("" + M0, "");
        result = result.replaceAll("" + M1, "");
        result = result.replaceAll("" + M2, "");
        result = result.replaceAll("" + M3, "");
        result = result.replaceAll("" + M4, "");
        result = result.replaceAll("" + M5, "");

        return result;
    }

    /**
     * splits a Stata string into two with the last encounter of the split token
     * makes sure that round and square brackets are balanced on either side of
     * the split i.e. does not split within brackets
     * 
     * @param str
     *            String to split
     * @param splt
     *            split marker
     * @return an array of one or two elements
     */
    public static String[] splitSave(String str, String splt) {
        String[] result = null;

        int pos = -1;
        boolean resolved = false;

        int currentpos = 0;
        while (!resolved) {
            int newpos = str.substring(currentpos).indexOf(splt);
            if (newpos == -1) {
                resolved = true;
            } else {
                currentpos = newpos + currentpos;
                String strs = str.substring(0, currentpos);
                int a = balanceChars(strs, "(", ")");
                int c = balanceChars(strs, "[", "]");
                int e = countChar(strs, "\"");
                if (a == 0 && c == 0 && (e % 2 == 0)) {
                    // TODO check if we are really always after the last match otherwise:
                    // resolved = true;
                    // currently ptherwise certainly a problem if the prefix has an option (,)
                    pos = currentpos;
                }
                currentpos += splt.length();
            }
        }

        if (pos == -1) {
            result = new String[] { str };
        } else {
            result = new String[] { str.substring(0, pos),
                    str.substring(pos + splt.length()) };
        }

        return result;
    }

    public static int countChar(String string, String c) {
        int counter = 0;
        for (int i = 0; i < string.length() - (c.length() - 1); i++) {
            boolean check = true;
            for (int j = 0; j < c.length(); j++) {
                if (string.charAt(i + j) != c.charAt(j)) {
                    check = false;
                    break;
                }
            }
            if (check) {
                counter++;
            }
        }
        return counter;
    }

    public static int balanceChars(String string, String c, String d) {

        return countChar(string, c) - countChar(string, d);
    }

    //    private static final Pattern BOUNDARYSPLIT = Pattern
    //            .compile("[ ]+");

    public static Set<String> parametersCleanSplit(String parameter) {

        // remove all non stata special characters including brackets
        parameter = parameter.replaceAll("[^a-zA-Z0-9\\(\\*\\?`'$\\{\\}_ ]",
                " ");

        Set<String> s = new HashSet<String>();

        String[] params = parameter.split("[\\s]+");

        // deal with no space after opening brackets
        for (int i = 0; i < params.length; i++) {
            String p = params[i];
            // remove brackets from the start
            p = p.replaceAll("^\\(", "");
            if (p.length() > 1) {
                int pos = p.indexOf('(');
                if (pos > -1) {
                    s.add(p.substring(0, pos + 1));
                    s.add(p.substring(pos + 1));
                } else {
                    s.add(p);
                }
            }
        }

        return s;
    }

    public static String stataTokenToRegex(String wildcard) {
        // clear out stata lacal, global
        wildcard = wildcard.replaceAll("\\$\\{.*\\}", "*");
        wildcard = wildcard.replaceAll("`.*'", "*");
        wildcard = wildcard.replaceAll("\\$[a-zA-Z0-9\\*_]*", "*");

        // remove all residual special characters
        wildcard = wildcard.replaceAll("[\\$\\{\\}`']", " ");
        wildcard = wildcard.trim();

        StringBuffer s = new StringBuffer(wildcard.length());
        // s.append("^.*?");
        for (int i = 0, is = wildcard.length(); i < is; i++) {
            char c = wildcard.charAt(i);
            switch (c) {
            case '*':
                s.append(".*");
                break;
            case '?':
                s.append(".");
                break;
            case '/':
                s.append(".");
                break;
            case '\\':
                s.append(".");
                break;
            // escape special regexp-characters
            case '(':
            case ')':
            case '[':
            case ']':
            case '$':
            case '^':
                //            case '.':
            case '{':
            case '}':
            case '|':
                s.append("\\");
                s.append(c);
                break;
            default:
                s.append(c);
                break;
            }
        }
        // s.append(".*?$");
        return (s.toString());
    }

    public static String runTemplate(String templateName, String stataexe,
            Map<String, Object> data, File outputDir) throws IOException,
            InterruptedException {

        Runtime rt = Runtime.getRuntime();

        Process ps;
        // BufferedReader in;
        // BufferedWriter out;

        // System.out.println( statacmd );

        Path tempDir = Files.createTempDirectory("statdoc");

        TemplateUtil.getInstance().evalVMtoFile(
                tempDir.resolve(templateName + ".do").toFile(),
                templateName + ".do.vm", data);

        // FIXME horrible, horrible horrible implementation for Windose
        if (stataexe.toLowerCase().endsWith(".exe")) {
            ps = rt.exec(new String[] { stataexe, "-q", "-e", "-s", "do",
                    templateName + ".do" }, null, tempDir.toFile());
            ps.waitFor();
        } else {
            ps = rt.exec(new String[] { stataexe, "-q", "-s", "do",
                    templateName + ".do" }, null, tempDir.toFile());
            ps.waitFor();
        }

        Files.copy(tempDir.resolve(templateName + ".smcl"), outputDir.toPath(),
                StandardCopyOption.REPLACE_EXISTING);

        return "";
    }
}
