/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2003 The Apache Software Foundation.  All rights
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
import org.apache.tools.ant.BuildException;

/**
 * Load project versions into the Visual Age for Java workspace.
 * Each project is identified by its name and a version qualifier.
 * Allowed qualifiers are:
 * <ul>
 * <li>Any valid Visual Age version name</li>
 * <li>* (loads the latest <b>versioned</b> edition)</li>
 * <li>** (loads the latest edition, including open editions)</li>
 * </ul>
 * Example:
 * <blockquote>
 * &lt;vajload&gt;
 * &nbsp;&lt;project name=&quot;MyVAProject&quot; version=&quot;*&quot;/&gt;
 * &nbsp;&lt;project name=&quot;Apache Xerces&quot; version=&quot;1.2.0&quot;/&gt;
 * &nbsp;&lt;project name=&quot;Brand New Stuff&quot; version=&quot;**&quot;/&gt;
 * &lt;/vajload&gt;
 * </blockquote>
 *
 * <p>Parameters:</p>
 * <table border="1" cellpadding="2" cellspacing="0">
 * <tr>
 *   <td valign="top"><b>Attribute</b></td>
 *   <td valign="top"><b>Description</b></td>
 *   <td align="center" valign="top"><b>Required</b></td>
 * </tr>
 * <tr>
 *   <td valign="top">remote</td>
 *   <td valign="top">remote tool server to run this command against
 *                    (format: &lt;servername&gt; : &lt;port no&gt;)</td>
 *   <td align="center" valign="top">No</td>
 * </tr>
 * <tr>
 *   <td valign="top">haltonerror</td>
 *   <td valign="top">stop the build process if an error occurs,
 *                      defaults to "yes"</td>
 *   <td align="center" valign="top">No</td>
 * </tr>
 * </table>
 * </p>
 *
 * @author Wolf Siberski, TUI Infotec GmbH
 * @author Martin Landers, Beck et al. projects
 */

public class VAJLoad extends VAJTask {
    Vector projectDescriptions = new Vector();

    /**
     * Load specified projects.
     */
    public void execute() {
        try {
            getUtil().loadProjects(projectDescriptions);
        } catch (BuildException ex) {
            if (haltOnError) {
                throw ex;
            } else {
                log(ex.toString());
            }
        }
    }

    /**
     * Add a project description entry on the project list.
     */
    public VAJProjectDescription createVAJProject() {
        VAJProjectDescription d = new VAJProjectDescription();
        projectDescriptions.addElement(d);
        return d;
    }
}
