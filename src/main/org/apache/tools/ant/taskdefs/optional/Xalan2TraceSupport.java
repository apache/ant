/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant.taskdefs.optional;

import java.io.PrintWriter;
import java.util.TooManyListenersException;

import javax.xml.transform.Transformer;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.XSLTProcess;
import org.apache.xalan.trace.PrintTraceListener;
import org.apache.xalan.transformer.TransformerImpl;

/**
 * Sets up trace support for a given transformer.
 *
 * @since Ant 1.8.0
 */
public class Xalan2TraceSupport implements XSLTTraceSupport {
    public void configureTrace(final Transformer t,
                               final XSLTProcess.TraceConfiguration conf) {
        if (t instanceof TransformerImpl && conf != null) {
            final PrintWriter w = new PrintWriter(conf.getOutputStream(), false);
            final PrintTraceListener tl = new PrintTraceListener(w);
            tl.m_traceElements = conf.getElements();
            tl.m_traceExtension = conf.getExtension();
            tl.m_traceGeneration = conf.getGeneration();
            tl.m_traceSelection = conf.getSelection();
            tl.m_traceTemplates = conf.getTemplates();
            try {
                ((TransformerImpl) t).getTraceManager().addTraceListener(tl);
            } catch (final TooManyListenersException tml) {
                throw new BuildException(tml);
            }
        }
    }
}
