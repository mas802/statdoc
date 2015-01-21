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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import statdoc.utils.NaturalComparator;
import statdoc.utils.StatdocUtils;

/**
 * Item is the key class for all elements of analysis and provides a flexible
 * container for information about an item and its children/relatives. It
 * extends a TreeMap to hold arbitrary key value information and has a number of
 * fields to store information with relevance for sorting out the
 * interdependence between Items.
 * 
 * Subclass of Item should be lean and avoid to duplicate/Override methods that
 * could be handled more generally here. The are mainly Marker classes and
 * provide very specialized information.
 * 
 * @author Markus Schaffner
 * 
 */
public class Item extends TreeMap<String, Object> implements Comparable<Item> {

    private static final long serialVersionUID = 1L;

    // FIXME workaround for the issue that the
    // hashmap is hashed over the hash of its elements
    // this should work though for now
    private final Object hashObject = new Object();

    protected String name;
    protected String fullName;
    protected String link;
    protected String type;
    protected String summary;
    private String color = null;
    protected String description;
    protected String content;

    protected List<String> warnings = new ArrayList<String>();

    // avoid raw subclassing
    protected Item() {

    }

    public Item(String name, String fullName, String link) {
	this(name, fullName, link, "internal:none");
    }

    public Item(String name, String fullName, String link, String type) {
	this.name = name;
	this.fullName = fullName;
	this.link = link;
	this.type = type;
    }

    /*
     * GETTERS and SETTERS
     */

    public String getName() {
	return name;
    }

    public String getFullName() {
	return fullName;
    }

    public String getLink() {
	return link;
    }

    public String getType() {
	return type;
    }

    public String getSummary() {
	return summary;
    }

    public void setSummary(String summary) {
	this.summary = summary;
    }

    /*
     * warnings
     */

    public void addWarning(String htmlWarning) {
	warnings.add(htmlWarning);
    }

    public List<String> getWarnings() {
	return warnings;
    }

    /*
     * content
     */

    public boolean hasContent() {
	return (content != null);
    }

    public String getContent() {
	return content;
    }

    public void setContent(String content) {
	this.content = content;
    }

    public void setType(String type) {
	this.type = type;
    }

    public String getDescription() {
	return description;
    }

    public void setDescription(String description) {
	this.description = description;
    }

    /*
     * CODE TO HANDLE DISPLAY (outputs HTML)
     */

    private Map<String, String> matched = new HashMap<String, String>();

    /**
     * Return property as linked HTML with matches. This function returns the
     * requested property after all tokens that can be appropriately matched
     * have been replaced with an standard html link item.
     * 
     * @return A linked HTML string of the property requested.
     */
    public String getMatched(String property) {
	String matchToken = "" + (char) 17 + "";

	if (matched.containsKey(property + "_matched")) {
	    return matched.get(property + "_matched");
	} else {
	    String result = this.get(property).toString();

	    ArrayList<String> replace = new ArrayList<String>();
	    int counter = 0;

	    for (Item item : this.getChildrenBy("match:")) {
		MatchItem m = (MatchItem) item;

		if (m.containsKey("term")
			&& (!m.containsKey("field") || m.get("field").equals(
				property))) {
		    String term = m.get("term").toString().trim();

		    if (!term.equals("")) {
			term = StatdocUtils.stringToRegex(term);
			Collection<Item> links = m.getChildren();

			Pattern pattern = Pattern.compile("(?<=[\\W]|^)" + term
				+ "(?:(?=[\\W]|$)|(?<=\\())");

			if (links.size() == 1) {
			    String linkStr = "ERROR";
			    for (Item link : links) {
				linkStr = "<a href=\"../" + link.getLink()
					+ "\">" + term + "</a>";
			    }
			    result = pattern.matcher(result).replaceAll(
				    matchToken + counter + matchToken);
			    replace.add(linkStr);
			    counter++;
			} else if (links.size() > 1) {
			    // TODO this could be handled better/differently
			    String linkStr = "ERROR (m)";
			    String title = "<a title=\"multiple matches: ";
			    for (Item link : links) {
				// title = title + " " + link.getFullName();
				linkStr = " href=\"../" + link.getLink()
					+ "\">" + term + "</a>";
			    }
			    linkStr = title + "\"" + linkStr;
			    result = pattern.matcher(result).replaceAll(
				    matchToken + counter + matchToken);
			    replace.add(linkStr);
			    counter++;
			} else {
			    throw new RuntimeException("no children in " + m
				    + " " + m.get("term"));
			}
		    }
		}
	    }

	    for (int i = 0; i < replace.size(); i++) {
		result = result.replaceAll(matchToken + i + matchToken,
			replace.get(i));
	    }

	    matched.put(property + "_matched", result);

	    return result;
	}
    }

