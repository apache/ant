/*
 *  The Apache Software License, Version 1.1
 *
 *  Copyright (c) 2002 The Apache Software Foundation.  All rights
 *  reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *  1. Redistributions of source code must retain the above copyright
 *  notice, this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in
 *  the documentation and/or other materials provided with the
 *  distribution.
 *
 *  3. The end-user documentation included with the redistribution, if
 *  any, must include the following acknowlegement:
 *  "This product includes software developed by the
 *  Apache Software Foundation (http://www.apache.org/)."
 *  Alternately, this acknowlegement may appear in the software itself,
 *  if and wherever such third-party acknowlegements normally appear.
 *
 *  4. The names "The Jakarta Project", "Ant", and "Apache Software
 *  Foundation" must not be used to endorse or promote products derived
 *  from this software without prior written permission. For written
 *  permission, please contact apache@apache.org.
 *
 *  5. Products derived from this software may not be called "Apache"
 *  nor may "Apache" appear in their names without prior written
 *  permission of the Apache Group.
 *
 *  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 *  ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 *  USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 *  OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *  SUCH DAMAGE.
 *  ====================================================================
 *
 *  This software consists of voluntary contributions made by many
 *  individuals on behalf of the Apache Software Foundation.  For more
 *  information on the Apache Software Foundation, please see
 *  <http://www.apache.org/>.
 */
package org.apache.tools.ant.taskdefs.condition;

import org.apache.tools.ant.BuildException;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * compare two files for bitwise equality based on size and content. timestamps
 * are not looked at at all.
 *
 * @author Steve Loughran
 * @created 12 January 2002
 * @since Ant 1.5
 */

public class FilesMatch implements Condition {

    /**
     * files to compare
     */
    private File file1, file2;


    /**
     * Sets the File1 attribute 
     *
     * @param file1 The new File1 value
     */
    public void setFile1(File file1) {
        this.file1 = file1;
    }


    /**
     * Sets the File2 attribute
     *
     * @param file2 The new File2 value
     */
    public void setFile2(File file2) {
        this.file2 = file2;
    }

    /**
     * simple but sub-optimal comparision algorithm. 
     * written for working rather than fast. Better would 
     * be a block read into buffers followed by long comparisions
     * apart from the final 1-7 bytes. 
     */
     
    public boolean simpleFileCompare(File f1,File f2) throws IOException {
        BufferedInputStream in1=null;
        BufferedInputStream in2=null;
        boolean matches=true;
        try {
            in1=new BufferedInputStream(new FileInputStream(f1));
            in2=new BufferedInputStream(new FileInputStream(f2));
            int c1,c2;
            do {
                c1=in1.read();
                c2=in2.read();
                if(c1!=c2) {
                    matches=false;
                } 
            } while(matches && (c1!=-1));
        } finally {
            try {
                if(in1!=null) {
                    in1.close();
                }
            } catch(IOException e) { }
            try {
                if(in2!=null) {
                    in2.close();
                }
            } catch(IOException e) { }
        }
        return matches; 
    }

    /**
     * comparision method of the interface
     *
     * @return true if the files are equal
     * @exception BuildException if it all went pear-shaped
     */
    public boolean eval()
        throws BuildException {

        //validate
        if (file1 == null || file2 == null) {
            throw new BuildException("both file1 and file2 are required in fileequals");
        }
        if (!file1.exists()) {
            throw new BuildException("file " + file1 + " not found");
        }
        if (!file2.exists()) {
            throw new BuildException("file " + file2 + " not found");
        }
        
        //shortcut tests
        //#1 : same filename => true
        if(file1.equals(file2)) {
            return true;
        }
        
        //#2 : different size =>false
        if(file1.length()!=file2.length()) {
            return false;
        }
        
        //#now match the files
        boolean matches=false;
        try {
            matches=simpleFileCompare(file1,file2);
        } catch(IOException ioe) {
            throw new BuildException("when comparing files", ioe);
        }
        return matches;
    }
}

