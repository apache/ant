/* 
 * Copyright  2000-2002,2004 Apache Software Foundation
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

package org.apache.tools.ant.taskdefs.optional;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.tools.ant.taskdefs.XSLTLiaison;
import org.apache.xalan.xslt.XSLTInputSource;
import org.apache.xalan.xslt.XSLTProcessor;
import org.apache.xalan.xslt.XSLTProcessorFactory;
import org.apache.xalan.xslt.XSLTResultTarget;

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
                if (xslStream != null) {
                    xslStream.close();
                }
            } catch (IOException ignored) {
                //ignore
            }
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException ignored) {
                //ignore
           }
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException ignored) {
                //ignore
            }
        }
    }

    public void addParam(String name, String value) {
        processor.setStylesheetParam(name, value);
    }

} //-- XalanLiaison
