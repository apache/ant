/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002-2003 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.BuildException;

import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.FileList;

import org.apache.tools.ant.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;

import java.util.Vector; // 1.1
import java.util.Enumeration; // 1.1

/**
 * This class contains the 'concat' task, used to concatenate a series
 * of files into a single stream. The destination of this stream may
 * be the system console, or a file. The following is a sample
 * invocation:
 *
 * <pre>
 * &lt;concat destfile=&quot;${build.dir}/index.xml&quot;
 *   append=&quot;false&quot;&gt;
 *
 *   &lt;fileset dir=&quot;${xml.root.dir}&quot;
 *     includes=&quot;*.xml&quot; /&gt;
 *
 * &lt;/concat&gt;
 * </pre>
 *
 * @author <a href="mailto:derek@activate.net">Derek Slager</a>
 */
public class Concat extends Task {

    // Attributes.

    /**
     * The destination of the stream. If <code>null</code>, the system
     * console is used.
     */
    private File destinationFile = null;

    /**
     * Whether or not the stream should be appended if the destination file 
     * exists.
     * Defaults to <code>false</code>.
     */
    private boolean append = false;

    /**
     * Stores the input file encoding.
     */
    private String encoding = null;

    // Child elements.

    /**
     * This buffer stores the text within the 'concat' element.
     */
    private StringBuffer textBuffer;

    /**
     * Stores a collection of file sets and/or file lists, used to
     * select multiple files for concatenation.
     */
    private Vector fileSets = new Vector(); // 1.1

    // Constructors.

    /**
     * Public, no-argument constructor. Required by Ant.
     */
    public Concat() {}

    // Attribute setters.

    /**
     * Sets the destination file, or uses the console if not specified.
     */
    public void setDestfile(File destinationFile) {
        this.destinationFile = destinationFile;
    }

    /**
     * Sets the behavior when the destination file exists. If set to
     * <code>true</code> the stream data will be appended to the
     * existing file, otherwise the existing file will be
     * overwritten. Defaults to <code>false</code>.
     */
    public void setAppend(boolean append) {
        this.append = append;
    }

    /**
     * Sets the encoding for the input files, used when displaying the
     * data via the console.
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    // Nested element creators.

    /**
     * Set of files to concatenate.
     */
    public void addFileset(FileSet set) {
        fileSets.addElement(set);
    }

    /**
     * List of files to concatenate.
     */
    public void addFilelist(FileList list) {
        fileSets.addElement(list);
    }

    /**
     * This method adds text which appears in the 'concat' element.
     */
    public void addText(String text) {
        if (textBuffer == null) {
            // Initialize to the size of the first text fragment, with
            // the hopes that it's the only one.
            textBuffer = new StringBuffer(text.length());
        }

        // Append the fragment -- we defer property replacement until
        // later just in case we get a partial property in a fragment.
        textBuffer.append(text);
    }

    /**
     * This method performs the concatenation.
     */
    public void execute() 
        throws BuildException {

        // treat empty nested text as no text
        sanitizeText();

        // Sanity check our inputs.
        if (fileSets.size() == 0 && textBuffer == null) {
            // Nothing to concatenate!
            throw new BuildException("At least one file " + 
                                     "must be provided, or " + 
                                     "some text.");
        }

        // If using filesets, disallow inline text. This is similar to
        // using GNU 'cat' with file arguments -- stdin is simply
        // ignored.
        if (fileSets.size() > 0 && textBuffer != null) {
            throw new BuildException("Cannot include inline text " + 
                                     "when using filesets.");
        }

        boolean savedAppend = append;
        try {
            // Iterate the FileSet collection, concatenating each file as
            // it is encountered.
            for (Enumeration e = fileSets.elements(); e.hasMoreElements();) {
                
                // Root directory for files.
                File fileSetBase = null;
                
                // List of files.
                String[] srcFiles = null;
                
                // Get the next file set, which could be a FileSet or a
                // FileList instance.
                Object next = e.nextElement();
                
                if (next instanceof FileSet) {
                    
                    FileSet fileSet = (FileSet) next;
                    
                    // Get a directory scanner from the file set, which will
                    // determine the files from the set which need to be
                    // concatenated.
                    DirectoryScanner scanner = 
                        fileSet.getDirectoryScanner(getProject());
                    
                    // Determine the root path.
                    fileSetBase = fileSet.getDir(getProject());
                    
                    // Get the list of files.
                    srcFiles = scanner.getIncludedFiles();
                    
                } else if (next instanceof FileList) {
                    
                    FileList fileList = (FileList) next;
                    
                    // Determine the root path.
                    fileSetBase = fileList.getDir(getProject());
                    
                    // Get the list of files.
                    srcFiles = fileList.getFiles(getProject());
                    
                }

                // Concatenate the files.
                if (srcFiles != null) {
                    catFiles(fileSetBase, srcFiles);
                } else {
                    log("Warning: Concat received empty fileset.", 
                        Project.MSG_WARN);
                }
            }
        } finally {
            append = savedAppend;
        }
        
        // Now, cat the inline text, if applicable.
        catText();
    }

    /**
     * Reset state to default.
     */
    public void reset() {
        append = false;
        destinationFile = null;
        encoding = null;
        fileSets = new Vector();
    }

