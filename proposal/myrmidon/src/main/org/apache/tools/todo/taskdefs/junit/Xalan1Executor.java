/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs.junit;

import java.io.OutputStream;
import org.apache.xalan.xslt.XSLTInputSource;
import org.apache.xalan.xslt.XSLTProcessor;
import org.apache.xalan.xslt.XSLTProcessorFactory;
import org.apache.xalan.xslt.XSLTResultTarget;

/**
 * Xalan 1 executor. It will need a lot of things in the classpath: xerces for
 * the serialization, xalan and bsf for the extension.
 *
 * @author RT
 * @todo do everything via reflection to avoid compile problems ?
 */
public class Xalan1Executor extends XalanExecutor
{
    void execute()
        throws Exception
    {
        XSLTProcessor processor = XSLTProcessorFactory.getProcessor();
        // need to quote otherwise it breaks because of "extra illegal tokens"
        processor.setStylesheetParam( "output.dir", "'" + caller.getToDir().getAbsolutePath() + "'" );
        XSLTInputSource xml_src = new XSLTInputSource( caller.getDocument() );
        String system_id = caller.getStylesheetSystemId();
        XSLTInputSource xsl_src = new XSLTInputSource( system_id );
        OutputStream os = getOutputStream();
        XSLTResultTarget target = new XSLTResultTarget( os );
        processor.process( xml_src, xsl_src, target );
    }
}
