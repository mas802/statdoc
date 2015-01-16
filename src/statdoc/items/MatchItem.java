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
package statdoc.items;

import java.util.Collection;
import java.util.TreeSet;

import statdoc.utils.NaturalComparator;

/**
 * A special Item to match up parts that might not have been resolved, such as
 * variables in script files where it is not yet known whether they reside in a
 * specific data file.
 * 
 * @author Markus Schaffner
 * 
 */
public class MatchItem extends Item {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private boolean resolved = false;

    public MatchItem(String name, String targetType) {
        this.put("targetType", targetType);
        this.type = "match:" + targetType;
        this.name = "match" + name + "_hash_" + super.hashCode();
        this.fullName = this.name;
    }

    public boolean isResolved() {
        return resolved;
    }

    public void setAsResolved() {
        resolved = true;
    }

    /**
     * gets the all children of a filtered subset of all children's children
     * 
     * only useful for second dependence of matched items
     * 
     * @param args
     *            the second level filter(s)
     * @return a filtered set of items
     */
    public final Collection<Item> getMatchChildrenBy(String... args) {
        Collection<Item> result = new TreeSet<Item>(NaturalComparator.INSTANCE);

        for (Item child : getChildren()) {
            for (Item link : child.getChildrenBy(args)) {
                result.addAll(link.getChildren());
            }

        }

        return result;
    }

}
