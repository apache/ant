/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.taskdefs.optional.ide;


import java.io.File;

/**
 * A Remote Access to Tools Servlet to extract package
 * sets from the Workbench to the local file system.
 * The following table describes the servlet parameters.
 *
 * <table border="1">
 *   <tr>
 *     <td><strong>Parameter</strong></td>
 *     <td><strong>Values</strong></td>
 *     <td><strong>Description</strong></td>
 *   </tr>
 *   <tr>
 *     <td>dir</td>
 *     <td>Any valid directory name on the server.</td>
 *     <td>The directory to export the files to on the machine 
 *         where the servlet is being run.  If the directory 
 *         doesn't exist, it will be created.<p>
 *         Relative paths are relative to 
 *         IBMVJava/ide/tools/com-ibm-ivj-toolserver, 
 *         where IBMVJava is the VisualAge for Java installation 
 *         directory.</td>
 *   </tr>
 *   <tr>
 *     <td>include</td>
 *     <td>See below.</td>
 *     <td>The pattern used to indicate which projects and 
 *         packages to export.</td>
 *   </tr>
 *   <tr>
 *     <td>exclude</td>
 *     <td>See below</td>
 *     <td>The pattern used to indicate which projects and 
 *         packages <em>not</em> to export.</td>
 *   </tr>
 *   <tr>
 *     <td>cls</td>
 *     <td>"yes" or "no" (without the quotes)</td>
 *     <td>Export class files.  Defaults to "no".</td>
 *   </tr>
 *   <tr>
 *     <td>src</td>
 *     <td>"yes" or "no" (without the quotes)</td>
 *     <td>Export source files.  Defaults to "yes".</td>
 *   </tr>
 *   <tr>
 *     <td>res</td>
 *     <td>"yes" or "no" (without the quotes)</td>
 *     <td>Export resource files associated with the included project(s).
 *         Defaults to "yes".</td>
 *   </tr>
 *   <tr>
 *     <td>dex</td>
 *     <td>"yes" or "no" (without the quotes)</td>
 *     <td>Use the default exclusion patterns.  Defaults to "yes".  
 *         See below for an explanation of default excludes.</td>
 *   </tr>
 *   <tr>
 *     <td>owr</td>
 *     <td>"yes" or "no" (without the quotes)</td>
 *     <td>Overwrite any existing files.  Defaults to "yes".</td>
 *   </tr>
 * </table>
 *
 * <p>The vajexport servlet uses include and exclude parameters to form 
 *    the criteria for selecting packages to export. The parameter is 
 *    broken up into ProjectName/packageNameSegments, where ProjectName 
 *    is what you expect, and packageNameSegments is a partial (or complete) 
 *    package name, separated by forward slashes, rather than periods.  
 *    Each packageNameSegment can have wildcard characters.</p>
 *
 * <table border="1">
 *   <tr>
 *     <td><strong>Wildcard Characters</strong></td>
 *     <td><strong>Description</strong></td>
 *   </tr>
 *   <tr>
 *     <td>*</td>
 *     <td>Match zero or more characters in that segment.</td>
 *   </tr>
 *   <tr>
 *     <td>?</td>
 *     <td>Match one character in that segment.</td>
 *   </tr>
 *   <tr>
 *     <td>**</td>
 *     <td>Matches all characters in zero or more segments.</td>
 *   </tr>
 * </table>
 *
 * @author Wolf Siberski, based on servlets written by Glenn McAllister
 */
public class VAJExportServlet extends VAJToolsServlet {
    // constants for servlet param names
    public static final String WITH_DEBUG_INFO = "deb";
    public static final String OVERWRITE_PARAM = "owr";

    /**
     * Respond to a request to export packages from the Workbench.
     */
    protected void executeRequest() {
        getUtil().exportPackages(
                         new File(getFirstParamValueString(DIR_PARAM)),
                         getParamValues(INCLUDE_PARAM),
                         getParamValues(EXCLUDE_PARAM),
                         getBooleanParam(CLASSES_PARAM, false),
                         getBooleanParam(WITH_DEBUG_INFO, false),
                         getBooleanParam(RESOURCES_PARAM, true),
                         getBooleanParam(SOURCES_PARAM, true),
                         getBooleanParam(DEFAULT_EXCLUDES_PARAM, true),
                         getBooleanParam(OVERWRITE_PARAM, true));
    }
}
