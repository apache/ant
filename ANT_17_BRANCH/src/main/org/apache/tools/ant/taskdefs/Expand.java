/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Mapper;
import org.apache.tools.ant.types.PatternSet;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.types.resources.Union;
import org.apache.tools.ant.types.selectors.SelectorUtils;
import org.apache.tools.ant.util.FileNameMapper;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.IdentityMapper;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;

/**
 * Unzip a file.
 *
 * @since Ant 1.1
 *
 * @ant.task category="packaging"
 *           name="unzip"
 *           name="unjar"
 *           name="unwar"
 */
public class Expand extends Task {
    private static final int BUFFER_SIZE = 1024;
    private File dest; //req
    private File source; // req
    private boolean overwrite = true;
    private Mapper mapperElement = null;
    private Vector patternsets = new Vector();
    private Union resources = new Union();
    private boolean resourcesSpecified = false;

    private static final String NATIVE_ENCODING = "native-encoding";

    private String encoding = "UTF8";
    /** Error message when more that one mapper is defined */
    public static final String ERROR_MULTIPLE_MAPPERS = "Cannot define more than one mapper";

    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    /**
     * Do the work.
     *
     * @exception BuildException Thrown in unrecoverable error.
     */
    public void execute() throws BuildException {
        if ("expand".equals(getTaskType())) {
            log("!! expand is deprecated. Use unzip instead. !!");
        }

        if (source == null && !resourcesSpecified) {
            throw new BuildException("src attribute and/or resources must be "
                                     + "specified");
        }

        if (dest == null) {
            throw new BuildException(
                "Dest attribute must be specified");
        }

        if (dest.exists() && !dest.isDirectory()) {
            throw new BuildException("Dest must be a directory.", getLocation());
        }

        if (source != null) {
            if (source.isDirectory()) {
                throw new BuildException("Src must not be a directory."
                    + " Use nested filesets instead.", getLocation());
            } else {
                expandFile(FILE_UTILS, source, dest);
            }
        }
        Iterator iter = resources.iterator();
        while (iter.hasNext()) {
            Resource r = (Resource) iter.next();
            if (!r.isExists()) {
                continue;
            }

            if (r instanceof FileResource) {
                expandFile(FILE_UTILS, ((FileResource) r).getFile(), dest);
            } else {
                expandResource(r, dest);
            }
        }
    }

    /**
     * This method is to be overridden by extending unarchival tasks.
     *
     * @param fileUtils the fileUtils
     * @param srcF      the source file
     * @param dir       the destination directory
     */
    protected void expandFile(FileUtils fileUtils, File srcF, File dir) {
        log("Expanding: " + srcF + " into " + dir, Project.MSG_INFO);
        ZipFile zf = null;
        FileNameMapper mapper = getMapper();
        try {
            zf = new ZipFile(srcF, encoding);
            Enumeration e = zf.getEntries();
            while (e.hasMoreElements()) {
                ZipEntry ze = (ZipEntry) e.nextElement();
                extractFile(fileUtils, srcF, dir, zf.getInputStream(ze),
                            ze.getName(), new Date(ze.getTime()),
                            ze.isDirectory(), mapper);
            }

            log("expand complete", Project.MSG_VERBOSE);
        } catch (IOException ioe) {
            throw new BuildException(
                "Error while expanding " + srcF.getPath()
                + "\n" + ioe.toString(),
                ioe);
        } finally {
            ZipFile.closeQuietly(zf);
        }
    }

    /**
     * This method is to be overridden by extending unarchival tasks.
     *
     * @param srcR      the source resource
     * @param dir       the destination directory
     */
    protected void expandResource(Resource srcR, File dir) {
        throw new BuildException("only filesystem based resources are"
                                 + " supported by this task.");
    }

    /**
     * get a mapper for a file
     * @return a filenamemapper for a file
     */
    protected FileNameMapper getMapper() {
        FileNameMapper mapper = null;
        if (mapperElement != null) {
            mapper = mapperElement.getImplementation();
        } else {
            mapper = new IdentityMapper();
        }
        return mapper;
    }

