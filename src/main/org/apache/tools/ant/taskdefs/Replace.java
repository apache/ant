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

package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.*;
import org.apache.tools.ant.taskdefs.*;
import java.io.*;
import java.util.*;

/**
 * Replaces all occurrences of one or more string tokens with given
 * values in the indicated files. Each value can be either a string 
 * or the value of a property available in a designated property file.
 *
 * @author Stefano Mazzocchi <a href="mailto:stefano@apache.org">stefano@apache.org</a>
 * @author <a href="mailto:erik@desknetinc.com">Erik Langenbach</a>
 */
public class Replace extends MatchingTask {
    
    private File src = null;
    private NestedString token = null;
    private NestedString value = new NestedString();

    private File propertyFile = null;
    private Properties properties = null;
    private Vector replacefilters = new Vector();

    private File dir = null;

    private int fileCount;
    private int replaceCount;    
    private boolean summary = false;
    
    //Inner class
    public class NestedString {

        private StringBuffer buf = new StringBuffer();

        public void addText(String val) {
            buf.append(val);
        }

        public String getText() {
            return buf.toString();
        }
    }

    //Inner class
    public class Replacefilter
    {
        private String token;
        private String value;
        private String property;

        public void validate() throws BuildException {
            //Validate mandatory attributes
            if (token == null) {
                String message = "token is a mandatory attribute " + "of replacefilter.";
                throw new BuildException(message);
            }

            if ("".equals(token)) {
                String message ="The token attribute must not be an empty string.";
                throw new BuildException(message);
            }

            //value and property are mutually exclusive attributes
            if ((value != null) && (property != null)) {
                String message = "Either value or property " + "can be specified, but a replacefilter " + "element cannot have both.";
                throw new BuildException(message);
            }

            if ((property != null)) {
                //the property attribute must have access to a property file
                if (propertyFile == null) {
                    String message = "The replacefilter's property attribute " + "can only be used with the replacetask's " + "propertyFile attribute.";
                    throw new BuildException(message);
                }

                //Make sure property exists in property file
                if (properties == null ||
                        properties.getProperty(property) == null) {
                    String message = "property \"" + property + "\" was not found in " + propertyFile.getPath();
                    throw new BuildException(message);
                }
            }
        }

