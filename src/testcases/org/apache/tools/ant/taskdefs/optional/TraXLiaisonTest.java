package org.apache.tools.ant.taskdefs.optional;

import org.apache.tools.ant.taskdefs.XSLTLiaison;
import org.apache.tools.ant.taskdefs.XSLTLogger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.JAXPUtils;

import java.io.File;

import junit.framework.AssertionFailedError;

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

/**
 * TraX XSLTLiaison testcase
 */
public class TraXLiaisonTest extends AbstractXSLTLiaisonTest
    implements XSLTLogger {

    public TraXLiaisonTest(String name){
        super(name);
    }

    public void tearDown() {
        File f = new File("xalan2-redirect-out.tmp");
        if (f.exists()) {
            f.delete();
        }
    }

    public XSLTLiaison createLiaison() throws Exception {
        TraXLiaison l = new TraXLiaison();
        l.setLogger(this);
        return l;
    }

    public void testXalan2Redirect() throws Exception {
        File xsl = getFile("/taskdefs/optional/xalan-redirect-in.xsl");
        liaison.setStylesheet(xsl);
        File out = new File("xalan2-redirect-out-dummy.tmp");
        File in = getFile("/taskdefs/optional/xsltliaison-in.xsl");
        try {
            liaison.addParam("xalan-version", "2");
            liaison.transform(in, out);
        } finally {
            out.delete();
        }
    }

    public void testMultipleTransform() throws Exception {
        File xsl = getFile("/taskdefs/optional/xsltliaison-in.xsl");
        liaison.setStylesheet(xsl);
        liaison.addParam("param", "value");
        File in = getFile("/taskdefs/optional/xsltliaison-in.xml");
        // test for 10 consecutives transform
        for (int i = 0; i < 50; i++){
            File out = new File("xsltliaison" + i + ".tmp");
            try {
                liaison.transform(in, out);
            } catch (Exception e){
                throw new BuildException("failed in transform " + i, e);
            } finally {
                out.delete();
            }
        }
    }

    public void testSystemId(){
        File file = null;
        if ( File.separatorChar == '\\' ){
            file = new File("d:\\jdk");
        } else {
            file = new File("/user/local/bin");
        }
        String systemid = JAXPUtils.getSystemId(file);
        assertTrue("SystemIDs should start by file:/", systemid.startsWith("file:/"));
        assertTrue("SystemIDs should not start with file:////", !systemid.startsWith("file:////"));
    }

    public void log(String message) {
        throw new AssertionFailedError("Liaison sent message: "+message);
    }

}
