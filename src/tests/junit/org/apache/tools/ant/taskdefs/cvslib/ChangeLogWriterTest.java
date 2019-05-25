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
package org.apache.tools.ant.taskdefs.cvslib;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.apache.tools.ant.util.JAXPUtils;
import org.junit.Test;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 *  Test for the cvslib ChangeLogWriter
 */
public class ChangeLogWriterTest {

    private ChangeLogWriter writer = new ChangeLogWriter();

    @Test
    public void testNonUTF8Characters() throws Exception {
        CVSEntry entry = new CVSEntry(new Date(), "Se\u00f1orita", "2003 < 2004 && 3 > 5");
        entry.addFile("Medicare & review.doc", "1.1");
        entry.addFile("El\u00e8ments de style", "1.2");
        CVSEntry[] entries = {entry};

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintWriter pwriter = new PrintWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8));
        writer.printChangeLog(pwriter, entries);

        // make sure that the parsing does not break
        XMLReader xmlReader = JAXPUtils.getXMLReader();
        InputStream input = new ByteArrayInputStream(output.toByteArray());
        xmlReader.setContentHandler(new NullContentHandler());
        xmlReader.parse(new InputSource(input));
    }

    public static class NullContentHandler implements ContentHandler {
        public void endDocument() throws SAXException {
        }

        public void startDocument() throws SAXException {
        }

        public void characters(char[] ch, int start, int length) throws SAXException {
            @SuppressWarnings("unused")
            String debug = new String(ch, start, length);
        }

        public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        }

        public void endPrefixMapping(String prefix) throws SAXException {
        }

        public void skippedEntity(String name) throws SAXException {
        }

        public void setDocumentLocator(Locator locator) {
        }

        public void processingInstruction(String target, String data) throws SAXException {
        }

        public void startPrefixMapping(String prefix, String uri) throws SAXException {
        }

        public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
        }

        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
        }
    }
}
