/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant.taskdefs.optional.image;

import com.sun.media.jai.codec.FileSeekableStream;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.optional.image.Draw;
import org.apache.tools.ant.types.optional.image.ImageOperation;
import org.apache.tools.ant.types.optional.image.Rotate;
import org.apache.tools.ant.types.optional.image.Scale;
import org.apache.tools.ant.types.optional.image.TransformOperation;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Vector;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * A MatchingTask which relies on <A HREF="http://java.sun.com/products/java-media/jai">JAI (Java Advanced Imaging)</A>
 * to perform image manipulation operations on existing images.  The
 * operations are represented as ImageOperation DataType objects.
 * The operations are arranged to conform to the Chaining Model
 * of JAI.
 * Check out the <A HREF="http://java.sun.com/products/java-media/jai/forDevelopers/jai1_0_1guide-unc/">JAI Programming Guide</A>
 *
 * @see org.apache.tools.ant.types.optional.image.ImageOperation
 * @see org.apache.tools.ant.types.DataType
 * @author <a href="mailto:kzgrey@ntplx.net">Kevin Z Grey</a>
 * @author <a href="mailto:dep4b@yahoo.com">Eric Pugh</a>
 */
public class Image extends MatchingTask {
    protected Vector instructions = new Vector();
    protected String str_encoding = "JPEG";
    protected boolean overwrite = false;
    protected boolean garbage_collect = false;
    private boolean failonerror = true;
    protected Vector filesets = new Vector();


    protected File srcDir = null;
    protected File destDir = null;

   /**
     * Adds a set of files to be deleted.
     */
    public void addFileset(FileSet set) {
        filesets.addElement(set);
    }

    /**
     * If false, note errors to the output but keep going.
     * @param failonerror true or false
     */
     public void setFailOnError(boolean failonerror) {
         this.failonerror = failonerror;
     }

    /**
     * Set the source dir to find the image files.
     */
    public void setSrcdir(File srcDir) {
        this.srcDir = srcDir;
    }

    /**
     * Set the image encoding type.  <A HREF="http://java.sun.com/products/java-media/jai/forDevelopers/jai1_0_1guide-unc/Encode.doc.html#56610">See this table in the JAI Programming Guide</A>.
     */
    public void setEncoding(String encoding) {
        str_encoding = encoding;
    }

    /**
     *  Sets whether or not to overwrite a file if there is a naming conflict.
     */
    public void setOverwrite(boolean overwrite) {
            this.overwrite = overwrite;
    }

    /**
     *  Enables Garbage Collection after each image processed.  Defaults to false.
     */
    public void setGc(boolean gc) {
            garbage_collect = gc;
    }


    /**
     * Sets the destination directory for manipulated images.
     * @param destination The destination directory
     */
    public void setDestDir(String destination) {
        destDir = new File(destination);
    }

    /**
     * Adds an ImageOperation to chain.
     * @param instr The ImageOperation to append to the chain
     */
    public void addImageOperation(ImageOperation instr) {
        instructions.add(instr);
    }

    /**
     * Adds a Rotate ImageOperation to the chain
     * @param instr The Rotate operation to add to the chain
     * @see org.apache.tools.ant.types.optional.image.Rotate
     */
    public void addRotate(Rotate instr) {
        instructions.add(instr);
    }

    /**
     * Adds a Scale ImageOperation to the chain
     * @param instr The Scale operation to add to the chain
     * @see org.apache.tools.ant.types.optional.image.Scale
     */
    public void addScale(Scale instr) {
        instructions.add(instr);
    }

    /**
     * Adds a Draw ImageOperation to the chain.  DrawOperation
     * DataType objects can be nested inside the Draw object
     * @param instr The Draw operation to add to the chain
     * @see org.apache.tools.ant.types.optional.image.Draw
     * @see org.apache.tools.ant.types.optional.image.DrawOperation
     */
    public void addDraw(Draw instr) {
        instructions.add(instr);
    }

    /**
     * Executes all the chained ImageOperations on the file
     * specified.
     * @param file The file to be processed
     */
    public void processFile(File file) {
        try {
            log("Processing File: " + file.getAbsolutePath());
            FileSeekableStream input = new FileSeekableStream(file);
            PlanarImage image = JAI.create("stream", input);
            for (int i = 0; i < instructions.size(); i++) {
                Object instr = instructions.elementAt(i);
                if (instr instanceof TransformOperation) {
                    image = ((TransformOperation) instr).executeTransformOperation(image);
                } else {
                    log("Not a TransformOperation: " + instr);
                }
            }
            input.close();


            log("Encoding As " + str_encoding);

            if (str_encoding.toLowerCase().equals("jpg")) {
                str_encoding = "JPEG";
            } else if (str_encoding.toLowerCase().equals("tif")) {
                str_encoding = "TIFF";
            }

            if (destDir == null){
              destDir = srcDir;
            }

            File new_file = new File(destDir.getAbsolutePath() + File.separator + file.getName());

            if ((overwrite && new_file.exists()) && (!new_file.equals(file))) {
                new_file.delete();
            }
            else if (!overwrite && new_file.exists()){
              return;
            }

            FileOutputStream stream = new FileOutputStream(new_file);

            JAI.create("encode", image, stream, str_encoding.toUpperCase(), null);
            stream.flush();
            stream.close();


        } catch (IOException err) {
           if (!failonerror) {
             log("Error processing file:  " + err);
           } else {
             throw new BuildException(err);
           }
        }

        catch (java.lang.RuntimeException rerr) {
           if (!failonerror) {
             log("Error processing file:  " + rerr);
           } else {
             throw new BuildException(rerr);
           }
        }

    }

    /**
     * Executes the Task
     */
    public void execute() {

        validateAttributes();

        try {
            DirectoryScanner ds = null;
            String [] files =null;
            ArrayList filesList = new ArrayList();


            // deal with specified srcDir
            if (srcDir != null){
              ds = super.getDirectoryScanner(srcDir);

              files = ds.getIncludedFiles();
              for (int i = 0; i < files.length; i++){
                filesList.add(new File(srcDir.getAbsolutePath() + File.separator + files[i]));
              }
            }
            // deal with the filesets
            for (int i = 0; i < filesets.size(); i++) {
                FileSet fs = (FileSet) filesets.elementAt(i);
                ds = fs.getDirectoryScanner(getProject());
                files =ds.getIncludedFiles();
                File fromDir = fs.getDir(getProject());
                for (int j = 0; j < files.length; j++){
                  filesList.add(new File(fromDir.getAbsolutePath() + File.separator + files[j]));
                }
            }
            // iterator through all the files and process them.
            for (Iterator i = filesList.iterator();i.hasNext();){
              File file = (File)i.next();

              processFile(file);
              if (garbage_collect) {
                  System.gc();
              }
            }


        } catch (Exception err) {
            err.printStackTrace();
            throw new BuildException(err.getMessage());
        }
    }


    /**
     * Ensure we have a consistent and legal set of attributes, and set
     * any internal flags necessary based on different combinations
     * of attributes.
     */
    protected void validateAttributes() throws BuildException {
        if (srcDir == null && filesets.size() == 0) {
            throw new BuildException("Specify at least one source "
                                     + "- a srcDir or a fileset.");
        }

        if (srcDir == null && destDir == null) {
            throw new BuildException("Specify the destDir, or the srcDir.");
        }


    }

}