    /**
     * This method concatenates a series of files to a single
     * destination.
     *
     * @param base the base directory for the list of file names.
     *
     * @param files the names of the files to be concatenated,
     * relative to the <code>base</code>.
     */
    private void catFiles(File base, String[] files) {

        // First, create a list of absolute paths for the input files.
        Vector inputFileNames = new Vector();
        for (int i = 0; i < files.length; i++) {

            File current = new File(base, files[i]);

            // Make sure the file exists. This will rarely fail when
            // using file sets, but it could be rather common when
            // using file lists.
            if (!current.exists()) {
                // File does not exist, log an error and continue.
                log("File " + current + " does not exist.", 
                    Project.MSG_ERR);
                continue;
            }

            inputFileNames.addElement(current.getAbsolutePath());
        }

        final int len = inputFileNames.size();
        if (len == 0) {
            log("Warning: Could not find any of the files specified " +
                "in concat task.", Project.MSG_WARN);
            return;
        }

        String[] input = new String[len];
        inputFileNames.copyInto(input);

        // Next, perform the concatenation.
        if (encoding == null) {
            OutputStream os = null;
            InputStream is = null;

            try {

                if (destinationFile == null) {
                    // Log using WARN so it displays in 'quiet' mode.
                    os = new LogOutputStream(this, Project.MSG_WARN);
                } else {
                    os = 
                        new FileOutputStream(destinationFile.getAbsolutePath(),
                                             append);
                    
                    // This flag should only be recognized for the first
                    // file. In the context of a single 'cat', we always
                    // want to append.
                    append = true;
                }
            
                for (int i = 0; i < len; i++) {

                    // Make sure input != output.
                    if (destinationFile != null &&
                        destinationFile.getAbsolutePath().equals(input[i])) {
                        throw new BuildException("Input file \"" 
                            + destinationFile.getName() 
                            + "\" is the same as the output file.");
                    }

                    is = new FileInputStream(input[i]);
                    byte[] buffer = new byte[8192];
                    while (true) {
                        int bytesRead = is.read(buffer);
                        if (bytesRead == -1) { // EOF
                            break;
                        }
                        
                        // Write the read data.
                        os.write(buffer, 0, bytesRead);
                    }
                    os.flush();
                    is.close();
                    is = null;
                }
            } catch (IOException ioex) {
                throw new BuildException("Error while concatenating: "
                                         + ioex.getMessage(), ioex);
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (Exception ignore) {}
                }
                if (os != null) {
                    try {
                        os.close();
                    } catch (Exception ignore) {}
                }
            }

        } else { // user specified encoding

            Writer out = null;
            BufferedReader in = null;

            try {
                if (destinationFile == null) {
                    // Log using WARN so it displays in 'quiet' mode.
                    out = new OutputStreamWriter(
                              new LogOutputStream(this, Project.MSG_WARN));
                } else {
                    out = new OutputStreamWriter(
                              new FileOutputStream(destinationFile
                                                   .getAbsolutePath(),
                                                   append),
                              encoding);
                    
                    // This flag should only be recognized for the first
                    // file. In the context of a single 'cat', we always
                    // want to append.
                    append = true;
                }

                for (int i = 0; i < len; i++) {
                    in = new BufferedReader(
                            new InputStreamReader(new FileInputStream(input[i]), 
                                encoding));

                    String line;
                    char[] buffer = new char[4096];
                    while (true) {
                        int charsRead = in.read(buffer);
                        if (charsRead == -1) { // EOF
                            break;
                        }
                        
                        // Write the read data.
                        out.write(buffer, 0, charsRead);
                    }
                    out.flush();
                    in.close();
                    in = null;
                }
            } catch (IOException ioe) {
                throw new BuildException("Error while concatenating: " 
                                         + ioe.getMessage(), ioe);
            } finally {
                // Close resources.
                if (in != null) {
                    try {
                        in.close();
                    } catch (Exception ignore) {}
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (Exception ignore) {}
                }
            }
        }
    }

    /**
     * This method concatenates the text which was added inside the
     * 'concat' tags. If the text between the tags consists only of
     * whitespace characters, it is ignored.
     */
    private void catText() {

        // Check the buffer.
        if (textBuffer == null) {
            // No text to write.
            return;
        }

        String text = textBuffer.toString();

        // Replace ${property} strings.
        text = ProjectHelper.replaceProperties(getProject(), text,
                                               getProject().getProperties());

        // Set up a writer if necessary.
        FileWriter writer = null;
        if (destinationFile != null) {
            try {
                writer = new FileWriter(destinationFile.getAbsolutePath(), 
                                        append);
            } catch (IOException ioe) {
                throw new BuildException("Error creating destination " + 
                                         "file.", ioe);
            }
        }

        // Reads the text, line by line.
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new StringReader(text));

            String line;
            while ((line = reader.readLine()) != null) {
                if (destinationFile == null) {
                    // Log the line, using WARN so it displays in
                    // 'quiet' mode.
                    log(line, Project.MSG_WARN);
                } else {
                    writer.write(line);
                    writer.write(StringUtils.LINE_SEP);
                    writer.flush();
                }
            }

        } catch (IOException ioe) {
            throw new BuildException("Error while concatenating " + 
                                     "text.", ioe);
        } finally {
            // Close resources.
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ignore) {}
            }

            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception ignore) {}
            }
        }
    }

    /**
     * Treat empty nested text as no text.
     *
     * <p>Depending on the XML parser, addText may have been called
     * for &quot;ignorable whitespace&quot; as well.</p>
     */
    private void sanitizeText() {
        if (textBuffer != null) {
            if (textBuffer.toString().trim().length() == 0) {
                textBuffer = null;
            }
        }
    }

}
