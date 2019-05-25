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

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeNoException;
import static org.junit.Assume.assumeTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.security.Permission;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.XSLTLiaison;
import org.apache.tools.ant.taskdefs.XSLTLogger;
import org.apache.tools.ant.util.JAXPUtils;
import org.junit.After;
import org.junit.Test;

/**
 * TraX XSLTLiaison testcase
 */
public class TraXLiaisonTest extends AbstractXSLTLiaisonTest implements XSLTLogger {

    @After
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

    @Test
    public void testXalan2RedirectViaJDKFactory() throws Exception {
        try {
            getClass().getClassLoader().loadClass("org.apache.xalan.lib.Redirect");
        } catch (Exception exc) {
            assumeNoException("xalan redirect is not on the classpath", exc);
        }
        try {
            String factoryName = TransformerFactory.newInstance().getClass().getName();
            assumeFalse("TraxFactory is Xalan",
                    "org.apache.xalan.processor.TransformerFactoryImpl".equals(factoryName));
        } catch (TransformerFactoryConfigurationError exc) {
            throw new RuntimeException(exc);
        }
        File xsl = getFile("/taskdefs/optional/xalan-redirect-in.xsl");
        liaison.setStylesheet(xsl);
        ((TraXLiaison) liaison)
            .setFeature("http://www.oracle.com/xml/jaxp/properties/enableExtensionFunctions", true);
        File out = new File("xalan2-redirect-out-dummy.tmp");
        File in = getFile("/taskdefs/optional/xsltliaison-in.xsl");
        ClassLoader orig = Thread.currentThread().getContextClassLoader();
        try {
            liaison.addParam("xalan-version", "2");
            // Use the JRE's Xerces, not lib/optional/xerces.jar:
            Thread.currentThread().setContextClassLoader(new ClassLoader(ClassLoader.getSystemClassLoader().getParent()) {
                public InputStream getResourceAsStream(String name) {
                    if (name.startsWith("META-INF/services/")) {
                        // work around JAXP #6723276 in JDK 6
                        return new ByteArrayInputStream(new byte[0]);
                    }
                    return super.getResourceAsStream(name);
                }
            });
            // Tickle #52382:
            System.setSecurityManager(new SecurityManager() {
                public void checkPermission(Permission perm) {
                }
            });
            liaison.transform(in, out);
        } finally {
            out.delete();
            Thread.currentThread().setContextClassLoader(orig);
            System.setSecurityManager(null);
        }
    }

    @Test
    public void testXalan2RedirectViaXalan() throws Exception {
        try {
            getClass().getClassLoader().loadClass("org.apache.xalan.lib.Redirect");
        } catch (Exception exc) {
            assumeNoException("xalan redirect is not on the classpath", exc);
        }
        try {
            String factoryName = TransformerFactory.newInstance().getClass().getName();
            assumeTrue("TraxFactory is " + factoryName + " and not Xalan",
                    "org.apache.xalan.processor.TransformerFactoryImpl".equals(factoryName));
        } catch (TransformerFactoryConfigurationError exc) {
            throw new RuntimeException(exc);
        }
        File xsl = getFile("/taskdefs/optional/xalan-redirect-in.xsl");
        liaison.setStylesheet(xsl);
        File out = new File("xalan2-redirect-out-dummy.tmp");
        File in = getFile("/taskdefs/optional/xsltliaison-in.xsl");
        try {
            liaison.addParam("xalan-version", "2");
            System.setSecurityManager(new SecurityManager() {
                public void checkPermission(Permission perm) {
                }
            });
            liaison.transform(in, out);
        } finally {
            out.delete();
            System.setSecurityManager(null);
        }
    }

    @Test
    public void testMultipleTransform() throws Exception {
        File xsl = getFile("/taskdefs/optional/xsltliaison-in.xsl");
        liaison.setStylesheet(xsl);
        liaison.addParam("param", "value");
        File in = getFile("/taskdefs/optional/xsltliaison-in.xml");
        // test for 10 consecutives transform
        for (int i = 0; i < 50; i++) {
            File out = new File("xsltliaison" + i + ".tmp");
            try {
                liaison.transform(in, out);
            } catch (Exception e) {
                throw new BuildException("failed in transform " + i, e);
            } finally {
                out.delete();
            }
        }
    }

    @Test
    public void testSystemId() {
        File file = null;
        if (File.separatorChar == '\\') {
            file = new File("d:\\jdk");
        } else {
            file = new File("/user/local/bin");
        }
        String systemid = JAXPUtils.getSystemId(file);
        assertThat("SystemIDs should start by file:/", systemid, startsWith("file:/"));
        assertThat("SystemIDs should not start with file:////", systemid, not(startsWith("file:////")));
    }

    public void log(String message) {
        throw new AssertionError("Liaison sent message: " + message);
    }

}
