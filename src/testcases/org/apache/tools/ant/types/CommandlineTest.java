/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000 The Apache Software Foundation.  All rights
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
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
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

package org.apache.tools.ant.types;

import org.apache.tools.ant.BuildException;

import junit.framework.TestCase;
import junit.framework.AssertionFailedError;

import java.io.File;

/**
 * JUnit 3 testcases for org.apache.tools.ant.CommandLine
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public class CommandlineTest extends TestCase {

    public CommandlineTest(String name) {
        super(name);
    }

    public void testTokenizer() {
        String[] s = Commandline.translateCommandline("1 2 3");
        assertEquals("Simple case", 3, s.length);
        for (int i=0; i<3; i++) {
            assertEquals(""+(i+1), s[i]);
        }
        
        s = Commandline.translateCommandline("");
        assertEquals("empty string", 0, s.length);

        s = Commandline.translateCommandline(null);
        assertEquals("null", 0, s.length);

        s = Commandline.translateCommandline("1 \'2\' 3");
        assertEquals("Simple case with single quotes", 3, s.length);
        assertEquals("Single quotes have been stripped", "2", s[1]);

        s = Commandline.translateCommandline("1 \"2\" 3");
        assertEquals("Simple case with double quotes", 3, s.length);
        assertEquals("Double quotes have been stripped", "2", s[1]);

        s = Commandline.translateCommandline("1 \"2 3\" 4");
        assertEquals("Case with double quotes and whitespace", 3, s.length);
        assertEquals("Double quotes stripped, space included", "2 3", s[1]);
        
        s = Commandline.translateCommandline("1 \"2\'3\" 4");
        assertEquals("Case with double quotes around single quote", 3, s.length);
        assertEquals("Double quotes stripped, single quote included", "2\'3",
                     s[1]);

        s = Commandline.translateCommandline("1 \'2 3\' 4");
        assertEquals("Case with single quotes and whitespace", 3, s.length);
        assertEquals("Single quotes stripped, space included", "2 3", s[1]);
        
        s = Commandline.translateCommandline("1 \'2\"3\' 4");
        assertEquals("Case with single quotes around double quote", 3, s.length);
        assertEquals("Single quotes stripped, double quote included", "2\"3",
                     s[1]);

        // \ doesn't have a special meaning anymore - this is different from
        // what the Unix sh does but causes a lot of problems on DOS
        // based platforms otherwise
        s = Commandline.translateCommandline("1 2\\ 3 4");
        assertEquals("case with quoted whitespace", 4, s.length);
        assertEquals("backslash included", "2\\", s[1]);


        // now to the expected failures
        
        try {
            s = Commandline.translateCommandline("a \'b c");
            fail("unbalanced single quotes undetected");
        } catch (BuildException be) {
            assertEquals("unbalanced quotes in a \'b c", be.getMessage());
        }

        try {
            s = Commandline.translateCommandline("a \"b c");
            fail("unbalanced double quotes undetected");
        } catch (BuildException be) {
            assertEquals("unbalanced quotes in a \"b c", be.getMessage());
        }
    }

    public void testToString() {
        assertEquals("", Commandline.toString(new String[0]));
        assertEquals("", Commandline.toString(null));
        assertEquals("1 2 3", Commandline.toString(new String[] {"1", "2", "3"}));
        assertEquals("1 \"2 3\"", Commandline.toString(new String[] {"1", "2 3"}));
        assertEquals("1 \"2\'3\"", Commandline.toString(new String[] {"1", "2\'3"}));
        assertEquals("1 \'2\"3\'", Commandline.toString(new String[] {"1", "2\"3"}));
    }
}
