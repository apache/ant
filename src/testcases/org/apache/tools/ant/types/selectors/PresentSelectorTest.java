/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2002 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.types.selectors;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.*;
import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.types.Mapper;
import org.apache.tools.ant.util.FileNameMapper;
import org.apache.tools.ant.util.IdentityMapper;
import org.apache.tools.ant.util.GlobPatternMapper;

import java.io.File;

import junit.framework.TestCase;
import junit.framework.AssertionFailedError;

/**
 * Tests Present Selectors
 *
 * @author <a href="mailto:bruce@callenish.com">Bruce Atherton</a>
 */
public class PresentSelectorTest extends BaseSelectorTest {

    private Project project;

    public PresentSelectorTest(String name) {
        super(name);
    }

    /**
     * Factory method from base class. This is overriden in child
     * classes to return a specific Selector class.
     */
    public BaseSelector getInstance() {
        return new PresentSelector();
    }

    /**
     * Test the code that validates the selector.
     */
    public void testValidate() {
        PresentSelector s = (PresentSelector)getInstance();
        try {
            s.createMapper();
            s.createMapper();
            fail("PresentSelector allowed more than one nested mapper.");
        } catch (BuildException be1) {
            assertEquals("Cannot define more than one mapper",
                    be1.getMessage());
        }

        s = (PresentSelector)getInstance();
        try {
            s.isSelected(basedir,filenames[0],files[0]);
            fail("PresentSelector did not check for required fields");
        } catch (BuildException be2) {
            assertEquals("The targetdir attribute is required.",
                    be2.getMessage());
        }

    }

    /**
     * Tests to make sure that the selector is selecting files correctly.
     */
    public void testSelectionBehaviour() {
        PresentSelector s;
        String results;
        Mapper m;
        Mapper.MapperType identity = new Mapper.MapperType();
        identity.setValue("identity");
        Mapper.MapperType glob = new Mapper.MapperType();
        glob.setValue("glob");
        Mapper.MapperType merge = new Mapper.MapperType();
        merge.setValue("merge");
        Mapper.MapperType flatten = new Mapper.MapperType();
        flatten.setValue("flatten");

        try {
            makeBed();

            s = (PresentSelector)getInstance();
            s.setTargetdir(beddir);
            results = selectionString(s);
            assertEquals("TTTTTTTTTTTT", results);

            s = (PresentSelector)getInstance();
            s.setTargetdir(beddir);
            m = s.createMapper();
            m.setType(identity);
            results = selectionString(s);
            assertEquals("TTTTTTTTTTTT", results);

            s = (PresentSelector)getInstance();
            File subdir = new File("src/etc/testcases/taskdefs/expected");
            s.setTargetdir(subdir);
            m = s.createMapper();
            m.setType(flatten);
            results = selectionString(s);
	    if (JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_1)) {
                assertEquals("TTTTTFFFFFFF", results);
            } else {
                assertEquals("TTTTTTTTTTTF", results);
            }

            s = (PresentSelector)getInstance();
            s.setTargetdir(beddir);
            m = s.createMapper();
            m.setType(merge);
            m.setTo("asf-logo.gif.gz");
            results = selectionString(s);
            assertEquals("TTTTTTTTTTTT", results);

            s = (PresentSelector)getInstance();
            subdir = new File(beddir, "tar/bz2");
            s.setTargetdir(subdir);
            m = s.createMapper();
            m.setType(glob);
            m.setFrom("*.bz2");
            m.setTo("*.tar.bz2");
            results = selectionString(s);
            assertEquals("FFTFFFFFFFFF", results);

            try {
                makeMirror();

                s = (PresentSelector)getInstance();
                subdir = getProject().resolveFile("selectortest2");
                s.setTargetdir(subdir);
                results = mirrorSelectionString(s);
                assertEquals("TTTFFTTTTTTT", results);
                results = selectionString(s);
                assertEquals("TTTFFTTTTTTT", results);


            }
            finally {
                cleanupMirror();
            }

        }
        finally {
            cleanupBed();
        }

    }

}
