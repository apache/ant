/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.tools.ant.taskdefs.optional.image;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Locale;
import java.util.Vector;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Mapper;
import org.apache.tools.ant.types.optional.image.Draw;
import org.apache.tools.ant.types.optional.image.ImageOperation;
import org.apache.tools.ant.types.optional.image.Rotate;
import org.apache.tools.ant.types.optional.image.Scale;
import org.apache.tools.ant.types.optional.image.TransformOperation;
import org.apache.tools.ant.util.FileNameMapper;
import org.apache.tools.ant.util.IdentityMapper;
import org.apache.tools.ant.util.StringUtils;

import com.sun.media.jai.codec.FileSeekableStream;

/**
 * A MatchingTask which relies on <a
 * href="https://www.oracle.com/technetwork/java/javasebusiness/downloads/java-archive-downloads-java-client-419417.html">JAI (Java
 * Advanced Imaging)</a> to perform image manipulation operations on
 * existing images.  The operations are represented as ImageOperation
 * DataType objects.  The operations are arranged to conform to the
 * Chaining Model of JAI.  Check out the JAI Programming Guide.
 *
 * @deprecated JAI is not developed any more. Internal APIs that JAI depends on were
 * scheduled for removal in Java 7 and finally removed in Java 9.
 * @see org.apache.tools.ant.types.optional.image.ImageOperation
 * @see org.apache.tools.ant.types.DataType
 */
@Deprecated
public class Image extends MatchingTask {
    // CheckStyle:VisibilityModifier OFF - bc
    protected Vector<ImageOperation> instructions = new Vector<>();
    protected boolean overwrite = false;
    protected Vector<FileSet> filesets = new Vector<>();
    protected File srcDir = null;
    protected File destDir = null;

    // CheckStyle:MemberNameCheck OFF - bc

    //cannot remove underscores due to protected visibility >:(
    protected String str_encoding = "JPEG";
    protected boolean garbage_collect = false;

    private boolean failonerror = true;

    // CheckStyle:MemberNameCheck ON

    // CheckStyle:VisibilityModifier ON

    private Mapper mapperElement = null;

    /**
     * Add a set of files to be deleted.
     * @param set the FileSet to add.
     */
    public void addFileset(FileSet set) {
        filesets.add(set);
    }

    /**
     * Set whether to fail on error.
     * If false, note errors to the output but keep going.
     * @param failonerror true or false.
     */
    public void setFailOnError(boolean failonerror) {
        this.failonerror = failonerror;
    }

    /**
     * Set the source dir to find the image files.
     * @param srcDir the directory in which the image files reside.
     */
    public void setSrcdir(File srcDir) {
        this.srcDir = srcDir;
    }

    /**
     * Set the image encoding type. 
     * See the encoding table in the JAI Programming Guide.
     * @param encoding the String image encoding.
     */
    public void setEncoding(String encoding) {
        str_encoding = encoding;
    }

    /**
     * Set whether to overwrite a file if there is a naming conflict.
     * @param overwrite whether to overwrite.
     */
    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    /**
     * Set whether to invoke Garbage Collection after each image processed.
     * Defaults to false.
     * @param gc whether to invoke the garbage collector.
     */
    public void setGc(boolean gc) {
        garbage_collect = gc;
    }

    /**
     * Set the destination directory for manipulated images.
     * @param destDir The destination directory.
     */
    public void setDestDir(File destDir) {
        this.destDir = destDir;
    }

    /**
     * Add an ImageOperation to chain.
     * @param instr The ImageOperation to append to the chain.
     */
    public void addImageOperation(ImageOperation instr) {
        instructions.add(instr);
    }

    /**
     * Add a Rotate ImageOperation to the chain.
     * @param instr The Rotate operation to add to the chain.
     * @see org.apache.tools.ant.types.optional.image.Rotate
     */
    public void addRotate(Rotate instr) {
        instructions.add(instr);
    }

    /**
     * Add a Scale ImageOperation to the chain.
     * @param instr The Scale operation to add to the chain.
     * @see org.apache.tools.ant.types.optional.image.Scale
     */
    public void addScale(Scale instr) {
        instructions.add(instr);
    }

    /**
     * Add a Draw ImageOperation to the chain.  DrawOperation
     * DataType objects can be nested inside the Draw object.
     * @param instr The Draw operation to add to the chain.
     * @see org.apache.tools.ant.types.optional.image.Draw
     * @see org.apache.tools.ant.types.optional.image.DrawOperation
     */
    public void addDraw(Draw instr) {
        instructions.add(instr);
    }

    /**
    * Add an ImageOperation to chain.
    * @param instr The ImageOperation to append to the chain.
    * @since Ant 1.7
    */
    public void add(ImageOperation instr) {
        addImageOperation(instr);
    }

    /**
     * Defines the mapper to map source to destination files.
     * @return a mapper to be configured
     * @exception BuildException if more than one mapper is defined
     * @since Ant 1.8.0
     */
    public Mapper createMapper() throws BuildException {
        if (mapperElement != null) {
            throw new BuildException("Cannot define more than one mapper",
                                     getLocation());
        }
        mapperElement = new Mapper(getProject());
        return mapperElement;
    }

