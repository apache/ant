/*
 * Copyright  2001-2002,2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.tools.ant.taskdefs.optional.junit;

import java.io.OutputStream;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.tools.ant.BuildException;

/**
 * Xalan executor via JAXP. Nothing special must exists in the classpath
 * besides of course, a parser, jaxp and xalan.
 *
 * @ant.task ignore="true"
 */
public class Xalan2Executor extends XalanExecutor {

    private static final String aPack = "org.apache.xalan.";
    private static final String sPack = "com.sun.org.apache.xalan.";

    private TransformerFactory tfactory = TransformerFactory.newInstance();

    protected String getImplementation() throws BuildException {
        return tfactory.getClass().getName();
    }

    protected String getProcVersion(String classNameImpl) 
        throws BuildException {
        try {
            // xalan 2
            if (classNameImpl.equals(aPack + "processor.TransformerFactoryImpl") 
                ||
                classNameImpl.equals(aPack + "xslt.XSLTProcessorFactory")) {
                return getXalanVersion(aPack + "processor.XSLProcessorVersion");
            }
            // xalan xsltc
            if (classNameImpl.equals(aPack 
                                     + "xsltc.trax.TransformerFactoryImpl")){
                return getXSLTCVersion(aPack +"xsltc.ProcessorVersion");
            }
            // jdk 1.5 xsltc
            if (classNameImpl
                .equals(sPack + "internal.xsltc.trax.TransformerFactoryImpl")){
                return getXSLTCVersion(sPack 
                                       + "internal.xsltc.ProcessorVersion");
            }
            throw new BuildException("Could not find a valid processor version"
                                     + " implementation from " 
                                     + classNameImpl);
        } catch (ClassNotFoundException e){
            throw new BuildException("Could not find processor version "
                                     + "implementation", e);
        }
    }

    void execute() throws Exception {
        String system_id = caller.getStylesheetSystemId();
        Source xsl_src = new StreamSource(system_id);
        Transformer tformer = tfactory.newTransformer(xsl_src);
        Source xml_src = new DOMSource(caller.document);
        OutputStream os = getOutputStream();
        try {
            tformer.setParameter("output.dir", caller.toDir.getAbsolutePath());
            Result result = new StreamResult(os);
            tformer.transform(xml_src, result);
        } finally {
            os.close();
        }
    }
}
