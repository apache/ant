/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.taskdefs;

import java.util.Enumeration;
import java.util.Properties;
import java.io.File;
import java.io.FileInputStream;

import org.apache.tools.ant.*;

/**
 * This task sets a token filter that is used by the file copy methods
 * of the project to do token substitution, or sets mutiple tokens by
 * reading these from a file.
 *
 * @author Stefano Mazzocchi <a href="mailto:stefano@apache.org">stefano@apache.org</a>
 * @author Gero Vermaas <a href="mailto:gero@xs4all.nl">gero@xs4all.nl</a>
 */
public class Filter extends Task {

    private String token;
    private String value;
    private File filtersFile;
    
    public void setToken(String token) {
        this.token = token;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setFiltersfile(File filtersFile) {
        this.filtersFile = filtersFile;
    }

    public void execute() throws BuildException {
        boolean isFiltersFromFile = filtersFile != null && token == null && value == null;
        boolean isSingleFilter = filtersFile == null && token != null && value != null;
        
        if (!isFiltersFromFile && !isSingleFilter) {
            throw new BuildException("both token and value parameters, or only a filtersFile parameter is required", location);
        }
        
        if (isSingleFilter) {
            project.addFilter(token, value);
        }
        
        if (isFiltersFromFile) {
            readFilters();
        }
    }
    
    protected void readFilters() throws BuildException {
        log("Reading filters from " + filtersFile, Project.MSG_VERBOSE);
        FileInputStream in = null;
        try {
            Properties props = new Properties();
            in = new FileInputStream(filtersFile);
            props.load(in);

            Project proj = getProject();

            Enumeration enum = props.propertyNames();
            while (enum.hasMoreElements()) {
                String strPropName = (String)enum.nextElement();
                String strValue = props.getProperty(strPropName);
                proj.addFilter(strPropName, strValue);
            }
        } catch (Exception e) {
            throw new BuildException("Could not read filters from file: " + filtersFile);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (java.io.IOException ioex) {}
            }
        }
    }
}
