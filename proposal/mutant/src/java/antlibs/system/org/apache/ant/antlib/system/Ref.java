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
package org.apache.ant.antlib.system;
import java.io.File;
import org.apache.ant.common.antlib.AntContext;
import org.apache.ant.common.model.Project;
import org.apache.ant.common.util.AntException;

/**
 * A Task to create a project reference.
 *
 * @author Conor MacNeill
 * @created 17 April 2002
 */
public class Ref extends SubBuild {

    /** The project file containing the project to be referenced. */
    private File projectFile;

    /** THe name under which this project is to be referenced. */
    private String name;

    /**
     * Initialise this task
     *
     * @param context core's context
     * @param componentType the component type of this component (i.e its
     *      defined name in the build file)
     * @exception AntException if we can't access the data service
     */
    public void init(AntContext context, String componentType)
         throws AntException {
        super.init(context, componentType);
    }


    /**
     * Sets the file containing the XML representation model of the referenced
     * project
     *
     * @param projectFile the file to build
     */
    public void setProject(File projectFile) {
        this.projectFile = projectFile;
    }


    /**
     * Set the name under which the project will be referenced
     *
     * @param name the reference label
     */
    public void setName(String name) {
        this.name = name;
    }


    /**
     * Create the project reference
     *
     * @exception AntException if the project cannot be referenced.
     */
    public void execute() throws AntException {
        Project model = getExecService().parseXMLBuildFile(projectFile);

        getExecService().createProjectReference(name, model, getProperties());
    }
}

