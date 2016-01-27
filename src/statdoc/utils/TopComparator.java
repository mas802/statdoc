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

import statdoc.items.Item;

public class TopComparator extends NaturalComparator {
    public static final TopComparator INSTANCE = new TopComparator();

    protected TopComparator() {
    }

    @Override
    public int compare(Item s1, Item s2) {
        int r = s2.getChildrenBy("match:").size() - s1.getChildrenBy("match:").size();

        if (r == 0) {
            r = super.compare(s1, s2);
        }

        return r;
    }

}
