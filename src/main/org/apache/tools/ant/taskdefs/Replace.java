/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights 
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

package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.*;
import java.io.*;
import java.util.*;

/**
 * Replaces all the occurrences of the given string token with the given
 * string value of the indicated files.
 *
 * @author Stefano Mazzocchi <a href="mailto:stefano@apache.org">stefano@apache.org</a>
 * @author <a href="mailto:erik@desknetinc.com">Erik Langenbach</a> 
 */
public class Replace extends MatchingTask {
    
    private File src = null;
    private NestedString token = null;
    private NestedString value = new NestedString();

    private File dir = null;
    
    public class NestedString {

        private StringBuffer buf = new StringBuffer();

        public void addText(String val) {
            buf.append(val);
        }

        public String getText() {
            return buf.toString();
        }
    }

    /**
     * Do the execution.
     */
    public void execute() throws BuildException {
        
        if (token == null) {
            throw new BuildException("replace token must not be null", location);
        }

        if (token.getText().equals("")) {
            throw new BuildException("replace token must not be empty", location);
        }

        if (src == null && dir == null) {
            throw new BuildException("Either the file or the dir attribute must be specified", location);
        }
        
        log("Replacing " + token.getText() + " --> " + value.getText());

        if (src != null) {
            processFile(src);
        }
        
        if (dir != null) {
            DirectoryScanner ds = super.getDirectoryScanner(dir);
            String[] srcs = ds.getIncludedFiles();

            for(int i=0; i<srcs.length; i++) {
                File file = new File(dir,srcs[i]); 
                processFile(file);
            }
        }
    }

    /**
     * Perform the replacement on the given file.
     *
     * The replacement is performed on a temporary file which then replaces the original file.
     *
     * @param src the source file
     */
    private void processFile(File src) throws BuildException {
        if (!src.exists()) { 
            throw new BuildException("Replace: source file " + src.getPath() + " doesn't exist", location);
        }

        File temp = new File(src.getPath() + ".temp");

        if (temp.exists()) {
            throw new BuildException("Replace: temporary file " + temp.getPath() + " already exists", location);
        }

        try {
            BufferedReader br = new BufferedReader(new FileReader(src));
            BufferedWriter bw = new BufferedWriter(new FileWriter(temp));

            // read the entire file into a char[]
            int fileLength = (int)(src.length());
            char[] tmpBuf = new char[fileLength];
            int numread = 0;
            int totread = 0;
            while (numread != -1 && totread < fileLength) {
                numread = br.read(tmpBuf,totread,fileLength);
                totread += numread;
            }

            // create a String so we can use indexOf
            String buf = new String(tmpBuf);

            // line separators in values and tokens are "\n"
            // in order to compare with the file contents, replace them
            // as needed
            String linesep = System.getProperty("line.separator");
            String val = stringReplace(value.getText(), "\n", linesep);
            String tok = stringReplace(token.getText(), "\n", linesep);

            // for each found token, replace with value
            String  newString = stringReplace(buf, tok, val);
            boolean changes   = !newString.equals(buf);

            if (changes) {
                bw.write(newString,0,newString.length());
                bw.flush();
            }
            
            // cleanup
            bw.close();
            br.close();

            // If there were changes, move the new one to the old one, otherwise, delete the new one
            if (changes) {
                src.delete();
                temp.renameTo(src);
            } else {
                temp.delete();
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new BuildException(ioe, location);
        }       
    }


    /**
     * Set the source file.
     */
    public void setFile(File file) {
        this.src = file;
    }

    /**
     * Set the source files path when using matching tasks.
     */
    public void setDir(File dir) {
        this.dir = dir;
    }

    /**
     * Set the string token to replace.
     */
    public void setToken(String token) {
        createReplaceToken().addText(token);
    }

    /**
     * Set the string value to use as token replacement.
     */
    public void setValue(String value) {
        createReplaceValue().addText(value);
    }

    /**
     * Nested <replacetoken> element.
     */
    public NestedString createReplaceToken() {
        if (token == null) {
            token = new NestedString();
        }
        return token;
    }

    /**
     * Nested <replacevalue> element.
     */
    public NestedString createReplaceValue() {
        return value;
    }

    /**
     * Replace occurrences of str1 in string str with str2
     */    
    private String stringReplace(String str, String str1, String str2) {
        StringBuffer ret = new StringBuffer();
        int start = 0;
        int found = str.indexOf(str1);
        while (found >= 0) {
            // write everything up to the found str1
            if (found > start) {
                ret.append(str.substring(start, found));
            }

            // write the replacement str2
            if (str2 != null) {
                ret.append(str2);
            }

            // search again
            start = found + str1.length();
            found = str.indexOf(str1,start);
        }

        // write the remaining characters
        if (str.length() > start) {
            ret.append(str.substring(start, str.length()));
        }

        return ret.toString();
    }

}
