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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ThreadPoolExecutor;

import statdoc.items.Item;
import statdoc.items.StatdocItemHub;
import statdoc.items.TokenItem;
import statdoc.tasks.Task;
import statdoc.utils.TemplateUtil;

public class GenerateTokeninfoTask implements Task {

    File rootDir;
    ThreadPoolExecutor taskList;
    StatdocItemHub hub;
    
    public GenerateTokeninfoTask(File rootDir, StatdocItemHub hub, ThreadPoolExecutor taskList) {
        this.rootDir = rootDir;
        this.taskList = taskList;
        this.hub = hub;
    }

    @Override
    public void run() {
        Thread.currentThread().setName("Run " + this.getClass());

         TemplateUtil tu = TemplateUtil.getInstance();

        {
            Item tokens = new Item("tokens", "tokens", "tokens/tokens-summary.html", "tokens:summary");

            Map<String, Object> data = new TreeMap<String, Object>(
                    hub.getGlobals());
            TreeSet<TokenItem> ts = new TreeSet<TokenItem>();
            ts.addAll(hub.getTokens().values());

            TreeSet<TokenItem> freqtmp = new TreeSet<TokenItem>(
                    new Comparator<Item>() {

                        @Override
                        public int compare(Item o1, Item o2) {
                            int comp = o2.getChildren().size()
                                    - o1.getChildren().size();
                            return (comp != 0) ? comp : o1.getFullName()
                                    .compareTo(o2.getFullName());
                        }
                    });
            freqtmp.addAll(hub.getTokens().values());

            List<Item> freq = new ArrayList<Item>();
            int counter = 0;
            for (Item i : freqtmp) {
                if (counter++ > 10) {
                    break;
                }
                freq.add(i);
            }

            data.put("section", "tokens");
            data.put("tokens", ts);
            data.put("item", tokens);
            data.put("frequent", freq);
            File f = new File(rootDir, "tokens/tokens-summary.html");
            tu.evalVMtoFile(f, "tokens-summary.vm", data);

            File f2 = new File(rootDir, "tokens/tokens-frame.html");
            tu.evalVMtoFile(f2, "tokens-frame.vm", data);
        }

        Map<String, TokenItem> tokenMap = hub.getTokens();

        Map<String,Item> itemMap = new TreeMap<String,Item>();
        for (TokenItem item : tokenMap.values()) {
            String key = item.getGroup();
            if ( !itemMap.containsKey(key)) {
                itemMap.put(key, new Item(key,key,"tokens/" + key + ".html"));
            }
            itemMap.get(key).addChild(item);
        }

        /*
         * make into array
         */
        ArrayList<Item> arr = new ArrayList<Item>(itemMap.values());

        for (int i = 0; i < arr.size(); i++) {

            Item item = arr.get(i);
            Map<String, Object> data = new TreeMap<String, Object>(
                    hub.getGlobals());
            data.put("item", item);
            data.put("section", "tokens");
            if (i > 0) {
                data.put("prev", arr.get(i - 1));
            }
            if (i < (arr.size() - 1)) {
                data.put("next", arr.get(i + 1));
            }

            File f = new File(rootDir, item.getLink() );
            taskList.execute(new GeneralVMTask("token-item.vm", f, data));
        }

        Thread.currentThread().setName(
                "Thread " + Thread.currentThread().getId());
    }

}