    // CheckStyle:ParameterNumberCheck OFF - bc
    /**
     * extract a file to a directory
     * @param fileUtils             a fileUtils object
     * @param srcF                  the source file
     * @param dir                   the destination directory
     * @param compressedInputStream the input stream
     * @param entryName             the name of the entry
     * @param entryDate             the date of the entry
     * @param isDirectory           if this is true the entry is a directory
     * @param mapper                the filename mapper to use
     * @throws IOException on error
     */
    protected void extractFile(FileUtils fileUtils, File srcF, File dir,
                               InputStream compressedInputStream,
                               String entryName, Date entryDate,
                               boolean isDirectory, FileNameMapper mapper)
                               throws IOException {

        if (patternsets != null && patternsets.size() > 0) {
            String name = entryName.replace('/', File.separatorChar)
                .replace('\\', File.separatorChar);
            boolean included = false;
            Set includePatterns = new HashSet();
            Set excludePatterns = new HashSet();
            for (int v = 0, size = patternsets.size(); v < size; v++) {
                PatternSet p = (PatternSet) patternsets.elementAt(v);
                String[] incls = p.getIncludePatterns(getProject());
                if (incls == null || incls.length == 0) {
                    // no include pattern implicitly means includes="**"
                    incls = new String[] {"**"};
                }

                for (int w = 0; w < incls.length; w++) {
                    String pattern = incls[w].replace('/', File.separatorChar)
                        .replace('\\', File.separatorChar);
                    if (pattern.endsWith(File.separator)) {
                        pattern += "**";
                    }
                    includePatterns.add(pattern);
                }

                String[] excls = p.getExcludePatterns(getProject());
                if (excls != null) {
                    for (int w = 0; w < excls.length; w++) {
                        String pattern = excls[w]
                            .replace('/', File.separatorChar)
                            .replace('\\', File.separatorChar);
                        if (pattern.endsWith(File.separator)) {
                            pattern += "**";
                        }
                        excludePatterns.add(pattern);
                    }
                }
            }

            for (Iterator iter = includePatterns.iterator();
                 !included && iter.hasNext();) {
                String pattern = (String) iter.next();
                included = SelectorUtils.matchPath(pattern, name);
            }

            for (Iterator iter = excludePatterns.iterator();
                 included && iter.hasNext();) {
                String pattern = (String) iter.next();
                included = !SelectorUtils.matchPath(pattern, name);
            }

            if (!included) {
                //Do not process this file
                return;
            }
        }
        String[] mappedNames = mapper.mapFileName(entryName);
        if (mappedNames == null || mappedNames.length == 0) {
            mappedNames = new String[] {entryName};
        }
        File f = fileUtils.resolveFile(dir, mappedNames[0]);
        try {
            if (!overwrite && f.exists()
                && f.lastModified() >= entryDate.getTime()) {
                log("Skipping " + f + " as it is up-to-date",
                    Project.MSG_DEBUG);
                return;
            }

            log("expanding " + entryName + " to " + f,
                Project.MSG_VERBOSE);
            // create intermediary directories - sometimes zip don't add them
            File dirF = f.getParentFile();
            if (dirF != null) {
                dirF.mkdirs();
            }

            if (isDirectory) {
                f.mkdirs();
            } else {
                byte[] buffer = new byte[BUFFER_SIZE];
                int length = 0;
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(f);

                    while ((length =
                            compressedInputStream.read(buffer)) >= 0) {
                        fos.write(buffer, 0, length);
                    }

                    fos.close();
                    fos = null;
                } finally {
                    FileUtils.close(fos);
                }
            }

            fileUtils.setFileLastModified(f, entryDate.getTime());
        } catch (FileNotFoundException ex) {
            log("Unable to expand to file " + f.getPath(), Project.MSG_WARN);
        }

    }
    // CheckStyle:ParameterNumberCheck ON

    /**
     * Set the destination directory. File will be unzipped into the
     * destination directory.
     *
     * @param d Path to the directory.
     */
    public void setDest(File d) {
        this.dest = d;
    }

    /**
     * Set the path to zip-file.
     *
     * @param s Path to zip-file.
     */
    public void setSrc(File s) {
        this.source = s;
    }

    /**
     * Should we overwrite files in dest, even if they are newer than
     * the corresponding entries in the archive?
     * @param b a <code>boolean</code> value
     */
    public void setOverwrite(boolean b) {
        overwrite = b;
    }

    /**
     * Add a patternset.
     * @param set a pattern set
     */
    public void addPatternset(PatternSet set) {
        patternsets.addElement(set);
    }

    /**
     * Add a fileset
     * @param set a file set
     */
    public void addFileset(FileSet set) {
        add(set);
    }

    /**
     * Add a resource collection.
     * @param rc a resource collection.
     * @since Ant 1.7
     */
    public void add(ResourceCollection rc) {
        resourcesSpecified = true;
        resources.add(rc);
    }

    /**
     * Defines the mapper to map source entries to destination files.
     * @return a mapper to be configured
     * @exception BuildException if more than one mapper is defined
     * @since Ant1.7
     */
    public Mapper createMapper() throws BuildException {
        if (mapperElement != null) {
            throw new BuildException(ERROR_MULTIPLE_MAPPERS,
                                     getLocation());
        }
        mapperElement = new Mapper(getProject());
        return mapperElement;
    }

    /**
     * A nested filenamemapper
     * @param fileNameMapper the mapper to add
     * @since Ant 1.6.3
     */
    public void add(FileNameMapper fileNameMapper) {
        createMapper().add(fileNameMapper);
    }


    /**
     * Sets the encoding to assume for file names and comments.
     *
     * <p>Set to <code>native-encoding</code> if you want your
     * platform's native encoding, defaults to UTF8.</p>
     * @param encoding the name of the character encoding
     * @since Ant 1.6
     */
    public void setEncoding(String encoding) {
        if (NATIVE_ENCODING.equals(encoding)) {
            encoding = null;
        }
        this.encoding = encoding;
    }

}
