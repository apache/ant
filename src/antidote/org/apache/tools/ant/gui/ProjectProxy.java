/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999, 2000 The Apache Software Foundation.  All rights
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
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
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
package org.apache.tools.ant.gui;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import java.io.File;
import java.io.IOException;
import javax.swing.tree.TreeModel;
import javax.swing.text.Document;

/**
 * This class provides the gateway interface to the data model for
 * the application. The translation between the Ant datamodel, 
 * (or other external datamodel) occurs. This class also provides various
 * views into the data model, such as TreeModel, Documenet, etc.
 * 
 * @version $Revision$ 
 * @author Simeon Fitch 
 */
public class ProjectProxy {

    /** The file where the project was last saved. */
    private File _file = null;

    /** The real Ant Project instance. */
    private Project _project = null;

	/** 
	 * Default constructor. NB: right now it is private, but
     * will be opened up once the gui supports creating new projects.
	 * 
	 */
    private ProjectProxy() {
    }

	/** 
	 * File loading ctor.
	 * 
	 * @param file File containing build file to load.
	 */
    public ProjectProxy(File file) throws IOException {
        this();
        _file = file;
        loadProject();
    }


    private void loadProject() throws IOException {
        _project = new Project();
        _project.init();

        // XXX there is a bunch of stuff in the class org.apache.tools.ant.Main
        // that needs to be abstracted out so that it doesn't 
        // have to be replicated here.
        
        // XXX need to provide a way to pass in externally defined properties.
        // Perhaps define an external Antidote properties file.
        _project.setUserProperty("ant.file" , _file.getAbsolutePath());
        ProjectHelper.configureProject(_project, _file);
    }

	/** 
	 * Get the file where the project is saved to. If the project
     * is a new one that has never been saved the this will return null.
	 * 
	 * @return Project file, or null if not save yet.
	 */
    public File getFile() {
        return _file;
    }

	/** 
	 * Get the TreeModel perspective on the data.
	 * 
	 * @return TreeModel view on project.
	 */
    public TreeModel getTreeModel() {
        if(_project != null) {
            return new ProjectTreeModel(_project);
        }
        return null;
    }

	/** 
	 * Get the Document perspective on the data.
	 * 
	 * @return Document view on project.
	 */
    public Document getDocument() {
        if(_project != null) {
            // This is what the call should look like
            //return new ProjectDocument(_project);

            return new ProjectDocument(_file);
        }
        return null;
    }
}