    /**
     * Add a nested filenamemapper.
     * @param fileNameMapper the mapper to add.
     * @since Ant 1.8.0
     */
    public void add(FileNameMapper fileNameMapper) {
        createMapper().add(fileNameMapper);
    }

    /**
     * Executes all the chained ImageOperations on the files inside
     * the directory.
     * @param srcDir File
     * @param srcNames String[]
     * @param dstDir File
     * @param mapper FileNameMapper
     * @return int
     * @since Ant 1.8.0
     */
    public int processDir(final File srcDir, final String[] srcNames,
                          final File dstDir, final FileNameMapper mapper) {
        int writeCount = 0;

        for (final String srcName : srcNames) {
            final File srcFile = new File(srcDir, srcName).getAbsoluteFile();

            final String[] dstNames = mapper.mapFileName(srcName);
            if (dstNames == null) {
                log(srcFile + " skipped, don't know how to handle it",
                    Project.MSG_VERBOSE);
                continue;
            }

            for (String dstName : dstNames) {
                final File dstFile = new File(dstDir, dstName).getAbsoluteFile();

                if (dstFile.exists()) {
                    // avoid overwriting unless necessary
                    if (!overwrite
                       && srcFile.lastModified() <= dstFile.lastModified()) {

                        log(srcFile + " omitted as " + dstFile
                            + " is up to date.", Project.MSG_VERBOSE);

                        // don't overwrite the file
                        continue;
                    }

                    // avoid extra work while overwriting
                    if (!srcFile.equals(dstFile)) {
                        dstFile.delete();
                    }
                }
                processFile(srcFile, dstFile);
                ++writeCount;
            }
        }

        // run the garbage collector if wanted
        if (garbage_collect) {
            System.gc();
        }

        return writeCount;
    }

    /**
     * Executes all the chained ImageOperations on the file
     * specified.
     * @param file The file to be processed.
     * @deprecated this method isn't used anymore
     */
    @Deprecated
    public void processFile(File file) {
        processFile(file, new File(destDir == null
                                   ? srcDir : destDir, file.getName()));
    }

    /**
     * Executes all the chained ImageOperations on the file
     * specified.
     * @param file The file to be processed.
     * @param newFile The file to write to.
     * @since Ant 1.8.0
     */
    public void processFile(File file, File newFile) {
        try {
            log("Processing File: " + file.getAbsolutePath());

            PlanarImage image = null;
            try (FileSeekableStream input = new FileSeekableStream(file)) {
                image = JAI.create("stream", input);
                for (ImageOperation instr : instructions) {
                    if (instr instanceof TransformOperation) {
                        image = ((TransformOperation) instr)
                            .executeTransformOperation(image);
                    } else {
                        log("Not a TransformOperation: " + instr);
                    }
                }
            }

            File dstParent = newFile.getParentFile();
            if (!dstParent.isDirectory()
                && !(dstParent.mkdirs() || dstParent.isDirectory())) {
                throw new BuildException("Failed to create parent directory %s",
                    dstParent);
            }

            if (overwrite && newFile.exists() && !newFile.equals(file)) {
                newFile.delete();
            }

            try (OutputStream stream = Files.newOutputStream(newFile.toPath())) {
                JAI.create("encode", image, stream,
                    str_encoding.toUpperCase(Locale.ENGLISH), null);
                stream.flush();
            }
        } catch (IOException | RuntimeException err) {
            if (!file.equals(newFile)) {
                newFile.delete();
            }
            if (!failonerror) {
                log("Error processing file:  " + err);
            } else {
                throw new BuildException(err);
            }
        }
    }

    /**
     * Executes the Task.
     * @throws BuildException on error.
     */
    @Override
    public void execute() throws BuildException {

        validateAttributes();

        try {
            File dest = (destDir != null) ? destDir : srcDir;

            int writeCount = 0;

            // build mapper
            final FileNameMapper mapper = mapperElement == null
                ? new IdentityMapper() : mapperElement.getImplementation();

            // deal with specified srcDir
            if (srcDir != null) {
                writeCount += processDir(srcDir,
                    super.getDirectoryScanner(srcDir).getIncludedFiles(), dest,
                    mapper);
            }
            // deal with the filesets
            for (FileSet fs : filesets) {
                writeCount += processDir(fs.getDir(),
                    fs.getDirectoryScanner().getIncludedFiles(),
                    dest, mapper);
            }

            if (writeCount > 0) {
                log("Processed " + writeCount + (writeCount == 1 ? " image." : " images."));
            }

        } catch (Exception err) {
            log(StringUtils.getStackTrace(err), Project.MSG_ERR);
            throw new BuildException(err.getMessage());
        }
    }

    /**
     * Ensure we have a consistent and legal set of attributes, and set
     * any internal flags necessary based on different combinations
     * of attributes.
     * @throws BuildException on error.
     */
    protected void validateAttributes() throws BuildException {
        if (srcDir == null && filesets.isEmpty()) {
            throw new BuildException(
                "Specify at least one source--a srcDir or a fileset.");
        }
        if (srcDir == null && destDir == null) {
            throw new BuildException("Specify the destDir, or the srcDir.");
        }
        if ("jpg".equalsIgnoreCase(str_encoding)) {
            str_encoding = "JPEG";
        } else if ("tif".equalsIgnoreCase(str_encoding)) {
            str_encoding = "TIFF";
        }
    }
}

