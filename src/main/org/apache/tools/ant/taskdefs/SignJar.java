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
package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.*;
import java.io.File;

/**
 * Sign a archive.
 * 
 * @author Peter Donald <a href="mailto:donaldp@mad.scientist.com">donaldp@mad.scientist.com</a>
 */
public class SignJar extends Task {

    /**
     * The name of the jar file.
     */
    protected String jar;

    /**
     * The alias of signer.
     */
    protected String alias;

    /**
     * The name of keystore file.
     */
    protected String keystore;
    protected String storepass;
    protected String storetype;
    protected String keypass;
    protected String sigfile;
    protected String signedjar;
    protected boolean verbose;
    protected boolean internalsf;
    protected boolean sectionsonly;

    public void setJar(final String jar) {
        this.jar = jar;
    } 

    public void setAlias(final String alias) {
        this.alias = alias;
    } 

    public void setKeystore(final String keystore) {
        this.keystore = keystore;
    } 

    public void setStorepass(final String storepass) {
        this.storepass = storepass;
    } 

    public void setStoretype(final String storetype) {
        this.storetype = storetype;
    } 

    public void setKeypass(final String keypass) {
        this.keypass = keypass;
    } 

    public void setSigfile(final String sigfile) {
        this.sigfile = sigfile;
    } 

    public void setSignedjar(final String signedjar) {
        this.signedjar = signedjar;
    } 

    public void setVerbose(final String verbose) {
        this.verbose = project.toBoolean(verbose);
    } 

    public void setInternalsf(final String internalsf) {
        this.internalsf = project.toBoolean(internalsf);
    } 

    public void setSectionsonly(final String sectionsonly) {
        this.sectionsonly = project.toBoolean(sectionsonly);
    } 

    public void execute() throws BuildException {
        if (project.getJavaVersion().equals(Project.JAVA_1_1)) {
            throw new BuildException("The signjar task is only available on JDK versions 1.2 or greater");
        } 

        if (null == jar) {
            throw new BuildException("jar attribute must be set");
        } 

        if (null == alias) {
            throw new BuildException("alias attribute must be set");
        } 

        if (null == storepass) {
            throw new BuildException("storepass attribute must be set");
        } 

        final StringBuffer sb = new StringBuffer();

        sb.append("jarsigner ");

        if (null != keystore) {
            sb.append("-keystore \"");
            sb.append(keystore);
            sb.append("\" ");
        } 

        if (null != storepass) {
            sb.append("-storepass \"");
            sb.append(storepass);
            sb.append("\" ");
        } 

        if (null != storetype) {
            sb.append("-storetype \"");
            sb.append(storetype);
            sb.append("\" ");
        } 

        if (null != keypass) {
            sb.append("-keypass \"");
            sb.append(keypass);
            sb.append("\" ");
        } 

        if (null != sigfile) {
            sb.append("-sigfile \"");
            sb.append(sigfile);
            sb.append("\" ");
        } 

        if (null != signedjar) {
            sb.append("-signedjar \"");
            sb.append(signedjar);
            sb.append("\" ");
        } 

        if (verbose) {
            sb.append("-verbose ");
        } 

        if (internalsf) {
            sb.append("-internalsf ");
        } 

        if (sectionsonly) {
            sb.append("-sectionsonly ");
        } 

        sb.append('\"');
        sb.append(jar);
        sb.append("\" ");
        
        sb.append('\"');
        sb.append(alias);
        sb.append("\" ");

        log("Signing Jar : " + (new File(jar)).getAbsolutePath());
        final Exec cmd = (Exec) project.createTask("exec");
        cmd.setCommand(sb.toString());
        cmd.setFailonerror("true");
        cmd.setTaskName( getTaskName() );
        cmd.execute();
    } 
}

