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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.EnumeratedAttribute;

import java.io.*;
import java.util.*;
import java.text.*;

/**
 * Task to convert text source files to local OS formatting conventions, as
 * well as repair text files damaged by misconfigured or misguided editors or
 * file transfer programs.
 * <p>
 * This task can take the following arguments:
 * <ul>
 * <li>srcdir
 * <li>destdir
 * <li>include
 * <li>exclude
 * <li>cr
 * <li>tab
 * <li>eof
 * </ul>
 * Of these arguments, only <b>sourcedir</b> is required.
 * <p>
 * When this task executes, it will scan the srcdir based on the include
 * and exclude properties.
 * <p>
 * <em>Warning:</em> do not run on binary or carefully formatted files.
 * this may sound obvious, but if you don't specify asis, presume that
 * your files are going to be modified.  If you want tabs to be fixed,
 * whitespace characters may be added or removed as necessary.  Similarly,
 * for CR's - in fact cr="add" can result in cr characters being removed.
 * (to handle cases where other programs have converted CRLF into CRCRLF).
 *
 * @author Sam Ruby <a href="mailto:rubys@us.ibm.com">rubys@us.ibm.com</a>
 */

public class FixCRLF extends MatchingTask {

    private int addcr;      // cr:  -1 => remove, 0 => asis, +1 => add
    private int addtab;     // tab: -1 => remove, 0 => asis, +1 => add
    private int ctrlz;      // eof: -1 => remove, 0 => asis, +1 => add
    private int tablength = 8;  // length of tab in spaces

    private File srcDir;
    private File destDir = null;

    /**
     * Defaults the properties based on the system type.
     * <ul><li>Unix: cr="remove" tab="asis" eof="remove"
     *     <li>DOS: cr="add" tab="asis" eof="asis"</ul>
     */
    public FixCRLF() {
        if (System.getProperty("path.separator").equals(":")) {
            addcr = -1; // remove
            ctrlz = -1; // remove
        } else {
            addcr = +1; // add
            ctrlz = 0;  // asis
        }
    }

    /**
     * Set the source dir to find the source text files.
     */
    public void setSrcdir(File srcDir) {
        this.srcDir = srcDir;
    }

    /**
     * Set the destination where the fixed files should be placed.
     * Default is to replace the original file.
     */
    public void setDestdir(File destDir) {
        this.destDir = destDir;
    }

    /**
     * Specify how carriage return (CR) charaters are to be handled
     *
     * @param option valid values:
     * <ul>
     * <li>add: ensure that there is a CR before every LF
     * <li>asis: leave CR characters alone
     * <li>remove: remove all CR characters
     * </ul>
     */
    public void setCr(AddAsisRemove attr) {
        String option = attr.getValue();
        if (option.equals("remove")) {
            addcr = -1;
        } else if (option.equals("asis")) {
            addcr = 0;
        } else {
            // must be "add"
            addcr = +1;
        }
    }

    /**
     * Specify how tab charaters are to be handled
     *
     * @param option valid values:
     * <ul>
     * <li>add: convert sequences of spaces which span a tab stop to tabs
     * <li>asis: leave tab and space characters alone
     * <li>remove: convert tabs to spaces
     * </ul>
     */
    public void setTab(AddAsisRemove attr) {
        String option = attr.getValue();
        if (option.equals("remove")) {
            addtab = -1;
        } else if (option.equals("asis")) {
            addtab = 0;
        } else {
            // must be "add"
            addtab = +1;
        }
    }

    /**
     * Specify tab length in characters
     *
     * @param tlength specify the length of tab in spaces, has to be a power of 2
     */
    public void setTablength(int tlength) throws BuildException {
        if (tlength < 2 || (tlength & (tlength-1)) != 0) {
            throw new BuildException("tablength must be a positive power of 2",
                                     location);
        }
        tablength = tlength;
    }

    /**
     * Specify how DOS EOF (control-z) charaters are to be handled
     *
     * @param option valid values:
     * <ul>
     * <li>add: ensure that there is an eof at the end of the file
     * <li>asis: leave eof characters alone
     * <li>remove: remove any eof character found at the end
     * </ul>
     */
    public void setEof(AddAsisRemove attr) {
        String option = attr.getValue();
        if (option.equals("remove")) {
            ctrlz = -1;
        } else if (option.equals("asis")) {
            ctrlz = 0;
        } else {
            // must be "add"
            ctrlz = +1;
        }
    }

