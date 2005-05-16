/** (C) Copyright 2005 Hewlett-Packard Development Company, LP

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.

 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

 For more information: www.smartfrog.org

 */

package org.apache.tools.ant.types.optional;

import org.apache.tools.ant.util.FileNameMapper;
import org.apache.tools.ant.util.ScriptRunner;
import org.apache.tools.ant.ProjectComponent;

import java.util.ArrayList;

/**
 * Script support at map time. 
 * @since Ant1.7
 */
public class ScriptMapper extends AbstractScriptComponent implements FileNameMapper {


    ArrayList files;
    static final String[] EMPTY_STRING_ARRAY = new String[0];


    /**
     * Sets the from part of the transformation rule.
     *
     * @param from a string.
     */
    public void setFrom(String from) {

    }

    /**
     * Sets the to part of the transformation rule.
     *
     * @param to a string.
     */
    public void setTo(String to) {

    }

    /**
     * Reset the list of files
     */
    public void clear() {
        files=new ArrayList(1);
    }

    /**
     * Add a mapped name
     * @param mapping
     */
    public void addMappedName(String mapping) {
        files.add(mapping);
    }

    /**
     * Returns an array containing the target filename(s) for the given source
     * file.
     * <p/>
     * <p>if the given rule doesn't apply to the source file, implementation
     * must return null. SourceFileScanner will then omit the source file in
     * question.</p>
     *
     * @param sourceFileName the name of the source file relative to some given
     *                       basedirectory.
     * @return an array of strings if the rule applies to the source file, or
     *         null if it does not.
     */

    public String[] mapFileName(String sourceFileName) {
        initScriptRunner();
        getRunner().addBean("source", sourceFileName);
        clear();
        executeScript("ant_mapper");
        if(files.size()==0) {
            return null;
        } else {
            return (String[])files.toArray(EMPTY_STRING_ARRAY);
        }
    }
}
