/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2002 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.taskdefs.optional;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.XSLTLiaison;

import org.apache.xalan.xslt.XSLTProcessorFactory;
import org.apache.xalan.xslt.XSLTProcessor;
import org.apache.xalan.xslt.XSLTInputSource;
import org.apache.xalan.xslt.XSLTResultTarget;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Concrete liaison for Xalan 1.x API.
 *
 * @author <a href="mailto:rubys@us.ibm.com">Sam Ruby</a>
 * @author <a href="mailto:sbailliez@apache.org">Stephane Bailliez</a>
 * @since Ant 1.1
 */
public class XalanLiaison implements XSLTLiaison {

    protected XSLTProcessor processor;
    protected File stylesheet;

    public XalanLiaison() throws Exception {
        processor = XSLTProcessorFactory.getProcessor();
    }

    public void setStylesheet(File stylesheet) throws Exception {
        this.stylesheet = stylesheet;
    }

    public void transform(File infile, File outfile) throws Exception {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        FileInputStream xslStream = null;
        try {
            xslStream = new FileInputStream(stylesheet);
            fis = new FileInputStream(infile);
            fos = new FileOutputStream(outfile);
            // systemid such as file:/// + getAbsolutePath() are considered
            // invalid here...
            XSLTInputSource xslSheet = new XSLTInputSource(xslStream);
            xslSheet.setSystemId(stylesheet.getAbsolutePath());
            XSLTInputSource src = new XSLTInputSource(fis);
            src.setSystemId(infile.getAbsolutePath());
            XSLTResultTarget res = new XSLTResultTarget(fos);
            processor.process(src, xslSheet, res);
        } finally {
            // make sure to close all handles, otherwise the garbage
            // collector will close them...whenever possible and
            // Windows may complain about not being able to delete files.
            try {
                if (xslStream != null){
                    xslStream.close();
                }
            } catch (IOException ignored){}
            try {
                if (fis != null){
                    fis.close();
                }
            } catch (IOException ignored){}
            try {
                if (fos != null){
                    fos.close();
                }
            } catch (IOException ignored){}
        }
    }

    public void addParam(String name, String value){
        processor.setStylesheetParam(name, value);
    }

} //-- XalanLiaison
