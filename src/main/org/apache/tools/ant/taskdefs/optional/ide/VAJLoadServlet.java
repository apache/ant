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

import java.util.Vector;

/**
 * A Remote Access to Tools Servlet to load a Project
 * from the Repository into the Workbench. The following 
 * table describes the servlet parameters.
 *
 * <table>
 *   <tr>
 *     <td>Parameter</td>
 *     <td>Description</td>
 *   </tr>
 *   <tr>
 *     <td>project</td>
 *     <td>The name of the Project you want to load into
 *         the Workbench.</td>
 *   </tr>
 *   <tr>
 *     <td>version</td>
 *     <td>The version of the package you want to load into
 *         the Workbench.</td>
 *   </tr>
 * </table>
 * 
 * @author Wolf Siberski, based on servlets written by Glenn McAllister
 */
public class VAJLoadServlet extends VAJToolsServlet {

    // constants for servlet param names
    public static final String VERSION_PARAM = "version";

    /**
     * Respond to a request to load a project from the Repository
     * into the Workbench.
     */
    protected void executeRequest() {
        String[] projectNames = getParamValues(PROJECT_NAME_PARAM);
        String[] versionNames = getParamValues(VERSION_PARAM);

        Vector projectDescriptions = new Vector(projectNames.length);
        for (int i = 0; i < projectNames.length && i < versionNames.length; i++) {
            VAJProjectDescription desc = new VAJProjectDescription();
            desc.setName(projectNames[i]);
            desc.setVersion(versionNames[i]);
            projectDescriptions.addElement(desc);
        }

        util.loadProjects(projectDescriptions);
    }
}