    /**
     * returns the name of the item abbreviated to length
     * 
     * @param length
     *            to abbreviate to
     * @return abbreviated name of the item
     */
    public String getAbbrevName(int length) {
	String result = getName();
	if (result.length() > length) {
	    if (length < 5) {
		result = result.substring(0, length);
	    } else {
		// 2/3 at the start
		int start = (2 * length) / 3;
		// make sure to capture a bit at the end
		int end = result.length() - (length - start - 2);
		result = result.substring(0, start) + ".."
			+ result.substring(end);
	    }
	}
	return result;
    }

    /**
     * returns the fullname of the item abbreviated to length
     * 
     * this method will try to preserve more of the end, rather than the
     * beginning as teh two related methods.
     * 
     * @param length
     *            to abbreviate to
     * @return abbreviated name of the item
     */
    public String getAbbrevFullName(int length) {
	String result = getFullName();
	if (result.length() > length) {
	    if (length < 5) {
		result = result.substring(0, length);
	    } else {
		// 1/3 at the start
		int start = (1 * length) / 3;
		// make sure to capture a bit at the end
		int end = result.length() - (length - start - 2);
		result = result.substring(0, start) + ".."
			+ result.substring(end);
	    }
	}
	return result;
    }

    /**
     * returns the property of the item abbreviated to length
     * 
     * @param length
     *            to abbreviate to
     * @return abbreviated property of the item
     */
    public String getAbbrevProperty(String key, int length) {
	String result = get(key).toString();
	if (result.length() > length) {
	    if (length < 5) {
		result = result.substring(0, length);
	    } else {
		// 2/3 at the start
		int start = (2 * length) / 3;
		// make sure to capture a bit at the end
		int end = result.length() - (length - start - 2);
		result = result.substring(0, start) + ".."
			+ result.substring(end);
	    }
	}
	return result;
    }

    /**
     * Retrieve the color of this class, the color might be calculated if
     * needed.
     * 
     * @return the color of this class
     */
    public String myColor() {
	if (this.color == null) {
	    this.color = calcColor();
	}
	return this.color;
    }

    /**
     * Calculate the color of this type of item
     * 
     * @return a color that can be used in a css style expression
     */
    public String calcColor() {
	String result = "#ffffff";

	if (type.startsWith("file:data")) {
	    result = "#eeeeff";
	} else if (type.startsWith("cmd:manipulate")) {
	    result = "#ffffff";
	} else if (type.startsWith("cmd:estcmd")) {
	    result = "#FFCC99";
	} else if (type.startsWith("cmd:statcmd")) {
	    result = "#FFFF99";
	} else if (type.startsWith("cmd:systemcmd")) {
	    result = "#eeeeef";
	} else if (type.startsWith("cmd:comment")) {
	    result = "#EFF4D7";
	} else if (type.startsWith("cmd:other")) {
	    result = "#ffcccc";
	} else if (type.startsWith("cmd:")) {
	    result = "#CCCCFF";
	} else if (type.startsWith("file:script")) {
	    // manipulate 255 255 255
	    // est FFCC99 255 201 153
	    // stat FFFF99 255 255 153

	    // here is where the action is
	    int cmanipulate = getChildrenBy("cmd:manipulate").size();
	    int cestcmd = getChildrenBy("cmd:estcmd").size();
	    int cstatcmd = getChildrenBy("cmd:statcmd").size();

	    int total = cmanipulate + cestcmd + cstatcmd;

	    int r = 238;
	    int b = 238;
	    int g = 238;
	    if (total > 0) {
		r = 255;
		g = 201 + ((cmanipulate + cstatcmd) * (255 - 201)) / total;
		b = 153 + (cmanipulate * (255 - 153)) / total;
	    }

	    result = "#" + Integer.toHexString(r) + Integer.toHexString(g)
		    + Integer.toHexString(b);
	} else if (type.startsWith("variable:")) {
	    // manipulate 255 255 255
	    // est FFCC99 255 201 153
	    // stat FFFF99 255 255 153

	    // here is where the action is
	    int cmanipulate = getChildrenBy("match:cmd:manipulate").size();
	    int cestcmd = getChildrenBy("match:cmd:estcmd").size();
	    int cstatcmd = getChildrenBy("match:cmd:statcmd").size();

	    int total = cmanipulate + cestcmd + cstatcmd;

	    int r = 238;
	    int g = 238;
	    int b = 255;
	    if (total > 0) {
		r = 255;
		g = 201 + ((cmanipulate + cstatcmd) * (255 - 201)) / total;
		b = 153 + (cmanipulate * (255 - 153)) / total;
	    }

	    result = "#" + Integer.toHexString(r) + Integer.toHexString(g)
		    + Integer.toHexString(b);
	}

	return result;
    }

