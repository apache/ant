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

import com.kvisco.xsl.XSLProcessor;
import com.kvisco.xsl.XSLReader;
import com.kvisco.xsl.XSLStylesheet;
import org.apache.tools.ant.taskdefs.XSLTLiaison;
import org.apache.tools.ant.BuildException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

/**
 * Concrete liaison for XSLP
 *
 * @author <a href="mailto:rubys@us.ibm.com">Sam Ruby</a>
 * @author <a href="mailto:sbailliez@apache.org">Stephane Bailliez</a>
 * @since Ant 1.1
 */
public class XslpLiaison implements XSLTLiaison {

    protected XSLProcessor processor;
    protected XSLStylesheet xslSheet;

    public XslpLiaison() {
        processor = new XSLProcessor();
        // uh ?! I'm forced to do that otherwise a setProperty crashes
        // with NPE !  I don't understand why the property map is static
        // though...  how can we do multithreading w/ multiple identical
        // parameters ?
        processor.getProperty("dummy-to-init-properties-map");
    }

    public void setStylesheet(File fileName) throws Exception {
        XSLReader xslReader = new XSLReader();
        // a file:/// + getAbsolutePath() does not work here
        // it is really the pathname
        xslSheet = xslReader.read(fileName.getAbsolutePath());
    }
    
    public void transform(File infile, File outfile) throws Exception {
        FileOutputStream fos = new FileOutputStream(outfile);
        // XSLP does not support encoding...we're in hot water.
        OutputStreamWriter out = new OutputStreamWriter(fos, "UTF8");
        processor.process(infile.getAbsolutePath(), xslSheet, out);
    }

    public void addParam(String name, String expression){
        processor.setProperty(name, expression);
    }

} //-- XSLPLiaison
