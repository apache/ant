/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.taskdefs.optional.ejb;

import java.io.*;
import java.util.*;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.taskdefs.Java;

public class WeblogicDeploymentTool extends GenericDeploymentTool {
    protected static final String WL_DD     = "weblogic-ejb-jar.xml";
    protected static final String WL_CMP_DD = "weblogic-cmp-rdbms-jar.xml";
    protected static final String WL_DTD     = "/weblogic/ejb/deployment/xml/ejb-jar.dtd";
    protected static final String WL_HTTP_DTD     = "http://www.bea.com/servers/wls510/dtd/weblogic-ejb-jar.dtd";

    /** Instance variable that stores the suffix for the weblogic jarfile. */
    private String jarSuffix = ".jar";

    /** Instance variable that stores the location of the weblogic DTD file. */
    private String weblogicDTD;

    private Path classpath;

    /** Instance variable that determines whether generic ejb jars are kept. */
    private boolean keepGeneric = false;
    
    /**
     * Set the classpath to be used for this compilation.
     */
    public void setClasspath(Path classpath) {
        this.classpath = classpath;
    }

    /**
     * Setter used to store the suffix for the generated weblogic jar file.
     * @param inString the string to use as the suffix.
     */
    public void setSuffix(String inString) {
        this.jarSuffix = inString;
    }

    /**
     * Setter used to store the value of keepGeneric
     * @param inValue a string, either 'true' or 'false'.
     */
    public void setKeepgeneric(boolean inValue) {
        this.keepGeneric = inValue;
    }
    
    /**
     * Setter used to store the location of the weblogic DTD. This can be a file on the system 
     * or a resource on the classpath. 
     * @param inString the string to use as the DTD location.
     */
    public void setWeblogicdtd(String inString) {
        this.weblogicDTD = inString;
    }

    protected DescriptorHandler getDescriptorHandler(File srcDir) {
        DescriptorHandler handler = new DescriptorHandler(srcDir);
        if (weblogicDTD != null) {
            // is the DTD a local file?
            File dtdFile = new File(weblogicDTD);
            if (dtdFile.exists()) {
                handler.registerFileDTD("-//Sun Microsystems, Inc.//DTD Enterprise JavaBeans 1.1//EN",
                                        dtdFile);
            } else {
                handler.registerResourceDTD("-//Sun Microsystems, Inc.//DTD Enterprise JavaBeans 1.1//EN",
                                            weblogicDTD);
            }                                            
        } else {
            handler.registerResourceDTD("-//Sun Microsystems, Inc.//DTD Enterprise JavaBeans 1.1//EN",
                                        WL_DTD);
        }
        
        return handler;                                    
    }

    /**
     * Add any vendor specific files which should be included in the 
     * EJB Jar.
     */
    protected void addVendorFiles(Hashtable ejbFiles, String baseName) {
        String ddPrefix = (usingBaseJarName() ? "" : baseName + getBaseNameTerminator());

        File weblogicDD = new File(getDescriptorDir(), ddPrefix + WL_DD);

        if (weblogicDD.exists()) {
            ejbFiles.put(META_DIR + WL_DD,
                         weblogicDD);
        }

        // The the weblogic cmp deployment descriptor
        File weblogicCMPDD = new File(getDescriptorDir(), ddPrefix + WL_CMP_DD);

        if (weblogicCMPDD.exists()) {
            ejbFiles.put(META_DIR + WL_CMP_DD,
                         weblogicCMPDD);
        }
    }
    
    /**
     * Get the vendor specific name of the Jar that will be output. The modification date
     * of this jar will be checked against the dependent bean classes.
     */
    File getVendorOutputJarFile(String baseName) {
        return new File(getDestDir(), baseName + jarSuffix);
    }

    /**
     * Helper method invoked by execute() for each WebLogic jar to be built.
     * Encapsulates the logic of constructing a java task for calling
     * weblogic.ejbc and executing it.
     * @param sourceJar java.io.File representing the source (EJB1.1) jarfile.
     * @param destJar java.io.File representing the destination, WebLogic
     *        jarfile.
     */
    private void buildWeblogicJar(File sourceJar, File destJar) {
        org.apache.tools.ant.taskdefs.Java javaTask = null;
        
        try {
            String args = "-noexit " + sourceJar.getPath() + " " + destJar.getPath();
            
            javaTask = (Java) getTask().getProject().createTask("java");
            javaTask.setClassname("weblogic.ejbc");
            Commandline.Argument arguments = javaTask.createArg();
            arguments.setLine(args);
            if (classpath != null) {
                javaTask.setClasspath(classpath);
                javaTask.setFork(true);
            }
            else {
                javaTask.setFork(false);
            }
            

            getTask().log("Calling weblogic.ejbc for " + sourceJar.toString(),
                          Project.MSG_VERBOSE);

            javaTask.execute();
        }
        catch (Exception e) {
            // Have to catch this because of the semantics of calling main()
            String msg = "Exception while calling ejbc. Details: " + e.toString();
            throw new BuildException(msg, e);
        }
    }

    /**
     * Method used to encapsulate the writing of the JAR file. Iterates over the
     * filenames/java.io.Files in the Hashtable stored on the instance variable
     * ejbFiles.
     */
    protected void writeJar(String baseName, File jarFile, Hashtable files) throws BuildException {
        // need to create a generic jar first.
        File genericJarFile = super.getVendorOutputJarFile(baseName);
        super.writeJar(baseName, genericJarFile, files);
        
        buildWeblogicJar(genericJarFile, jarFile);
        if (!keepGeneric) {
             getTask().log("deleting generic jar " + genericJarFile.toString(),
                           Project.MSG_VERBOSE);
             genericJarFile.delete();
        }
    }

    /**
     * Called to validate that the tool parameters have been configured.
     *
     */
    public void validateConfigured() throws BuildException {
        super.validateConfigured();
    }
}
