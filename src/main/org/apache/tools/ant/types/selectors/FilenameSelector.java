/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Ant" and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package org.apache.tools.ant.types.selectors;

import java.io.File;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Parameter;

/**
 * Selector that filters files based on the filename.
 *
 * @author <a href="mailto:bruce@callenish.com">Bruce Atherton</a>
 * @since 1.5
 */
public class FilenameSelector extends BaseExtendSelector {

    private String pattern = null;
    private boolean casesensitive = true;
    private boolean negated = false;
    public final static String NAME_KEY = "name";
    public final static String CASE_KEY = "casesensitive";
    public final static String NEGATE_KEY = "negate";

    public FilenameSelector() {
    }

    public String toString() {
        StringBuffer buf = new StringBuffer("{filenameselector name: ");
        buf.append(pattern);
        buf.append(" negate: ");
        if (negated) {
            buf.append("true");
        } else {
            buf.append("false");
        }
        buf.append(" casesensitive: ");
        if (casesensitive) {
            buf.append("true");
        } else {
            buf.append("false");
        }
        buf.append("}");
        return buf.toString();
    }

    /**
     * The name of the file, or the pattern for the name, that
     * should be used for selection.
     *
     * @param pattern the file pattern that any filename must match
     *                against in order to be selected.
     */
    public void setName(String pattern) {
        pattern = pattern.replace('/',File.separatorChar).replace('\\',
                File.separatorChar);
        if (pattern.endsWith(File.separator)) {
            pattern += "**";
        }
        this.pattern = pattern;
    }

    /**
     * Whether to ignore case when checking filenames.
     *
     * @param casesensitive whether to pay attention to case sensitivity
     */
    public void setCasesensitive(boolean casesensitive) {
        this.casesensitive = casesensitive;
    }

    /**
     * You can optionally reverse the selection of this selector,
     * thereby emulating an &lt;exclude&gt; tag, by setting the attribute
     * negate to true. This is identical to surrounding the selector
     * with &lt;not&gt;&lt;/not&gt;.
     *
     * @param negated whether to negate this selection
     */
    public void setNegate(boolean negated) {
        this.negated = negated;
    }

    /**
     * When using this as a custom selector, this method will be called.
     * It translates each parameter into the appropriate setXXX() call.
     *
     * @param parameters the complete set of parameters for this selector
     */
    public void setParameters(Parameter[] parameters) {
        super.setParameters(parameters);
        if (parameters != null) {
            for (int i = 0; i < parameters.length; i++) {
                String paramname = parameters[i].getName();
                if (NAME_KEY.equalsIgnoreCase(paramname)) {
                    setName(parameters[i].getValue());
                }
                else if (CASE_KEY.equalsIgnoreCase(paramname)) {
                    setCasesensitive(Project.toBoolean(
                        parameters[i].getValue()));
                }
                else if (NEGATE_KEY.equalsIgnoreCase(paramname)) {
                    setNegate(Project.toBoolean(parameters[i].getValue()));
                }
                else {
                    setError("Invalid parameter " + paramname);
                }
            }
        }
    }

    /**
     * Checks to make sure all settings are kosher. In this case, it
     * means that the name attribute has been set.
     *
     */
    public void verifySettings() {
        if (pattern == null) {
            setError("The name attribute is required");
        }
    }

    /**
     * The heart of the matter. This is where the selector gets to decide
     * on the inclusion of a file in a particular fileset. Most of the work
     * for this selector is offloaded into SelectorUtils, a static class
     * that provides the same services for both FilenameSelector and
     * DirectoryScanner.
     *
     * @param basedir the base directory the scan is being done from
     * @param filename is the name of the file to check
     * @param file is a java.io.File object the selector can use
     * @return whether the file should be selected or not
     */
    public boolean isSelected(File basedir, String filename, File file) {
        validate();

        return (SelectorUtils.matchPath(pattern,filename,
                casesensitive) == !(negated));
    }

}

