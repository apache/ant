/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
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
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
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
package org.apache.tools.ant.gui.core;

import org.apache.tools.ant.gui.acs.*;
import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * This class is responsible for managing the currently open projects,
 * and the loading of new projects.
 * 
 * @version $Revision$ 
 * @author Simeon Fitch 
 */
public class ProjectManager {

    /** Set of open projects. */
    private List _projects = new ArrayList(1);

    public ProjectManager() {
    }

    /** 
     * Get all the open projects.
     * 
     * @return an array of all open projects.
     */
    public ACSProjectElement[] getOpen() {
        ACSProjectElement[] retval = new ACSProjectElement[_projects.size()];
        _projects.toArray(retval);
        return retval;
    }

    /** 
     * Save the given project.
     * 
     * @param project Project to save.
     */
    public void save(ACSProjectElement project) throws IOException {
        saveAs(project, null);
    }


    /** 
     * Save the given project to the given location.
     * 
     * @param project Project to save.
     * @param location Location to save to.
     */
    public void saveAs(ACSProjectElement project, URL location) 
        throws IOException {

        if(location == null) {
            location = project.getLocation();
        }
        if(location == null) {
            // xxx Fix me.
            throw new IOException("xxx need a file name xxx");
        }
    }

    /** 
     * Open the project persisted at the given location
     * 
     * @param location  Location to save to.
     * @return Successfully loaded project.
     * @throws IOException thrown if there is a problem opening the project.
     */
    public ACSProjectElement open(URL location) throws IOException {
        ACSProjectElement retval = null;
        retval = ACSFactory.getInstance().load(location);
        return retval;
    }

    /** 
     * Create a new, unpopulated project.
     * 
     * @return Unpopulated project.
     */
    public ACSProjectElement createNew() {
        ACSProjectElement retval = null;
        return retval;
    }
}
