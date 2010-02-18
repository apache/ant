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
import org.apache.tools.ant.types.EnumeratedAttribute;
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
 * <li>Processing Instructions get ignored</li>
 * <li>Encoding is always UTF-8</li>
 * </ol>
 *
 * @since Ant 1.7
 */
public class EchoXML extends XMLFragment {

    private File file;
    private boolean append;
    private NamespacePolicy namespacePolicy = NamespacePolicy.DEFAULT;
    private static final String ERROR_NO_XML = "No nested XML specified";

    /**
     * Set the output file.
     * @param f the output file.
     */
    public void setFile(File f) {
        file = f;
    }

    /**
     * Set the namespace policy for the xml file
     * @param n namespace policy: "ignore," "elementsOnly," or "all"
     * @see
     * org.apache.tools.ant.util.DOMElementWriter.XmlNamespacePolicy
     */
    public void setNamespacePolicy(NamespacePolicy n) {
        namespacePolicy = n;
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
        DOMElementWriter writer =
            new DOMElementWriter(!append, namespacePolicy.getPolicy());
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

    public static class NamespacePolicy extends EnumeratedAttribute {
        private static final String IGNORE = "ignore";
        private static final String ELEMENTS = "elementsOnly";
        private static final String ALL = "all";

        public static final NamespacePolicy DEFAULT
            = new NamespacePolicy(IGNORE);

        public NamespacePolicy() {}

        public NamespacePolicy(String s) {
            setValue(s);
        }
        /** {@inheritDoc}. */
        public String[] getValues() {
            return new String[] {IGNORE, ELEMENTS, ALL};
        }

        public DOMElementWriter.XmlNamespacePolicy getPolicy() {
            String s = getValue();
            if (IGNORE.equalsIgnoreCase(s)) {
                return DOMElementWriter.XmlNamespacePolicy.IGNORE;
            } else if (ELEMENTS.equalsIgnoreCase(s)) {
                return
                    DOMElementWriter.XmlNamespacePolicy.ONLY_QUALIFY_ELEMENTS;
            } else if (ALL.equalsIgnoreCase(s)) {
                return DOMElementWriter.XmlNamespacePolicy.QUALIFY_ALL;
            } else {
                throw new BuildException("Invalid namespace policy: " + s);
            }
        }
    }
}
