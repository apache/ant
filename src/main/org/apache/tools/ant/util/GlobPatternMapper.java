/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant.util;

import org.apache.tools.ant.BuildException;

/**
 * Implementation of FileNameMapper that does simple wildcard pattern
 * replacements.
 *
 * <p>This does simple translations like *.foo -&gt; *.bar where the
 * prefix to .foo will be left unchanged. It only handles a single *
 * character, use regular expressions for more complicated
 * situations.</p>
 *
 * <p>This is one of the more useful Mappers, it is used by javac for
 * example.</p>
 *
 */
public class GlobPatternMapper implements FileNameMapper {

    // CheckStyle:VisibilityModifier OFF - bc
    /**
     * Part of &quot;from&quot; pattern before the *.
     */
    protected String fromPrefix = null;

    /**
     * Part of &quot;from&quot; pattern after the *.
     */
    protected String fromPostfix = null;

    /**
     * Length of the prefix (&quot;from&quot; pattern).
     */
    protected int prefixLength;

    /**
     * Length of the postfix (&quot;from&quot; pattern).
     */
    protected int postfixLength;

    /**
     * Part of &quot;to&quot; pattern before the *.
     */
    protected String toPrefix = null;

    /**
     * Part of &quot;to&quot; pattern after the *.
     */
    protected String toPostfix = null;

    // CheckStyle:VisibilityModifier ON

    private boolean fromContainsStar = false;
    private boolean toContainsStar = false;
    private boolean handleDirSep = false;
    private boolean caseSensitive = true;

    /**
     * Attribute specifying whether to ignore the difference
     * between / and \ (the two common directory characters).
     * @param handleDirSep a boolean, default is false.
     * @since Ant 1.6.3
     */
    public void setHandleDirSep(boolean handleDirSep) {
        this.handleDirSep = handleDirSep;
    }

    /**
     * Attribute specifying whether to ignore the difference
     * between / and \ (the two common directory characters).
     * @return boolean
     * @since Ant 1.8.3
     */
    public boolean getHandleDirSep() {
        return handleDirSep;
    }

    /**
     * Attribute specifying whether to ignore the case difference
     * in the names.
     *
     * @param caseSensitive a boolean, default is false.
     * @since Ant 1.6.3
     */
    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    /**
     * Sets the &quot;from&quot; pattern. Required.
     * @param from a string
     */
    @Override
    public void setFrom(String from) {
        if (from == null) {
            throw new BuildException("this mapper requires a 'from' attribute");
        }
        int index = from.lastIndexOf('*');
        if (index < 0) {
            fromPrefix = from;
            fromPostfix = "";
        } else {
            fromPrefix = from.substring(0, index);
            fromPostfix = from.substring(index + 1);
            fromContainsStar = true;
        }
        prefixLength = fromPrefix.length();
        postfixLength = fromPostfix.length();
    }

    /**
     * Sets the &quot;to&quot; pattern. Required.
     * @param to a string
     */
    @Override
    public void setTo(String to) {
        if (to == null) {
            throw new BuildException("this mapper requires a 'to' attribute");
        }
        int index = to.lastIndexOf('*');
        if (index < 0) {
            toPrefix = to;
            toPostfix = "";
        } else {
            toPrefix = to.substring(0, index);
            toPostfix = to.substring(index + 1);
            toContainsStar = true;
        }
    }

    /**
     * Returns null if the source file name doesn't match the
     * &quot;from&quot; pattern, an one-element array containing the
     * translated file otherwise.
     * @param sourceFileName the filename to map
     * @return a list of converted filenames
     */
    @Override
    public String[] mapFileName(String sourceFileName) {
        if (sourceFileName == null) {
            return null;
        }
        String modName = modifyName(sourceFileName);
        if (fromPrefix == null
            || (sourceFileName.length() < (prefixLength + postfixLength))
            || (!fromContainsStar
                && !modName.equals(modifyName(fromPrefix))
                )
            || (fromContainsStar
                && (!modName.startsWith(modifyName(fromPrefix))
                    || !modName.endsWith(modifyName(fromPostfix)))
                )
            ) {
            return null;
        }
        return new String[] {toPrefix
                             + (toContainsStar
                                ? extractVariablePart(sourceFileName)
                                  + toPostfix
                                : "")};
    }

    /**
     * Returns the part of the given string that matches the * in the
     * &quot;from&quot; pattern.
     * @param name the source file name
     * @return the variable part of the name
     */
    protected String extractVariablePart(String name) {
        return name.substring(prefixLength,
                              name.length() - postfixLength);
    }

    /**
     * modify string based on dir char mapping and case sensitivity
     * @param name the name to convert
     * @return the converted name
     */
    private String modifyName(String name) {
        if (!caseSensitive) {
            name = name.toLowerCase();
        }
        if (handleDirSep) {
            if (name.contains("\\")) {
                name = name.replace('\\', '/');
            }
        }
        return name;
    }
}
