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

import java.util.Collection;
import java.util.TreeSet;


import org.junit.Test;

import statdoc.items.Item;
import statdoc.items.StatdocItemHub;

public class SortTests {

    @Test
    public void sortTest() {

        String[] testSet = new String[] { "g", "gr", "green_blue_12", "green_12",
                "helpme", "unique", "green52", "green99123", "green99124",
                "green99125"};

        Collection<Item> set = new TreeSet<Item>();

        for (String s : testSet) {
            Item i = new Item(s, s, s);
            // i.put("tokens", s.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)|\\b"));
            set.add(i);

        }

        Collection<Item> result = StatdocItemHub.groupMap(set, 3, 3, "test");
        
        System.out.println(set);
        System.out.println(result);

        org.junit.Assert.assertEquals( result.size(), 5 );
    }
    
    @Test
    public void sortTestRERUN() {

        String[] testSet = new String[] { "RERUN2_snapPond1.dta", 
                "RERUN2_snapPond1.dta", "RERUN2_snapPond22.dta" , 
                "RERUN2_snapPond13.csv", "RERUN2_snapPond1.dta", 
                "RERUN2_snapPond.dta" };

        Collection<Item> set = new TreeSet<Item>();

        for (String s : testSet) {
            Item i = new Item(s, s, s);
            set.add(i);

        }

        Collection<Item> result = StatdocItemHub.groupMap(set, 3, 3, "test");
        
        System.out.println(set);
        System.out.println(result);

        org.junit.Assert.assertEquals( result.size(), 1 );
    }
    @Test
    public void doubleEntryTest() {

        String[] testSet = new String[] { "AAAAA" , "BBBBB", "BBBBB", "CCCCC" };

        Collection<Item> set = new TreeSet<Item>();

        int x = 0;
        for (String s : testSet) {
            x++;
            Item i = new Item(s, s+x, s);
            set.add(i);
        }

        Collection<Item> result = StatdocItemHub.groupMap(set, 3, 3, "test");

        System.out.println(set);
        System.out.println(result);

        org.junit.Assert.assertEquals( result.size(), 4 );
    }
    
    

    @Test
    public void sortTestBoth() {

        
        String[] testSet = new String[] { "a", "aa", "aaaaa_123", "aaaaa_456",
                "bbbbb", "unique", "aaaaa52", "aaaaa99123", "aaaaa99124",
                "AAAAA99125", "rerun_snapPond1.dta", 
                "rerun_snapPond1.dta", "rerun_snapPond22.dta" , 
                "rerun_snapPond13.csv", "rerun_snapPond1.dta", 
                "rerun_snapPond.dta", "AAAAA1", "AAAAA2", "AAAAA3" };

        Collection<Item> set = new TreeSet<Item>();

        for (String s : testSet) {
            Item i = new Item(s, s, s);
            // i.put("tokens", s.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)|\\b"));
            set.add(i);

        }

        Collection<Item> result = StatdocItemHub.groupMap(set, 3, 3, "test");
        
        System.out.println(set);
        System.out.println(result);

        org.junit.Assert.assertEquals( result.size(), 7 );
    }
    
//    @Test
//    public void sortTest2() {
//
//        StatdocItemHub hub = StatdocItemHub.getInstance();
//        hub.setStataPath("/Applications/Stata/StataMP.app/Contents/MacOS/stata-mp");
//        
//        File rootDir = new File("/Users/mas/Dropbox/EXPERIMENTS/Daylight Saving Project/Data_Analysis");
//        File file = new File("/Users/mas/Dropbox/EXPERIMENTS/Daylight Saving Project/Data_Analysis/dlproject_all.dta");
//        
//        StataDtaFileTask task = new StataDtaFileTask(rootDir, file, "file:data", null);
//        
//        task.run();
//        
//        Collection<FileItem> c = hub.getFiles();
//        
//        FileItem f = c.iterator().next();
//        
//        Collection<Item> set = f.getChildrenBy("variable:");
//        
//        Collection<Item> result = StatdocItemHub.groupMap(set, 3, 3, "test");
//        
//        System.out.println(set);
//        System.out.println(result);
//    }    
    
}
