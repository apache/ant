/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Ant" and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package org.apache.tools.ant.filters;

import java.io.File;
import java.io.Reader;
import java.io.FileReader;
import java.io.IOException;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.util.FileUtils;

/**
 * @author Peter Reilly
 */
public class DynamicFilterTest extends BuildFileTest {

    public DynamicFilterTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/filters/dynamicfilter.xml");
        executeTarget("init");
    }

    public void tearDown() {
        executeTarget("cleanup");
    }
    public void testCustomFilter() throws IOException {
        expectFileContains("dynamicfilter", "result/dynamicfilter",
                           "hellO wOrld");
    }

    // ------------------------------------------------------
    //   Helper methods
    // -----------------------------------------------------


    private void assertStringContains(String string, String contains) {
        assertTrue("[" + string + "] does not contain [" + contains +"]",
                   string.indexOf(contains) > -1);
    }

    private void assertStringNotContains(String string, String contains) {
        assertTrue("[" + string + "] does contain [" + contains +"]",
                   string.indexOf(contains) == -1);
    }

    private String getFileString(String filename)
        throws IOException
    {
        Reader r = null;
        try {
            r = new FileReader(getProject().resolveFile(filename));
            return  FileUtils.newFileUtils().readFully(r);
        }
        finally {
            try {r.close();} catch (Throwable ignore) {}
        }

    }

    private String getFileString(String target, String filename)
        throws IOException
    {
        executeTarget(target);
        return getFileString(filename);
    }

    private void expectFileContains(String name, String contains)
        throws IOException
    {
        String content = getFileString(name);
        assertTrue(
            "expecting file " + name + " to contain " + contains +
            " but got " + content, content.indexOf(contains) > -1);
    }

    private void expectFileContains(
        String target, String name, String contains)
        throws IOException
    {
        executeTarget(target);
        expectFileContains(name, contains);
    }

    public static class CustomFilter implements ChainableReader {
        char replace = 'x';
        char with    = 'y';

        public void setReplace(char replace) {
            this.replace = replace;
        }

        public void setWith(char with) {
            this.with = with;
        }

        public Reader chain(final Reader rdr) {
            return new BaseFilterReader(rdr) {
                public int read()
                    throws IOException
                {
                    int c = in.read();
                    if (c == replace)
                        return with;
                    else
                        return c;
                }
            };
        }
    }
}