    /**
     * Executes the task.
     */
    public void execute() throws BuildException {
        // first off, make sure that we've got a srcdir and destdir

        if (srcDir == null) {
            throw new BuildException("srcdir attribute must be set!");
        }
        if (!srcDir.exists()) {
            throw new BuildException("srcdir does not exist!");
        }
        if (!srcDir.isDirectory()) {
            throw new BuildException("srcdir is not a directory!");
        }
        if (destDir != null) {
            if (!destDir.exists()) {
                throw new BuildException("destdir does not exist!");
            }
            if (!destDir.isDirectory()) {
                throw new BuildException("destdir is not a directory!");
            }
        }

        // log options used
        log("options:" +
            " cr=" + (addcr==-1 ? "add" : addcr==0 ? "asis" : "remove") +
            " tab=" + (addtab==-1 ? "add" : addtab==0 ? "asis" : "remove") +
            " eof=" + (ctrlz==-1 ? "add" : ctrlz==0 ? "asis" : "remove") +
            " tablength=" + tablength,
            Project.MSG_VERBOSE);

        DirectoryScanner ds = super.getDirectoryScanner(srcDir);
        String[] files = ds.getIncludedFiles();

        for (int i = 0; i < files.length; i++) {
            File srcFile = new File(srcDir, files[i]);

            // read the contents of the file
            int count = (int)srcFile.length();
            byte indata[] = new byte[count];
            try {
                FileInputStream inStream = new FileInputStream(srcFile);
                inStream.read(indata);
                inStream.close();
            } catch (IOException e) {
                throw new BuildException(e);
            }

            // count the number of cr, lf,  and tab characters
            int cr = 0;
            int lf = 0;
            int tab = 0;

            for (int k=0; k<count; k++) {
                byte c = indata[k];
                if (c == '\r') cr++;
                if (c == '\n') lf++;
                if (c == '\t') tab++;
            }

            // check for trailing eof
            boolean eof = ((count>0) && (indata[count-1] == 0x1A));

            // log stats (before fixes)
            log(srcFile + ": size=" + count + " cr=" + cr +
                        " lf=" + lf + " tab=" + tab + " eof=" + eof,
                        Project.MSG_VERBOSE);

            // determine the output buffer size (slightly pessimisticly)
            int outsize = count;
            if (addcr  !=  0) outsize-=cr;
            if (addcr  == +1) outsize+=lf;
            if (addtab == -1) outsize+=tab*(tablength-1);
            if (ctrlz  == +1) outsize+=1;

            // copy the data
            byte outdata[] = new byte[outsize];
            int o = 0;    // output offset
            int line = o; // beginning of line
            int col = 0;  // desired column

            for (int k=0; k<count; k++) {
                switch (indata[k]) {
                    case (byte)' ':
                        // advance column
                        if (addtab == 0) outdata[o++]=(byte)' ';
                        col++;
                        break;

                    case (byte)'\t':
                        if (addtab == 0) {
                            // treat like any other character
                            outdata[o++]=(byte)'\t';
                            col++;
                        } else {
                            // advance column to next tab stop
                            col = (col|(tablength-1))+1;
                        }
                        break;

                    case (byte)'\r':
                        if (addcr == 0) {
                            // treat like any other character
                            outdata[o++]=(byte)'\r';
                            col++;
                        }
                        break;

                    case (byte)'\n':
                        // start a new line (optional CR followed by LF)
                        if (addcr == +1) outdata[o++]=(byte)'\r';
                        outdata[o++]=(byte)'\n';
                        line=o;
                        col=0;
                        break;

                    default:
                        // add tabs if two or more spaces are required
                        if (addtab>0 && o+1<line+col) {
                            // determine logical column
                            int diff=o-line;

                            // add tabs until this column would be passed
                            // note: the start of line is adjusted to match
                            while ((diff|(tablength-1))<col) {
                                outdata[o++]=(byte)'\t';
                                line-=(tablength-1)-(diff&(tablength-1));
                                diff=o-line;
                            };
                        };

                        // space out to desired column
                        while (o<line+col) outdata[o++]=(byte)' ';

                        // append desired character
                        outdata[o++]=indata[k];
                        col++;
                }
            }

            // add or remove an eof character as required
            if (ctrlz == +1) {
                if (outdata[o-1]!=0x1A) outdata[o++]=0x1A;
            } else if (ctrlz == -1) {
                if (o>2 && outdata[o-1]==0x0A && outdata[o-2]==0x1A) o--;
                if (o>1 && outdata[o-1]==0x1A) o--;
            }

            // output the data
            try {
                File destFile = srcFile;
                if (destDir != null) destFile = new File(destDir, files[i]);
                FileOutputStream outStream = new FileOutputStream(destFile);
                outStream.write(outdata,0,o);
                outStream.close();
            } catch (IOException e) {
                throw new BuildException(e);
            }

        } /* end for */
    }

    /**
     * Enumerated attribute with the values "asis", "add" and "remove".
     */
    public static class AddAsisRemove extends EnumeratedAttribute {
        public String[] getValues() {
            return new String[] {"add", "asis", "remove"};
        }
    }
}
