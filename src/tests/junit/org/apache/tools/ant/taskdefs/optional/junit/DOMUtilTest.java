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
package org.apache.tools.ant.taskdefs.optional.junit;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;

import org.apache.tools.ant.util.JAXPUtils;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class DOMUtilTest {

    @Test
    public void testListChildNodes() throws SAXException, IOException {
        DocumentBuilder db = JAXPUtils.getDocumentBuilder();
        InputStream is = this.getClass().getClassLoader().getResourceAsStream("taskdefs/optional/junit/matches.xml");
        Document doc = db.parse(is);
        NodeList nl = DOMUtil.listChildNodes(doc.getDocumentElement(), new FooNodeFilter(), true);
        assertEquals("expecting 3", 3, nl.getLength());
    }

    public class FooNodeFilter implements DOMUtil.NodeFilter {
        public boolean accept(Node node) {
            return node.getNodeName().equals("foo");
        }
    }
}
