/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional.ide;

import java.io.File;

/**
 * A Remote Access to Tools Servlet to extract package sets from the Workbench
 * to the local file system. The following table describes the servlet
 * parameters.
 * <tableborder="1">
 *
 *   <tr>
 *
 *     <td>
 *       <strong>Parameter</strong>
 *     </td>
 *
 *     <td>
 *       <strong>Values</strong>
 *     </td>
 *
 *     <td>
 *       <strong>Description</strong>
 *     </td>
 *
 *   </tr>
 *
 *   <tr>
 *
 *     <td>
 *       dir
 *     </td>
 *
 *     <td>
 *       Any valid directory name on the server.
 *     </td>
 *
 *     <td>
 *       The directory to export the files to on the machine where the servlet
 *       is being run. If the directory doesn't exist, it will be created.<p>
 *
 *       Relative paths are relative to IBMVJava/ide/tools/com-ibm-ivj-toolserver,
 *       where IBMVJava is the VisualAge for Java installation directory.
 *     </td>
 *
 *   </tr>
 *
 *   <tr>
 *
 *     <td>
 *       include
 *     </td>
 *
 *     <td>
 *       See below.
 *     </td>
 *
 *     <td>
 *       The pattern used to indicate which projects and packages to export.
 *
 *     </td>
 *
 *   </tr>
 *
 *   <tr>
 *
 *     <td>
 *       exclude
 *     </td>
 *
 *     <td>
 *       See below
 *     </td>
 *
 *     <td>
 *       The pattern used to indicate which projects and packages <em>not</em>
 *       to export.
 *     </td>
 *
 *   </tr>
 *
 *   <tr>
 *
 *     <td>
 *       cls
 *     </td>
 *
 *     <td>
 *       "yes" or "no" (without the quotes)
 *     </td>
 *
 *     <td>
 *       Export class files. Defaults to "no".
 *     </td>
 *
 *   </tr>
 *
 *   <tr>
 *
 *     <td>
 *       src
 *     </td>
 *
 *     <td>
 *       "yes" or "no" (without the quotes)
 *     </td>
 *
 *     <td>
 *       Export source files. Defaults to "yes".
 *     </td>
 *
 *   </tr>
 *
 *   <tr>
 *
 *     <td>
 *       res
 *     </td>
 *
 *     <td>
 *       "yes" or "no" (without the quotes)
 *     </td>
 *
 *     <td>
 *       Export resource files associated with the included project(s). Defaults
 *       to "yes".
 *     </td>
 *
 *   </tr>
 *
 *   <tr>
 *
 *     <td>
 *       dex
 *     </td>
 *
 *     <td>
 *       "yes" or "no" (without the quotes)
 *     </td>
 *
 *     <td>
 *       Use the default exclusion patterns. Defaults to "yes". See below for an
 *       explanation of default excludes.
 *     </td>
 *
 *   </tr>
 *
 *   <tr>
 *
 *     <td>
 *       owr
 *     </td>
 *
 *     <td>
 *       "yes" or "no" (without the quotes)
 *     </td>
 *
 *     <td>
 *       Overwrite any existing files. Defaults to "yes".
 *     </td>
 *
 *   </tr>
 *
 * </table>
 * <p>
 *
 * The vajexport servlet uses include and exclude parameters to form the
 * criteria for selecting packages to export. The parameter is broken up into
 * ProjectName/packageNameSegments, where ProjectName is what you expect, and
 * packageNameSegments is a partial (or complete) package name, separated by
 * forward slashes, rather than periods. Each packageNameSegment can have
 * wildcard characters.</p>
 * <tableborder="1">
 *
 *   <tr>
 *
 *     <td>
 *       <strong>Wildcard Characters</strong>
 *     </td>
 *
 *     <td>
 *       <strong>Description</strong>
 *     </td>
 *
 *   </tr>
 *
 *   <tr>
 *
 *     <td>
 *       *
 *     </td>
 *
 *     <td>
 *       Match zero or more characters in that segment.
 *     </td>
 *
 *   </tr>
 *
 *   <tr>
 *
 *     <td>
 *       ?
 *     </td>
 *
 *     <td>
 *       Match one character in that segment.
 *     </td>
 *
 *   </tr>
 *
 *   <tr>
 *
 *     <td>
 *       **
 *     </td>
 *
 *     <td>
 *       Matches all characters in zero or more segments.
 *     </td>
 *
 *   </tr>
 *
 * </table>
 *
 *
 * @author Wolf Siberski, based on servlets written by Glenn McAllister
 */
public class VAJExportServlet extends VAJToolsServlet
{
    // constants for servlet param names
    public final static String WITH_DEBUG_INFO = "deb";
    public final static String OVERWRITE_PARAM = "owr";

    /**
     * Respond to a request to export packages from the Workbench.
     */
    protected void executeRequest()
    {
        getUtil().exportPackages(
            new File( getFirstParamValueString( DIR_PARAM ) ),
            getParamValues( INCLUDE_PARAM ),
            getParamValues( EXCLUDE_PARAM ),
            getBooleanParam( CLASSES_PARAM, false ),
            getBooleanParam( WITH_DEBUG_INFO, false ),
            getBooleanParam( RESOURCES_PARAM, true ),
            getBooleanParam( SOURCES_PARAM, true ),
            getBooleanParam( DEFAULT_EXCLUDES_PARAM, true ),
            getBooleanParam( OVERWRITE_PARAM, true )
        );
    }
}
