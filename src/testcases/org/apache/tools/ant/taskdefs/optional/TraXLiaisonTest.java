package org.apache.tools.ant.taskdefs.optional;

import org.apache.tools.ant.taskdefs.XSLTLiaison;
import org.apache.tools.ant.taskdefs.XSLTLogger;
import org.apache.tools.ant.BuildException;

import java.io.File;

import junit.framework.AssertionFailedError;

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
 *
 */

/**
 * TraX XSLTLiaison testcase
 * @author <a href="mailto:sbailliez@apache.org">Stephane Bailliez</a>
 */
public class TraXLiaisonTest extends AbstractXSLTLiaisonTest 
    implements XSLTLogger {

    public TraXLiaisonTest(String name){
        super(name);
    }

    public void tearDown() {
        File f = new File("xalan2-redirect-out.tmp");
        if (f.exists()) {
            f.delete();
        }
    }

    public XSLTLiaison createLiaison() throws Exception {
        TraXLiaison l = new TraXLiaison();
        l.setLogger(this);
        return l;
    }

    public void testXalan2Redirect() throws Exception {
        File xsl = getFile("/taskdefs/optional/xalan-redirect-in.xsl");
        liaison.setStylesheet(xsl);
        File out = new File("xalan2-redirect-out-dummy.tmp");
        File in = getFile("/taskdefs/optional/xsltliaison-in.xsl");
        try {
            liaison.addParam("xalan-version", "2");
            liaison.transform(in, out);
        } finally {
            out.delete();
        }
    }

    public void testMultipleTransform() throws Exception {
        File xsl = getFile("/taskdefs/optional/xsltliaison-in.xsl");
        liaison.setStylesheet(xsl);
        liaison.addParam("param", "value");
        File in = getFile("/taskdefs/optional/xsltliaison-in.xml");
        // test for 10 consecutives transform
        for (int i = 0; i < 50; i++){
            File out = new File("xsltliaison" + i + ".tmp");
            try {
                liaison.transform(in, out);
            } catch (Exception e){
                throw new BuildException("failed in transform " + i, e);
            } finally {
                out.delete();
            }
        }
    }

    public void testSystemId(){
        File file = null;
        if ( File.separatorChar == '\\' ){
            file = new File("d:\\jdk");
        } else {
            file = new File("/user/local/bin");
        }
        String systemid = ((TraXLiaison)liaison).getSystemId(file);
        assertTrue("SystemIDs should start by file:///", systemid.startsWith("file:///"));
        assertTrue("SystemIDs should not start with file:////", !systemid.startsWith("file:////"));
    }

    public void log(String message) {
        throw new AssertionFailedError("Liaison sent message: "+message);
    }

}
