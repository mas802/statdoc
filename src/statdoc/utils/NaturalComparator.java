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
package statdoc.utils;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.regex.Pattern;

import statdoc.items.Item;

public class NaturalComparator implements Comparator<Item> {
    public static final NaturalComparator INSTANCE = new NaturalComparator();

    protected NaturalComparator() {
    }

    private static final Pattern BOUNDARYSPLIT = Pattern
            .compile("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");

    @Override
    public int compare(Item s1, Item s2) {
        int r = compareSplit(s1.getName(), s2.getName());

        if (r == 0) {
            r = s1.getFullName().compareTo(s2.getFullName());
        }

        return r;
    }

    public int compareSplit(String s1, String s2) {
        String[] s1Parts = BOUNDARYSPLIT.split(s1);
        String[] s2Parts = BOUNDARYSPLIT.split(s2);

        int i = 0;
        while (i < s1Parts.length && i < s2Parts.length) {

            // if parts are the same
            if (s1Parts[i].compareTo(s2Parts[i]) == 0) {
                ++i;
            } else {
                if (s1Parts[i].charAt(0) >= '0' && s1Parts[i].charAt(0) <= '9'
                        && s2Parts[i].charAt(0) >= '0'
                        && s2Parts[i].charAt(0) <= '9') {

                    try {

                        BigInteger int1 = new BigInteger(s1Parts[i]);
                        BigInteger int2 = new BigInteger(s2Parts[i]);

                        // if the parse works

                        int diff = int1.compareTo(int2);
                        if (diff == 0) {
                            ++i;
                        } else {
                            return diff;
                        }
                    } catch (Exception ex) {
                        // actually this point should never be reached now, maybe for very large
                        // numbers
                        return 0;
                    }
                } else if (s1Parts[i].charAt(0) >= '0'
                        && s1Parts[i].charAt(0) <= '9') {
                    return 1;
                } else if (s2Parts[i].charAt(0) >= '0'
                        && s2Parts[i].charAt(0) <= '9') {
                    return -1;
                } else {
                    return s1Parts[i].toLowerCase().compareTo(
                            s2Parts[i].toLowerCase());
                }
            }
        }

        // Handle if one string is a prefix of the other.
        // nothing comes before something.
        if (s1.length() < s2.length()) {
            return -1;
        } else if (s1.length() > s2.length()) {
            return 1;
        } else {
            return 0;
        }
    }

    public static final int compareNatural(String s1, String s2) {
        // Skip all identical characters
        int len1 = s1.length();
        int len2 = s2.length();
        int i;
        char c1, c2;
        for (i = 0, c1 = 0, c2 = 0; (i < len1) && (i < len2)
                && (c1 = s1.charAt(i)) == (c2 = s2.charAt(i)); i++)
            ;

        // Check end of string
        if (c1 == c2)
            return (len1 - len2);

        // Check digit in first string
        if (Character.isDigit(c1)) {
            // Check digit only in first string
            if (!Character.isDigit(c2))
                return ((i > 0) && Character.isDigit(s1.charAt(i - 1)) ? 1 : c1
                        - c2);

            // Scan all integer digits
            int x1, x2;
            for (x1 = i + 1; (x1 < len1) && Character.isDigit(s1.charAt(x1)); x1++)
                ;
            for (x2 = i + 1; (x2 < len2) && Character.isDigit(s2.charAt(x2)); x2++)
                ;

            // Longer integer wins, first digit otherwise
            return (x2 == x1 ? c1 - c2 : x1 - x2);
        }

        // Check digit only in second string
        if (Character.isDigit(c2))
            return ((i > 0) && Character.isDigit(s2.charAt(i - 1)) ? -1 : c1
                    - c2);

        // No digits
        return (c1 - c2);
    }
}
