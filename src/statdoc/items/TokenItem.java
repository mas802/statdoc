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

public class TokenItem extends Item {
    
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    private String token;
    
    protected TokenItem( String token ) {
        this.token = token;
        this.link = 
        this.type = "token";
        
        this.name = token;
        this.fullName = token;
        
    }

    public String getClean() {
        String clean = token.replaceAll("[^a-zA-Z0-9-]", "_");
        return clean;
    }
 
    public String getGroup() {
        return getClean().substring(0,1).toLowerCase();
    }
    
    @Override
    public String getLink() {
        String link = "tokens/" + getGroup() + ".html#" + getClean();
        Collection<Item> items = getChildren();
        if ( items.size() == 1 ) {
            Item i = items.iterator().next();
            if ( !i.getType().startsWith("match")) {
                link = i.getLink();
            }
        } 
        return link;
    }
}