    /*
     * CHILDREN (in a MVC pattern the access methods might be moved into the
     * hub)
     */

    /**
     * Children are links to other items, they can be set either as a link just
     * with the type or as a custom type of link (not sure yet if that is
     * useful). Sets of children can be obtained by specifying a number of
     * beginning of types/links
     * 
     */
    private Map<String, Collection<Item>> children = new TreeMap<String, Collection<Item>>();
    private Map<String, Collection<Item>> groupedChildren = new TreeMap<String, Collection<Item>>();
    private Collection<Item> childrenSet = new TreeSet<Item>();

    /* Determine whether children need to be regrouped */
    boolean dirtyChildren = true;

    public final void addChild(String link, Item child) {
	if (children.containsKey(link)) {
	    children.get(link).add(child);
	} else {
	    Collection<Item> ts = new TreeSet<Item>(NaturalComparator.INSTANCE);
	    ts.add(child);
	    children.put(link, ts);
	}
	dirtyChildren = true;
	childrenSet = null;
    }

    public final void addChild(Item child) {
	addChild(child.getType(), child);
    }

    synchronized public Collection<Item> getChildren() {
	synchronized (children) {
	    if (childrenSet == null) {
		childrenSet = new TreeSet<Item>(NaturalComparator.INSTANCE);
		for (Collection<Item> group : children.values()) {
		    childrenSet.addAll(group);
		}
	    }
	}
	return childrenSet;
    }

    /**
     * gets a collection of filtered children
     * 
     * @param args
     *            filter(s)
     * @return a filtered set of items
     */
    public final Collection<Item> getChildrenBy(String... args) {
	Collection<Item> result = new TreeSet<Item>(NaturalComparator.INSTANCE);
	for (String link : children.keySet()) {
	    for (String arg : args) {
		if (link.startsWith(arg)) {
		    result.addAll(children.get(link));
		}
	    }
	}
	return result;
    }

    /**
     * gets the filtered children of a subset of children
     * 
     * @param filter
     *            the first level filter (e.g. match:)
     * @param args
     *            the second level filter(s)
     * @return a filtered set of items
     */
    public final Collection<Item> getChildrenByBy(String filter, String... args) {
	Collection<Item> result = new TreeSet<Item>(NaturalComparator.INSTANCE);

	for (String link : children.keySet()) {
	    if (link.startsWith(filter)) {
		for (Item item : children.get(link)) {
		    result.addAll(item.getChildrenBy(args));
		}
	    }
	}
	return result;
    }

    /**
     * provides stacked result of filtered children
     * 
     * @param args
     *            filter the children
     * @return a collection with single and grouped children
     */
    public final Collection<Item> getGroupedChildrenBy(String... args) {
	if (dirtyChildren) {
	    groupedChildren.clear();
	    for (String link : children.keySet()) {
		Collection<Item> cs = children.get(link);
		Collection<Item> gc = StatdocItemHub.groupMap(cs, 3, 3,
			"group:" + link);
		groupedChildren.put(link, gc);
	    }
	    dirtyChildren = false;
	}
	Collection<Item> result = new TreeSet<Item>(NaturalComparator.INSTANCE);
	for (String link : groupedChildren.keySet()) {
	    for (String arg : args) {
		if (link.startsWith(arg)) {
		    result.addAll(groupedChildren.get(link));
		}
	    }
	}
	return result;
    }

    public Map<String, Collection<Item>> getChildrenMap() {
	return this.children;
    }

    /*
     * GUT CODE FOR COMPARE AND SUCH
     */

    @Override
    public String toString() {
	return this.getName();
    }

    @Override
    public int compareTo(Item item) {
	// TODO check the getFullName() is always unique
	return this.getFullName().compareTo(item.getFullName());
    }

    @Override
    public int hashCode() {
	return hashObject.hashCode() + super.hashCode();
    }

    /**
     * Unique creation of objects is handled by the code, might be changed at
     * some point though. Override the Map's equal method for now
     * 
     */
    @Override
    public boolean equals(Object obj) {
	return (this == obj);
    }

}
