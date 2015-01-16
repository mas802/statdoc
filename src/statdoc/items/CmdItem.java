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

/**
 * An Item representing a line in a script file.
 * 
 * @author Markus Schaffner
 * 
 */
public class CmdItem extends Item {

    private static final long serialVersionUID = 1L;

    private String command;
    private FileItem fileItem;
    private Integer[] range;

    public CmdItem(String line, String command, FileItem fileItem,
            Integer[] range) {
        this(line, command, "cmd:default", fileItem, range);
    }

    public CmdItem(String line, String command, String type, FileItem fileItem,
            Integer[] range) {
        this.command = command;
        this.range = new Integer[] { range[0], range[1] };

        this.content = line;
        this.type = type;
        this.name = fileItem.getFullName() + ":" + getLine();
        this.fullName = fileItem.getFullName() + ":" + getLine() + " ("
                + getCommand() + ")";
        this.link = fileItem.getLink() + "#" + getLine();
        this.fileItem = fileItem;
    }

    public String getCommand() {
        return command;
    }

    public String getLine() {
        return "" + range[0];
    }

    public FileItem getFileItem() {
        return this.fileItem;
    }

}