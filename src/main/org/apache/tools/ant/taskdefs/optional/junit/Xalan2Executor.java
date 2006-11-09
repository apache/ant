/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
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
 * This class is not used by the framework any more.
 * We plan to remove it in Ant 1.8
 * @deprecated since Ant 1.7
 *
 *
 * @ant.task ignore="true"
 */
public class Xalan2Executor extends XalanExecutor {

    private static final String APAC = "org.apache.xalan.";
    private static final String SPAC = "com.sun.org.apache.xalan.";

    private TransformerFactory tfactory = TransformerFactory.newInstance();

    /** {@inheritDoc}. */
    protected String getImplementation() throws BuildException {
        return tfactory.getClass().getName();
    }

    /** {@inheritDoc}. */
    protected String getProcVersion(String classNameImpl)
        throws BuildException {
        try {
            // xalan 2
            if (classNameImpl.equals(APAC + "processor.TransformerFactoryImpl")
                ||
                classNameImpl.equals(APAC + "xslt.XSLTProcessorFactory")) {
                return getXalanVersion(APAC + "processor.XSLProcessorVersion");
            }
            // xalan xsltc
            if (classNameImpl.equals(APAC
                                     + "xsltc.trax.TransformerFactoryImpl")) {
                return getXSLTCVersion(APAC + "xsltc.ProcessorVersion");
            }
            // jdk 1.5 xsltc
            if (classNameImpl
                .equals(SPAC + "internal.xsltc.trax.TransformerFactoryImpl")) {
                return getXSLTCVersion(SPAC
                                       + "internal.xsltc.ProcessorVersion");
            }
            throw new BuildException("Could not find a valid processor version"
                                     + " implementation from "
                                     + classNameImpl);
        } catch (ClassNotFoundException e) {
            throw new BuildException("Could not find processor version "
                                     + "implementation", e);
        }
    }

    /** {@inheritDoc}. */
    void execute() throws Exception {
        String systemId = caller.getStylesheetSystemId();
        Source xslSrc = new StreamSource(systemId);
        Transformer tformer = tfactory.newTransformer(xslSrc);
        Source xmlSrc = new DOMSource(caller.document);
        OutputStream os = getOutputStream();
        try {
            tformer.setParameter("output.dir", caller.toDir.getAbsolutePath());
            Result result = new StreamResult(os);
            tformer.transform(xmlSrc, result);
        } finally {
            os.close();
        }
    }
}
