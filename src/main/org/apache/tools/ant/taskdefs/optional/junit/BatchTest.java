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

package org.apache.tools.ant.taskdefs.optional.junit;


import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;


import java.util.Enumeration;
import java.util.Vector;
import java.io.File;

/**
 * <p> Create then run <code>JUnitTest</code>'s based on the list of files given by the fileset attribute.
 *
 * <p> Every <code>.java</code> or <code>.class</code> file in the fileset is 
 * assumed to be a testcase. 
 * A <code>JUnitTest</code> is created for each of these named classes with basic setup
 * inherited from the parent <code>BatchTest</code>.
 *
 * @author <a href="mailto:jeff.martin@synamic.co.uk">Jeff Martin</a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:sbailliez@imediation.com">Stephane Bailliez</a>
 *
 * @see JUnitTest
 */
public final class BatchTest extends BaseTest {

    /** the reference to the project */
    private Project project;

    /** the list of filesets containing the testcase filename rules */
    private Vector filesets = new Vector();

    /**
     * create a new batchtest instance
     * @param project     the project it depends on.
     */
    public BatchTest(Project project){
        this.project = project;
    }

    /**
     * Add a new fileset instance to this batchtest. Whatever the fileset is,
     * only filename that are <tt>.java</tt> or <tt>.class</tt> will be
     * considered as 'candidates'.
     * @param     fs the new fileset containing the rules to get the testcases.
     */
    public void addFileSet(FileSet fs) {
        filesets.addElement(fs);
    }

    /**
     * Return all <tt>JUnitTest</tt> instances obtain by applying the fileset rules.
     * @return  an enumeration of all elements of this batchtest that are
     * a <tt>JUnitTest</tt> instance.
     */
    public final Enumeration elements(){
        JUnitTest[] tests = createAllJUnitTest();
        return Enumerations.fromArray(tests);
    }

    /**
     * Convenient method to merge the <tt>JUnitTest</tt>s of this batchtest
     * to a <tt>Vector</tt>.
     * @param v the vector to which should be added all individual tests of this
     * batch test.
     */
    final void addTestsTo(Vector v){
        JUnitTest[] tests = createAllJUnitTest();
        v.ensureCapacity(v.size() + tests.length);
        for (int i = 0; i < tests.length; i++) {
            v.addElement(tests[i]);
        }
    }

    /**
     * Create all <tt>JUnitTest</tt>s based on the filesets. Each instance
     * is configured to match this instance properties.
     * @return the array of all <tt>JUnitTest</tt>s that belongs to this batch.
     */
    private JUnitTest[] createAllJUnitTest(){
        String[] filenames = getFilenames();
        JUnitTest[] tests = new JUnitTest[filenames.length];
        for (int i = 0; i < tests.length; i++) {
            String classname = javaToClass(filenames[i]);
            tests[i] = createJUnitTest(classname);
        }
        return tests;
    }

    /**
     * Iterate over all filesets and return the filename of all files
     * that end with <tt>.java</tt> or <tt>.class</tt>. This is to avoid
     * wrapping a <tt>JUnitTest</tt> over an xml file for example. A Testcase
     * is obviously a java file (compiled or not).
     * @return an array of filenames without their extension. As they should
     * normally be taken from their root, filenames should match their fully
     * qualified class name (If it is not the case it will fail when running the test).
     * For the class <tt>org/apache/Whatever.class</tt> it will return <tt>org/apache/Whatever</tt>.
     */
    private String[] getFilenames(){
        Vector v = new Vector();
        final int size = this.filesets.size();
        for (int j = 0; j < size; j++) {
            FileSet fs = (FileSet) filesets.elementAt(j);
            DirectoryScanner ds = fs.getDirectoryScanner(project);
            ds.scan();
            String[] f = ds.getIncludedFiles();
            for (int k = 0; k < f.length; k++) {
                String pathname = f[k];
                if (pathname.endsWith(".java")) {
                    v.addElement(pathname.substring(0, pathname.length() - ".java".length()));
                } else if (pathname.endsWith(".class")) {
                    v.addElement(pathname.substring(0, pathname.length() - ".class".length()));
                }
            }
        }

        String[] files = new String[v.size()];
        v.copyInto(files);
        return files;
    }

    /**
     * Convenient method to convert a pathname without extension to a
     * fully qualified classname. For example <tt>org/apache/Whatever</tt> will
     * be converted to <tt>org.apache.Whatever</tt>
     * @param filename the filename to "convert" to a classname.
     * @return the classname matching the filename.
     */
    public static final String javaToClass(String filename){
        return filename.replace(File.separatorChar, '.');
    }

    /**
     * Create a <tt>JUnitTest</tt> that has the same property as this
     * <tt>BatchTest</tt> instance.
     * @param classname the name of the class that should be run as a
     * <tt>JUnitTest</tt>. It must be a fully qualified name.
     * @return the <tt>JUnitTest</tt> over the given classname.
     */
    private JUnitTest createJUnitTest(String classname){
        JUnitTest test = new JUnitTest();
        test.setName(classname);
        test.setHaltonerror(this.haltOnError);
        test.setHaltonfailure(this.haltOnFail);
        test.setFiltertrace(this.filtertrace);
        test.setFork(this.fork);
        test.setIf(this.ifProperty);
        test.setUnless(this.unlessProperty);
        test.setTodir(this.destDir);
        test.setFailureProperty(failureProperty);
        test.setErrorProperty(errorProperty);
        Enumeration list = this.formatters.elements();
        while (list.hasMoreElements()) {
            test.addFormatter((FormatterElement) list.nextElement());
        }
        return test;
    }

}
