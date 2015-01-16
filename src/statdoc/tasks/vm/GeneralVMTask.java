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
import java.util.Map;

import statdoc.tasks.Task;
import statdoc.utils.TemplateUtil;

/**
 * A task to process a vm template.
 * 
 * @author Markus Schaffner
 * 
 */
public class GeneralVMTask implements Task {

    String template;
    File target;
    Map<String, Object> data;

    public GeneralVMTask(File target, String template, Map<String, Object> data) {
        this.template = template;
        this.target = target;
        this.data = data;
    }

    public GeneralVMTask(String template, File target, Map<String, Object> data) {
        this.template = template;
        this.target = target;
        this.data = data;
    }

    @Override
    public void run() {
        Thread.currentThread().setName(
                "Run " + this.getClass() + " f: " + target.getName() + " t: "
                        + template);

        TemplateUtil tu = TemplateUtil.getInstance();

        tu.evalVMtoFile(target, template, data);

        Thread.currentThread().setName(
                "Thread " + Thread.currentThread().getId());
    }

}