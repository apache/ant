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

import junit.framework.TestCase;

import javax.xml.parsers.DocumentBuilder;

import org.apache.tools.ant.util.JAXPUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.InputStream;
import java.io.IOException;

public class DOMUtilTest extends TestCase {
    public void testListChildNodes() throws SAXException, IOException {
        DocumentBuilder db = JAXPUtils.getDocumentBuilder();
        InputStream is = this.getClass().getClassLoader().getResourceAsStream("taskdefs/optional/junit/matches.xml");
        Document doc = db.parse(is);
        NodeList nl = DOMUtil.listChildNodes(doc.getFirstChild(), new FooNodeFilter(), true);
        assertEquals(nl.getLength(), 3);
    }
    public class FooNodeFilter implements DOMUtil.NodeFilter {
        public boolean accept(Node node) {
            if (node.getNodeName().equals("foo")) {
                return true;
            }
            return false;  //To change body of implemented methods use File | Settings | File Templates.
        }
    }
}
