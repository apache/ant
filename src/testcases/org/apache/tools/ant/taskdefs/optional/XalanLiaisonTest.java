package org.apache.tools.ant.taskdefs.optional;

/*
 * Copyright  2001-2002,2004 The Apache Software Foundation
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

import org.apache.tools.ant.taskdefs.XSLTLiaison;

import java.io.File;

/**
 * Xalan Liaison testcase
 * @author <a href="mailto:sbailliez@apache.org">Stephane Bailliez</a>
 */
public class XalanLiaisonTest extends AbstractXSLTLiaisonTest {
    public XalanLiaisonTest(String name){
        super(name);
    }

    public void tearDown() {
        File f = new File("xalan1-redirect-out.tmp");
        if (f.exists()) {
            f.delete();
        }
    }

    protected XSLTLiaison createLiaison() throws Exception {
        return new XalanLiaison();
    }

    public void testXalan1Redirect() throws Exception {
        File xsl = getFile("/taskdefs/optional/xalan-redirect-in.xsl");
        liaison.setStylesheet(xsl);
        File out = new File("xalan1-redirect-out-dummy.tmp");
        File in = getFile("/taskdefs/optional/xsltliaison-in.xsl");
        try {
            liaison.addParam("xalan-version", "1");
            liaison.transform(in, out);
        } finally {
            out.delete();
        }
    }
}

