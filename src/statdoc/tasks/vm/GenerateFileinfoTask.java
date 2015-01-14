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
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ThreadPoolExecutor;

import statdoc.items.FileItem;
import statdoc.items.Item;
import statdoc.items.StatdocItemHub;
import statdoc.tasks.Task;
import statdoc.utils.TemplateUtil;

public class GenerateFileinfoTask implements Task {

    File rootDir;
    ThreadPoolExecutor taskList;
    StatdocItemHub hub;
    
    public GenerateFileinfoTask(File filesDir,  StatdocItemHub hub, ThreadPoolExecutor taskList) {
        this.rootDir = filesDir;
        this.taskList = taskList;
        this.hub = hub;
    }

    @Override
    public void run() {
	Thread.currentThread().setName("Run " + this.getClass() );
	
        TemplateUtil tu = TemplateUtil.getInstance();

        {
            // get an item with grouped children
            Item files = new Item("files", "files", "files/files-summary.html", "files:summary");
            for(Item item:hub.getFiles()) {
                files.addChild(item);
            }
            
            // produce files/files-summary.html
            Map<String, Object> data = new TreeMap<String, Object>(hub.getGlobals());
            data.put("files", hub.getFiles());
            data.put("typeMap", StatdocItemHub.getItemTypeMap( hub.getFiles() ));
            data.put("filesItem", files);
            data.put("item", files);
            data.put("section", "files");
            File f = new File(rootDir, "files/files-summary.html");
            tu.evalVMtoFile(f, "files-summary.vm", data);

            // produce files/package-list.html
            File f2 = new File(rootDir, "files/files-frame.html");
            tu.evalVMtoFile(f2, "files-frame.vm", data);
        }

        ArrayList<FileItem> arr = new ArrayList<FileItem>(hub.getFiles());
        for (int i = 0; i<arr.size(); i++) {
            FileItem fi = arr.get(i);
            
            Map<String, Object> data = new TreeMap<String, Object>(hub.getGlobals());
            data.put("item", fi);
            data.put("section", "files");
            if ( i>0 ) {
                data.put("prev", arr.get(i-1));
            }
            if ( i<(arr.size()-1) ) {
                data.put("next", arr.get(i+1));
            }
            
            if ( fi.getType().startsWith("file:data")) {
                Collection<Item> grouped = StatdocItemHub.groupMap( fi.getChildrenBy("variable:"), 3, 3, "group");
                data.put("groupedVars", grouped);
            }
            
            File f = new File(rootDir, fi.getLink() + "" );
            taskList.execute(new GeneralVMTask("file-item.vm", f, data));
        }

	Thread.currentThread().setName("Thread " + Thread.currentThread().getId() );
    }

}