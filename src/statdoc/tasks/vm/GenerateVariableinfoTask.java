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
package statdoc.tasks.vm;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ThreadPoolExecutor;

import statdoc.items.Item;
import statdoc.items.StatdocItemHub;
import statdoc.tasks.Task;
import statdoc.utils.TemplateUtil;

public class GenerateVariableinfoTask implements Task {

    // File variableDir;
    ThreadPoolExecutor taskList;
    
    public GenerateVariableinfoTask( ThreadPoolExecutor taskList) {
        // this.variableDir = dir;
        this.taskList = taskList;
    }

    @Override
    public void run() {
	Thread.currentThread().setName("Run " + this.getClass() );
	
        StatdocItemHub itemHolder = StatdocItemHub.getInstance();

        File variableDir = new File( itemHolder.outputDir, "/variables" );
        
        TemplateUtil tu = TemplateUtil.getInstance();

        {
            Item vars = new Item("variables", "variables", "variables/variables-summary.html", "variables:summary");
            
            // produce variables/variables-summary.html
            Map<String, Object> data = new TreeMap<String, Object>(itemHolder.getGlobals());
            TreeSet<String> ts = new TreeSet<String>();
            ts.addAll(itemHolder.getVariables().keySet());
            data.put("section", "variables");
            data.put("varNames", ts);
            data.put("item", vars);
            data.put("variables", itemHolder.getVariables().values());
            File f = new File(variableDir, "variables-summary.html");
            tu.evalVMtoFile(f, "variables-summary.vm", data);

            // produce variables/variables-frame.html
            File f2 = new File(variableDir, "variables-frame.html");
            tu.evalVMtoFile(f2, "variables-frame.vm", data);
        }
        
        Map<String,Item> varMap = itemHolder.getVariables();
        
        ArrayList<String> arr = new ArrayList<String>( itemHolder.getVariables().keySet() );
        for (int i = 0; i<arr.size(); i++) {
            String var = arr.get(i);

            Item varGroup = varMap.get(var); 
            
            // produce files/[file].html
            Map<String, Object> data = new TreeMap<String, Object>(itemHolder.getGlobals());
            data.put("section", "variables");
            data.put("item", varGroup);
            if ( i>0 ) {
                data.put("prev", varMap.get(arr.get(i-1)));
            }
            if ( i<(arr.size()-1) ) {
                data.put("next", varMap.get(arr.get(i+1)));
            }

            File f = new File(variableDir, var + ".html" );

            taskList.execute(new GeneralVMTask("variable-item.vm", f, data));
        }

	Thread.currentThread().setName("Thread " + Thread.currentThread().getId() );
    }

}
