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

import java.nio.file.Path;


/**
 * An Item to hold a link to a file.
 * 
 * @author Markus Schaffner
 * 
 */
public class FileItem extends Item {

    private static final long serialVersionUID = 1L;

    private Path file;
    private Path rootDir;

    protected FileItem(Path file, Path rootDir, String type) {
        
        this.file = file;
        this.rootDir = rootDir;
        this.type = type;
        this.name = file.getFileName().toString();
        this.link = "files/"
                + rootDir.relativize(file).toString()
                        .replaceAll("[\\\\\\/]", "_") + ".html";
        this.fullName = rootDir.relativize(file).toString();
        ;
    }

    /**
     * returns the underlying noi path. 
     * 
     * @return the File object for this item
     */
    public Path getPath() {
        return file;
    }
    
    /**
     * Get the link to the original file on the file system.
     * 
     * @return a relative or absolute path
     */
    public String getFileLink() {
        return rootDir.relativize(file).toString();
    }

}