        public String getReplaceValue()
        {
            if (property != null) {
                return (String)properties.getProperty(property);
            }
            else if (value != null) {
                return value;
            }
            else if (Replace.this.value != null) {
                return Replace.this.value.getText();
            }
            else {
                //Default is empty string
                return new String("");
            }
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getToken() {
            return token;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public void setProperty(String property) {
            this.property = property;
        }

        public String getProperty() {
            return property;
        }
    }

    /**
     * Do the execution.
     */
    public void execute() throws BuildException {
        validateAttributes();

        if (propertyFile != null) {
            properties = getProperties(propertyFile);
        }

        validateReplacefilters();
        fileCount = 0;
        replaceCount = 0;

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
        
        if (summary) {
            log("Replaced " + replaceCount + " occurrences in " + fileCount + " files.", Project.MSG_INFO);
        }
    }
    
    /**
     * Validate attributes provided for this task in .xml build file.
     *
     * @exception BuildException if any supplied attribute is invalid or any
     * mandatory attribute is missing
     */
    public void validateAttributes() throws BuildException {
        if (src == null && dir == null) {
            String message = "Either the file or the dir attribute " + "must be specified";
            throw new BuildException(message, location);
        }
        if (propertyFile != null && !propertyFile.exists()) {
            String message = "Property file " + propertyFile.getPath() + " does not exist.";
            throw new BuildException(message, location);
        }
        if (token == null && replacefilters.size() == 0) {
            String message = "Either token or a nested replacefilter "
                + "must be specified";
            throw new BuildException(message, location);
        }
        if (token != null && "".equals(token.getText())) {
            String message ="The token attribute must not be an empty string.";
            throw new BuildException(message, location);
        }
    }

    /**
     * Validate nested elements.
     *
     * @exception BuildException if any supplied attribute is invalid or any
     * mandatory attribute is missing
     */
    public void validateReplacefilters()
            throws BuildException {
        for (int i = 0; i < replacefilters.size(); i++) {
            Replacefilter element = (Replacefilter) replacefilters.elementAt(i);
            element.validate();
        }
    }

    public Properties getProperties(File propertyFile) throws BuildException {
        Properties properties = new Properties();

        try {
            properties.load(new FileInputStream(propertyFile));
        }
        catch (FileNotFoundException e) {
            String message = "Property file (" + propertyFile.getPath() + ") not found.";
            throw new BuildException(message);
        }
        catch (IOException e) {
            String message = "Property file (" + propertyFile.getPath() + ") cannot be loaded.";
            throw new BuildException(message);
        }

        return properties;
    }

    /**
     * Perform the replacement on the given file.
     *
     * The replacement is performed on a temporary file which then
     * replaces the original file.
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

            // read the entire file into a StringBuffer
            //   size of work buffer may be bigger than needed
            //   when multibyte characters exist in the source file
            //   but then again, it might be smaller than needed on
            //   platforms like Windows where length can't be trusted
            int fileLengthInBytes = (int)(src.length());
            StringBuffer tmpBuf = new StringBuffer(fileLengthInBytes);
            int readChar = 0;
            int totread = 0;
            while (true) {
                readChar = br.read();
                if (readChar < 0) { break; }
                tmpBuf.append((char)readChar);
                totread++;
            }

            // create a String so we can use indexOf
            String buf = tmpBuf.toString();

            //Preserve original string (buf) so we can compare the result
            String newString = new String(buf);

            if (token != null)
            {
                // line separators in values and tokens are "\n"
                // in order to compare with the file contents, replace them
                // as needed
                String linesep = System.getProperty("line.separator");
                String val = stringReplace(value.getText(), "\n", linesep);
                String tok = stringReplace(token.getText(), "\n", linesep);

                // for each found token, replace with value
                log("Replacing in " + src.getPath() + ": " + token.getText() + " --> " + value.getText(), Project.MSG_VERBOSE);
                newString = stringReplace(newString, tok, val);
            }

            if (replacefilters.size() > 0) {
                newString = processReplacefilters(newString, src.getPath());
            }

            boolean changes = !newString.equals(buf);
            if (changes) {
                bw.write(newString,0,newString.length());
                bw.flush();
            }

            // cleanup
            bw.close();
            br.close();

            // If there were changes, move the new one to the old one;
            // otherwise, delete the new one
            if (changes) {
                ++fileCount;
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

    private String processReplacefilters(String buffer, String filename) {
        String newString = new String(buffer);

        for (int i = 0; i < replacefilters.size(); i++) {
            Replacefilter filter = (Replacefilter) replacefilters.elementAt(i);

            //for each found token, replace with value
            log("Replacing in " + filename + ": " + filter.getToken() + " --> " + filter.getReplaceValue(), Project.MSG_VERBOSE);
            newString = stringReplace(newString, filter.getToken(), filter.getReplaceValue());
        }

        return newString;
    }


    /**
     * Set the source file.
     */
    public void setFile(File file) {
        this.src = file;
    }

    /**
     * Request a summary
     *
     * @param summary true if you would like a summary logged of the replace operation
     */
    public void setSummary(boolean summary) {
        this.summary = summary;
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
     * Sets a file to be searched for property values.
     */
    public void setPropertyFile(File filename) {
        propertyFile = filename;
    }

    /**
     * Add nested <replacefilter> element.
     */
    public Replacefilter createReplacefilter() {
        Replacefilter filter = new Replacefilter();
        replacefilters.addElement(filter);
        return filter;
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
            ++replaceCount;
        }

        // write the remaining characters
        if (str.length() > start) {
            ret.append(str.substring(start, str.length()));
        }

        return ret.toString();
    }

}
