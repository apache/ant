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

package org.apache.tools.ant.taskdefs.optional;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.tools.ant.taskdefs.XSLTLiaison;
import org.apache.tools.ant.taskdefs.XSLTLoggerAware;
import org.apache.tools.ant.taskdefs.XSLTLogger;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.Templates;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.OutputKeys;

/**
 * Concrete liaison for XSLT processor implementing TraX. (ie JAXP 1.1)
 *
 * @author <a href="mailto:rubys@us.ibm.com">Sam Ruby</a>
 * @author <a href="mailto:dims@yahoo.com">Davanum Srinivas</a>
 * @author <a href="mailto:sbailliez@apache.org">Stephane Bailliez</a>
 */
public class TraXLiaison implements XSLTLiaison, ErrorListener, XSLTLoggerAware {

    /** The trax TransformerFactory */
    private TransformerFactory tfactory = null;

    /** stylesheet stream, close it asap */
    private FileInputStream xslStream = null;

    /** Stylesheet template */
    private Templates templates = null;

    /** transformer */
    private Transformer transformer = null;

    private XSLTLogger logger;

    public TraXLiaison() throws Exception {
        tfactory = TransformerFactory.newInstance();
        tfactory.setErrorListener(this);
    }

//------------------- IMPORTANT
    // 1) Don't use the StreamSource(File) ctor. It won't work with
    // xalan prior to 2.2 because of systemid bugs.

    // 2) Use a stream so that you can close it yourself quickly
    // and avoid keeping the handle until the object is garbaged.
    // (always keep control), otherwise you won't be able to delete
    // the file quickly on windows.

    // 3) Always set the systemid to the source for imports, includes...
    // in xsl and xml...

    public void setStylesheet(File stylesheet) throws Exception {
        xslStream = new FileInputStream(stylesheet);
        StreamSource src = new StreamSource(xslStream);
        src.setSystemId(getSystemId(stylesheet));
        templates = tfactory.newTemplates(src);
        transformer = templates.newTransformer();
        transformer.setErrorListener(this);
    }

    public void transform(File infile, File outfile) throws Exception {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(infile);
            fos = new FileOutputStream(outfile);
            StreamSource src = new StreamSource(fis);
            src.setSystemId(getSystemId(infile));
            StreamResult res = new StreamResult(fos);
            // not sure what could be the need of this...
            res.setSystemId(getSystemId(outfile));

            transformer.transform(src, res);
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

    // make sure that the systemid is made of '/' and not '\' otherwise
    // crimson will complain that it cannot resolve relative entities
    // because it grabs the base uri via lastIndexOf('/') without
    // making sure it is really a /'ed path
    protected String getSystemId(File file){
        String path = file.getAbsolutePath();
        path = path.replace('\\','/');

        // on Windows, use 'file:///'
        if (File.separatorChar == '\\') {
            return FILE_PROTOCOL_PREFIX + "/" + path;
        }
        // Unix, use 'file://'
        return FILE_PROTOCOL_PREFIX + path;
    }

    public void addParam(String name, String value){
        transformer.setParameter(name, value);
    }

    public void setOutputtype(String type) throws Exception {
        transformer.setOutputProperty(OutputKeys.METHOD, type);
    }

    public void setLogger(XSLTLogger l) {
        logger = l;
    }
    
    public void error(TransformerException e)  {
        logError(e, "Error");
    }
    
    public void fatalError(TransformerException e)  {
        logError(e, "Fatal Error");
    }
    
    public void warning(TransformerException e)  {
        logError(e, "Warning");
    }
    
    private void logError(TransformerException e, String type) {
        StringBuffer msg = new StringBuffer();
        if(e.getLocator() != null) {
            if(e.getLocator().getSystemId() != null) {
                String url = e.getLocator().getSystemId();
                if(url.startsWith("file:///")) {
                  url = url.substring(8);
                }
                msg.append(url);
            } else {
                msg.append("Unknown file");
            }
            if(e.getLocator().getLineNumber() != -1) {                          
                msg.append(":"+e.getLocator().getLineNumber());
                if(e.getLocator().getColumnNumber() != -1) {
                    msg.append(":"+e.getLocator().getColumnNumber());
                }
            }
        }
        msg.append(": "+type+"! ");
        msg.append(e.getMessage());
        if(e.getCause() != null) {
            msg.append(" Cause: "+e.getCause());
        }

        logger.log(msg.toString());
    }

} //-- TraXLiaison
