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

import com.kvisco.xsl.XSLProcessor;
import com.kvisco.xsl.XSLReader;
import com.kvisco.xsl.XSLStylesheet;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import org.apache.tools.ant.taskdefs.XSLTLiaison;

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

    public void addParam(String name, String expression) {
        processor.setProperty(name, expression);
    }

} //-- XSLPLiaison
