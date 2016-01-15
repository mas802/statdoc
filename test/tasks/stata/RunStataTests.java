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
package tasks.stata;

import java.io.File;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import statdoc.items.FileItem;
import statdoc.items.StatdocItemHub;
import statdoc.tasks.stata.StataRunDoFileTask;

/**
 * test running Stata from Java
 * 
 * @author Markus Schaffner
 *
 */
@RunWith(JUnit4.class)
public class RunStataTests {

    /**
     * FIXME currently this test is incomplete
     */
    @Test
    public void testStatdocRunDo() {
        
        File file = new File("test/statdoc/tests/test_statdocrun.do");

        StatdocItemHub hub = new StatdocItemHub();
        hub.sourceDir = new File("tests/statdoc/tests").toPath();
        hub.outputDir = new File("tests/statdoc/tests").toPath();

        FileItem fileItem = hub.createFile(file.toPath(), "test");
        
        StataRunDoFileTask task = new StataRunDoFileTask(fileItem, hub, null);
        
        task.run();
        
        System.out.println(fileItem.getWarnings());
    }
    
}
