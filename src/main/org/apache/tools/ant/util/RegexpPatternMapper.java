/*
 * Copyright  2000,2002-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant.util;

import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.regexp.RegexpMatcher;
import org.apache.tools.ant.util.regexp.RegexpMatcherFactory;

/**
 * Implementation of FileNameMapper that does regular expression
 * replacements.
 *
 */
public class RegexpPatternMapper implements FileNameMapper {
    protected RegexpMatcher reg = null;
    protected char[] to = null;
    protected StringBuffer result = new StringBuffer();

    public RegexpPatternMapper() throws BuildException {
        reg = (new RegexpMatcherFactory()).newRegexpMatcher();
    }

    private boolean handleDirChar = false;
    private int     regexpOptions = 0;

    /**
     * Attribute specifing whether to ignore the difference
     * between / and \ (the two common directory characters).
     * @param handleDirChar a boolean, default is false.
     * @since Ant 1.6.3
     */
    public void setHandleDirChar(boolean handleDirChar) {
        this.handleDirChar = handleDirChar;
    }

    /**
     * Attribute specifing whether to ignore the case difference
     * in the names.
     *
     * @param caseSensitive a boolean, default is false.
     * @since Ant 1.6.3
     */
    public void setCaseSensitive(boolean caseSensitive) {
        if (!caseSensitive) {
            regexpOptions = RegexpMatcher.MATCH_CASE_INSENSITIVE;
        } else {
            regexpOptions = 0;
        }
    }

    /**
     * Sets the &quot;from&quot; pattern. Required.
     */
    public void setFrom(String from) throws BuildException {
        try {
            reg.setPattern(from);
        } catch (NoClassDefFoundError e) {
            // depending on the implementation the actual RE won't
            // get instantiated in the constructor.
            throw new BuildException("Cannot load regular expression matcher",
                                     e);
        }
    }

    /**
     * Sets the &quot;to&quot; pattern. Required.
     */
    public void setTo(String to) {
        this.to = to.toCharArray();
    }

    /**
     * Returns null if the source file name doesn't match the
     * &quot;from&quot; pattern, an one-element array containing the
     * translated file otherwise.
     */
    public String[] mapFileName(String sourceFileName) {
        if (handleDirChar) {
            if (sourceFileName.indexOf("\\") != -1) {
                sourceFileName = sourceFileName.replace('\\', '/');
            }
        }
        if (reg == null  || to == null
            || !reg.matches(sourceFileName, regexpOptions)) {
            return null;
        }
        return new String[] {replaceReferences(sourceFileName)};
    }

    /**
     * Replace all backreferences in the to pattern with the matched
     * groups of the source.
     */
    protected String replaceReferences(String source) {
        Vector v = reg.getGroups(source, regexpOptions);

        result.setLength(0);
        for (int i = 0; i < to.length; i++) {
            if (to[i] == '\\') {
                if (++i < to.length) {
                    int value = Character.digit(to[i], 10);
                    if (value > -1) {
                        result.append((String) v.elementAt(value));
                    } else {
                        result.append(to[i]);
                    }
                } else {
                    // XXX - should throw an exception instead?
                    result.append('\\');
                }
            } else {
                result.append(to[i]);
            }
        }
        return result.substring(0);
    }

}
