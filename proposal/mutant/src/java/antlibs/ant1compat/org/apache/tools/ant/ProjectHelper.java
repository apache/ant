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
package org.apache.tools.ant;
import java.util.Hashtable;
import java.util.Vector;
import java.io.File;
import org.apache.ant.common.antlib.AntContext;
import org.apache.ant.common.service.DataService;
import org.apache.ant.common.util.AntException;
import org.apache.ant.common.util.PropertyUtils;

/**
 * Ant1 ProjectHelper facade
 *
 * @author Conor MacNeill
 * @created 31 January 2002
 */
public class ProjectHelper {
    /**
     * This method will parse a string containing ${value} style property
     * values into two lists. The first list is a collection of text
     * fragments, while the other is a set of string property names null
     * entries in the first list indicate a property reference from the
     * second list.
     *
     * @param value the string to be parsed
     * @param fragments the fragments parsed out of the string
     * @param propertyRefs the property refs to be replaced
     */
    public static void parsePropertyString(String value, Vector fragments,
                                           Vector propertyRefs) {
        try {
            PropertyUtils.parsePropertyString(value, fragments, propertyRefs);
        } catch (AntException e) {
            throw new BuildException(e);
        }
    }

    /**
     * Replace ${} style constructions in the given value with the string
     * value of the corresponding data types.
     *
     * @param value the string to be scanned for property references.
     * @param project the project object which contains the property values
     * @return the string with the property references replaced with their
     *      project values
     * @exception BuildException if there is a problem replacing the
     *      property values.
     */
    public static String replaceProperties(Project project, String value)
         throws BuildException {
        try {
            AntContext context = project.getContext();
            DataService dataService
                 = (DataService) context.getCoreService(DataService.class);
            return dataService.replacePropertyRefs(value);
        } catch (AntException e) {
            throw new BuildException(e);
        }
    }

    /**
     * Replace ${} style constructions in the given value with the string
     * value of the corresponding data types.
     *
     * @param value the string to be scanned for property references.
     * @param project the project object
     * @param keys the collection of property values to use
     * @return the string with the property references replaced with their
     *      project values
     */
    public static String replaceProperties(Project project, String value,
                                           Hashtable keys) {
        try {
            AntContext context = project.getContext();
            DataService dataService
                 = (DataService) context.getCoreService(DataService.class);
            return dataService.replacePropertyRefs(value, keys);
        } catch (AntException e) {
            throw new BuildException(e);
        }
    }

    /**
     * Old method to build a project.
     *
     * @param project The project to configure. Must not be <code>null</code>.
     * @param buildFile An XML file giving the project's configuration.
     *                  Must not be <code>null</code>.
     *
     * @exception BuildException always
     * @deprecated
     */
    public static void configureProject(Project project, File buildFile)
         throws BuildException {
        project.configure(buildFile);
    }
}

