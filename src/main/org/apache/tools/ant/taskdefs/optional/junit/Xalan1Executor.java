/*
 * Copyright  2001-2002,2004 Apache Software Foundation
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
import org.apache.xalan.xslt.XSLTInputSource;
import org.apache.xalan.xslt.XSLTProcessor;
import org.apache.xalan.xslt.XSLTProcessorFactory;
import org.apache.xalan.xslt.XSLTResultTarget;

/**
 * Xalan 1 executor. It will need a lot of things in the classpath:
 * xerces for the serialization, xalan and bsf for the extension.
 * @todo do everything via reflection to avoid compile problems ?
 *
 * @ant.task ignore="true"
 */
public class Xalan1Executor extends XalanExecutor {
    void execute() throws Exception {
        XSLTProcessor processor = XSLTProcessorFactory.getProcessor();
        // need to quote otherwise it breaks because of "extra illegal tokens"
        processor.setStylesheetParam("output.dir", "'" + caller.toDir.getAbsolutePath() + "'");
        XSLTInputSource xml_src = new XSLTInputSource(caller.document);
        String system_id = caller.getStylesheetSystemId();
        XSLTInputSource xsl_src = new XSLTInputSource(system_id);
        OutputStream os = getOutputStream();
        try {
            XSLTResultTarget target = new XSLTResultTarget(os);
            processor.process(xml_src, xsl_src, target);
        } finally {
            os.close();
        }
    }
}
