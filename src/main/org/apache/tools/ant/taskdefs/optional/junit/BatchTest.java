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
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Reference;

import java.util.*;

/**
 * Create JUnitTests from a list of files.
 *
 * @author <a href="mailto:jeff.martin@synamic.co.uk">Jeff Martin</a>
 * @author <a href="mailto:stefan.bodewig@megabit.net">Stefan Bodewig</a> 
 */
public final class BatchTest {
    private boolean fork=false;
    private boolean haltOnError=false;
    private boolean haltOnFailure=false;
    private Project project;
    private String ifCond = null;
    private String unlessCond = null;

    private Vector filesets = new Vector();
    private Vector formatters = new Vector();

    public BatchTest(Project project){
        this.project = project;
    }

    public void addFileSet(FileSet fs) {
        filesets.addElement(fs);
    }

    public void addFormatter(FormatterElement elem) {
        formatters.addElement(elem);
    }

    public void setIf(String propertyName) {
        ifCond = propertyName;
    }

    public void setUnless(String propertyName) {
        unlessCond = propertyName;
    }

    public final void setFork(boolean value) {
        this.fork = value;
    }
    public final void setHaltonerror(boolean value) {
        this.haltOnError = value;
    }
    public final void setHaltonfailure(boolean value) {
        this.haltOnFailure = value;
    }
    public final Enumeration elements(){
        return new FileList();
    }
    public class FileList implements Enumeration{
        private String files[]=null;
        private int i=0;
        
        private FileList(){
            Vector v = new Vector();
            for (int j=0; j<filesets.size(); j++) {
                FileSet fs = (FileSet) filesets.elementAt(j);
                DirectoryScanner ds = fs.getDirectoryScanner(project);
                ds.scan();
                String[] f = ds.getIncludedFiles();
                for (int k=0; k<f.length; k++) {
                    if (f[k].endsWith(".java")) {
                        v.addElement(f[k].substring(0, f[k].length()-5));
                    } else if (f[k].endsWith(".class")) {
                        v.addElement(f[k].substring(0, f[k].length()-6));
                    }
                }
            }

            files = new String[v.size()];
            v.copyInto(files);
        }
        public final boolean hasMoreElements(){
            if(i<files.length)return true;
            return false;
        }
        public final Object nextElement() throws NoSuchElementException{
            if(hasMoreElements()){
                JUnitTest test = new JUnitTest(javaToClass(files[i]));
                test.setHaltonerror(haltOnError);
                test.setHaltonfailure(haltOnFailure);
                test.setFork(fork);
                test.setIf(ifCond);
                test.setUnless(unlessCond);
                Enumeration list = formatters.elements();
                while (list.hasMoreElements()) {
                    test.addFormatter((FormatterElement)list.nextElement());
                }
                i++;
                return test;
            }
            throw new NoSuchElementException();
        }
        public final String javaToClass(String fileName){
            return fileName.replace(java.io.File.separatorChar, '.');
        }
    }
}
