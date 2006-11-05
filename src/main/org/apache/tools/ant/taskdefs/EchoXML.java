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
package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.io.OutputStream;
import java.io.FileOutputStream;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.XMLFragment;
import org.apache.tools.ant.util.DOMElementWriter;
import org.apache.tools.ant.util.FileUtils;

import org.w3c.dom.Node;
import org.w3c.dom.Element;

/**
 * Echo XML.
 *
 * Known limitations:
 * <ol>
 * <li>Currently no XMLNS support</li>
 * <li>Processing Instructions get ignored</li>
 * <li>Encoding is always UTF-8</li>
 * </ol>
 *
 * @since Ant 1.7
 */
public class EchoXML extends XMLFragment {

    private File file;
    private boolean append;
    private static final String ERROR_NO_XML = "No nested XML specified";

    /**
     * Set the output file.
     * @param f the output file.
     */
    public void setFile(File f) {
        file = f;
    }

    /**
     * Set whether to append the output file.
     * @param b boolean append flag.
     */
    public void setAppend(boolean b) {
        append = b;
    }

    /**
     * Execute the task.
     */
    public void execute() {
        DOMElementWriter writer = new DOMElementWriter(!append);
        OutputStream os = null;
        try {
            if (file != null) {
                os = new FileOutputStream(file.getAbsolutePath(), append);
            } else {
                os = new LogOutputStream(this, Project.MSG_INFO);
            }
            Node n = getFragment().getFirstChild();
            if (n == null) {
                throw new BuildException(ERROR_NO_XML);
            }
            writer.write((Element) n, os);
        } catch (BuildException e) {
            throw e;
        } catch (Exception e) {
            throw new BuildException(e);
        } finally {
            FileUtils.close(os);
        }
    }

}
