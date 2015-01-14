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
package statdoc.tasks;

import java.util.concurrent.ThreadPoolExecutor;

import statdoc.items.StatdocItemHub;

/**
 * filter out script files and start and appropriate parse task
 * 
 * @author Markus Schaffner
 * 
 */
public class MatchingTask implements Task {

    ThreadPoolExecutor taskList;
    StatdocItemHub hub;

    public MatchingTask(StatdocItemHub hub, ThreadPoolExecutor taskList) {
        this.hub = hub;
        this.taskList = taskList;
    }

    @Override
    public void run() {
        Thread.currentThread().setName("Run " + this.getClass());

        hub.resolveMatches();
 
        Thread.currentThread().setName(
                "Thread " + Thread.currentThread().getId());
    }

}