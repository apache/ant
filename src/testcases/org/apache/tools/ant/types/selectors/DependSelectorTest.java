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
import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.types.Mapper;
import org.apache.tools.ant.util.FileNameMapper;
import org.apache.tools.ant.util.IdentityMapper;
import org.apache.tools.ant.util.GlobPatternMapper;
import org.apache.tools.ant.util.JavaEnvUtils;

import java.io.File;

import junit.framework.TestCase;
import junit.framework.AssertionFailedError;

/**
 * Tests Depend Selectors
 *
 * @author <a href="mailto:bruce@callenish.com">Bruce Atherton</a>
 */
public class DependSelectorTest extends BaseSelectorTest {

    private Project project;

    public DependSelectorTest(String name) {
        super(name);
    }

    /**
     * Factory method from base class. This is overriden in child
     * classes to return a specific Selector class.
     */
    public BaseSelector getInstance() {
        return new DependSelector();
    }

    /**
     * Test the code that validates the selector.
     */
    public void testValidate() {
        DependSelector s = (DependSelector)getInstance();
        try {
            s.createMapper();
            s.createMapper();
            fail("DependSelector allowed more than one nested mapper.");
        } catch (BuildException be1) {
            assertEquals("Cannot define more than one mapper",
                    be1.getMessage());
        }

        s = (DependSelector)getInstance();
        try {
            s.isSelected(basedir,filenames[0],files[0]);
            fail("DependSelector did not check for required fields");
        } catch (BuildException be2) {
            assertEquals("The targetdir attribute is required.",
                    be2.getMessage());
        }

    }

    /**
     * Tests to make sure that the selector is selecting files correctly.
     */
    public void testSelectionBehaviour() {
        DependSelector s;
        String results;
        File subdir;
        Mapper m;
        Mapper.MapperType identity = new Mapper.MapperType();
        identity.setValue("identity");
        Mapper.MapperType glob = new Mapper.MapperType();
        glob.setValue("glob");
        Mapper.MapperType merge = new Mapper.MapperType();
        merge.setValue("merge");

        try {
            makeBed();

            s = (DependSelector)getInstance();
            s.setTargetdir(beddir);
            results = selectionString(s);
            assertEquals("FFFFFFFFFFFF", results);

            s = (DependSelector)getInstance();
            s.setTargetdir(beddir);
            m = s.createMapper();
            m.setType(identity);
            results = selectionString(s);
            assertEquals("FFFFFFFFFFFF", results);

            if (!JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_1)) {
                s = (DependSelector)getInstance();
                s.setTargetdir(beddir);
                m = s.createMapper();
                m.setType(merge);
                m.setTo("asf-logo.gif.gz");
                results = selectionString(s);
                assertEquals("TFFFFTTTFFF", results.substring(0,11));

                s = (DependSelector)getInstance();
                s.setTargetdir(beddir);
                m = s.createMapper();
                m.setType(merge);
                m.setTo("asf-logo.gif.bz2");
                results = selectionString(s);
                assertEquals("TTFTTTTTTTTT", results);

                // Test for path relative to project base directory
                s = (DependSelector)getInstance();
                subdir = new File("selectortest/tar/bz2");
                s.setTargetdir(subdir);
                m = s.createMapper();
                m.setType(glob);
                m.setFrom("*.bz2");
                m.setTo("*.tar.bz2");
                results = selectionString(s);
                assertEquals("FFTFFFFFFTTF", results);
            }

            s = (DependSelector)getInstance();
            subdir = new File(beddir,"tar/bz2");
            s.setTargetdir(subdir);
            m = s.createMapper();
            m.setType(glob);
            m.setFrom("*.bz2");
            m.setTo("*.tar.bz2");
            results = selectionString(s);
            assertEquals("FFFFFFFFFTTF", results);

            try {
                makeMirror();

                s = (DependSelector)getInstance();
                File testdir = getProject().resolveFile("selectortest2");
                s.setTargetdir(testdir);
                results = selectionString(s);
                assertEquals("FFFTTFFFFFFF", results);

                s = (DependSelector)getInstance();
                testdir = getProject().resolveFile("selectortest2/tar/bz2");
                s.setTargetdir(testdir);
                m = s.createMapper();
                m.setType(glob);
                m.setFrom("*.bz2");
                m.setTo("*.tar.bz2");
                results = mirrorSelectionString(s);
                assertEquals("FFFFFFFFFTTF", results);
                results = selectionString(s);
                assertEquals("FFFFFFFFFTTF", results);
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
