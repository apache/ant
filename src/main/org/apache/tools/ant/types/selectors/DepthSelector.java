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
import java.util.StringTokenizer;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Parameter;
import org.apache.tools.ant.BuildException;

/**
 * Selector that filters files based on the how deep in the directory
 * tree they are.
 *
 * @author <a href="mailto:bruce@callenish.com">Bruce Atherton</a>
 * @since 1.5
 */
public class DepthSelector extends BaseExtendSelector {

    public int min = -1;
    public int max = -1;
    public final static String MIN_KEY = "min";
    public final static String MAX_KEY = "max";

    public DepthSelector() {
    }

    public String toString() {
        StringBuffer buf = new StringBuffer("{depthselector min: ");
        buf.append(min);
        buf.append(" max: ");
        buf.append(max);
        buf.append("}");
        return buf.toString();
    }

    /**
     * The minimum depth below the basedir before a file is selected.
     *
     * @param min minimum directory levels below basedir to go
     */
    public void setMin(int min) {
        this.min = min;
    }

    /**
     * The minimum depth below the basedir before a file is selected.
     *
     * @param min maximum directory levels below basedir to go
     */
    public void setMax(int max) {
        this.max = max;
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
                if (MIN_KEY.equalsIgnoreCase(paramname)) {
                    try {
                        setMin(Integer.parseInt(parameters[i].getValue()));
                    }
                    catch (NumberFormatException nfe1) {
                        setError("Invalid minimum value "
                            + parameters[i].getValue());
                    }
                }
                else if (MAX_KEY.equalsIgnoreCase(paramname)) {
                    try {
                        setMax(Integer.parseInt(parameters[i].getValue()));
                    }
                    catch (NumberFormatException nfe1) {
                        setError("Invalid maximum value "
                            + parameters[i].getValue());
                    }
                }
                else {
                    setError("Invalid parameter " + paramname);
                }
            }
        }
    }

    /**
     * Checks to make sure all settings are kosher. In this case, it
     * means that the max depth is not lower than the min depth.
     */
    public void verifySettings() {
        if (min < 0 && max < 0) {
            setError("You must set at least one of the min or the " +
                    "max levels.");
        }
        if (max < min && max > -1) {
            setError("The maximum depth is lower than the minimum.");
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

        // throw BuildException on error
        validate();

        int depth = -1;
        // If you felt daring, you could cache the basedir absolute path
        String abs_base = basedir.getAbsolutePath();
        String abs_file = file.getAbsolutePath();
        StringTokenizer tok_base = new StringTokenizer(abs_base, File.separator);
        StringTokenizer tok_file = new StringTokenizer(abs_file, File.separator);
        while (tok_file.hasMoreTokens()) {
            String filetoken = tok_file.nextToken();
            if (tok_base.hasMoreTokens()) {
                String basetoken = tok_base.nextToken();
                // Sanity check. Ditch it if you want faster performance
                if (!basetoken.equals(filetoken)) {
                    throw new BuildException("File " + filename +
                        " does not appear within " + abs_base + "directory");
                }
            }
            else {
                depth += 1;
                if (max > -1 && depth > max) {
                    return false;
                }
            }
        }
        if (tok_base.hasMoreTokens()) {
            throw new BuildException("File " + filename +
                " is outside of " + abs_base + "directory tree");
        }
        if (min > -1 && depth < min) {
            return false;
        }
        return true;
    }

}

