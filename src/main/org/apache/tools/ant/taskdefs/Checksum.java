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
package org.apache.tools.ant.taskdefs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.condition.Condition;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.FileProvider;
import org.apache.tools.ant.types.resources.Restrict;
import org.apache.tools.ant.types.resources.Union;
import org.apache.tools.ant.types.resources.selectors.Type;
import org.apache.tools.ant.util.FileUtils;

/**
 * Used to create or verify file checksums.
 *
 * @since Ant 1.5
 *
 * @ant.task category="control"
 */
public class Checksum extends MatchingTask implements Condition {

    private static final int NIBBLE = 4;
    private static final int WORD = 16;
    private static final int BUFFER_SIZE = 8 * 1024;
    private static final int BYTE_MASK = 0xFF;

    private static class FileUnion extends Restrict {
        private Union u;
        FileUnion() {
            u = new Union();
            super.add(u);
            super.add(Type.FILE);
        }
        @Override
        public void add(ResourceCollection rc) {
            u.add(rc);
        }
    }

    /**
     * File for which checksum is to be calculated.
     */
    private File file = null;

    /**
     * Root directory in which the checksum files will be written.
     * If not specified, the checksum files will be written
     * in the same directory as each file.
     */
    private File todir;

    /**
     * MessageDigest algorithm to be used.
     */
    private String algorithm = "MD5";
    /**
     * MessageDigest Algorithm provider
     */
    private String provider = null;
    /**
     * File Extension that is be to used to create or identify
     * destination file
     */
    private String fileext;
    /**
     * Holds generated checksum and gets set as a Project Property.
     */
    private String property;
    /**
     * Holds checksums for all files (both calculated and cached on disk).
     * Key:   java.util.File (source file)
     * Value: java.lang.String (digest)
     */
    private Map<File, byte[]> allDigests = new HashMap<>();
    /**
     * Holds relative file names for all files (always with a forward slash).
     * This is used to calculate the total hash.
     * Key:   java.util.File (source file)
     * Value: java.lang.String (relative file name)
     */
    private Map<File, String> relativeFilePaths = new HashMap<>();
    /**
     * Property where totalChecksum gets set.
     */
    private String totalproperty;
    /**
     * Whether or not to create a new file.
     * Defaults to <code>false</code>.
     */
    private boolean forceOverwrite;
    /**
     * Contains the result of a checksum verification. ("true" or "false")
     */
    private String verifyProperty;
    /**
     * Resource Collection.
     */
    private FileUnion resources = null;
    /**
     * Stores SourceFile, DestFile pairs and SourceFile, Property String pairs.
     */
    private Hashtable<File, Object> includeFileMap = new Hashtable<>();
    /**
     * Message Digest instance
     */
    private MessageDigest messageDigest;
    /**
     * is this task being used as a nested condition element?
     */
    private boolean isCondition;
    /**
     * Size of the read buffer to use.
     */
    private int readBufferSize = BUFFER_SIZE;

    /**
     * Formatter for the checksum file.
     */
    private MessageFormat format = FormatElement.getDefault().getFormat();

    /**
     * Sets the file for which the checksum is to be calculated.
     * @param file a <code>File</code> value
     */
    public void setFile(File file) {
        this.file = file;
    }

    /**
     * Sets the root directory where checksum files will be
     * written/read
     * @param todir the directory to write to
     * @since Ant 1.6
     */
    public void setTodir(File todir) {
        this.todir = todir;
    }

    /**
     * Specifies the algorithm to be used to compute the checksum.
     * Defaults to "MD5". Other popular algorithms like "SHA" may be used as well.
     * @param algorithm a <code>String</code> value
     */
    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    /**
     * Sets the MessageDigest algorithm provider to be used
     * to calculate the checksum.
     * @param provider a <code>String</code> value
     */
    public void setProvider(String provider) {
        this.provider = provider;
    }

    /**
     * Sets the file extension that is be to used to
     * create or identify destination file.
     * @param fileext a <code>String</code> value
     */
    public void setFileext(String fileext) {
        this.fileext = fileext;
    }

    /**
     * Sets the property to hold the generated checksum.
     * @param property a <code>String</code> value
     */
    public void setProperty(String property) {
        this.property = property;
    }

    /**
     * Sets the property to hold the generated total checksum
     * for all files.
     * @param totalproperty a <code>String</code> value
     *
     * @since Ant 1.6
     */
    public void setTotalproperty(String totalproperty) {
        this.totalproperty = totalproperty;
    }

    /**
     * Sets the verify property.  This project property holds
     * the result of a checksum verification - "true" or "false"
     * @param verifyProperty a <code>String</code> value
     */
    public void setVerifyproperty(String verifyProperty) {
        this.verifyProperty = verifyProperty;
    }

    /**
     * Whether or not to overwrite existing file irrespective of
     * whether it is newer than
     * the source file.  Defaults to false.
     * @param forceOverwrite a <code>boolean</code> value
     */
    public void setForceOverwrite(boolean forceOverwrite) {
        this.forceOverwrite = forceOverwrite;
    }

    /**
     * The size of the read buffer to use.
     * @param size an <code>int</code> value
     */
    public void setReadBufferSize(int size) {
        this.readBufferSize = size;
    }

    /**
     * Select the in/output pattern via a well know format name.
     * @param e an <code>enumerated</code> value
     *
     * @since 1.7.0
     */
    public void setFormat(FormatElement e) {
        format = e.getFormat();
    }

    /**
     * Specify the pattern to use as a MessageFormat pattern.
     *
     * <p>{0} gets replaced by the checksum, {1} by the filename.</p>
     * @param pattern a <code>String</code> value
     *
     * @since 1.7.0
     */
    public void setPattern(String pattern) {
        format = new MessageFormat(pattern);
    }

    /**
     * Files to generate checksums for.
     * @param set a fileset of files to generate checksums for.
     */
    public void addFileset(FileSet set) {
        add(set);
    }

    /**
     * Add a resource collection.
     * @param rc the ResourceCollection to add.
     */
    public void add(ResourceCollection rc) {
        if (rc == null) {
            return;
        }
        resources = (resources == null) ? new FileUnion() : resources;
        resources.add(rc);
    }

    /**
     * Calculate the checksum(s).
     * @throws BuildException on error
     */
    @Override
    public void execute() throws BuildException {
        isCondition = false;
        boolean value = validateAndExecute();
        if (verifyProperty != null) {
            getProject().setNewProperty(verifyProperty,
                Boolean.toString(value));
        }
    }

    /**
     * Calculate the checksum(s)
     *
     * @return Returns true if the checksum verification test passed,
     * false otherwise.
     * @throws BuildException on error
     */
    @Override
    public boolean eval() throws BuildException {
        isCondition = true;
        return validateAndExecute();
    }

    /**
     * Validate attributes and get down to business.
     */
    private boolean validateAndExecute() throws BuildException {
        String savedFileExt = fileext;

        if (file == null && (resources == null || resources.size() == 0)) {
            throw new BuildException(
                "Specify at least one source - a file or a resource collection.");
        }
        if (resources != null && !resources.isFilesystemOnly()) {
            throw new BuildException("Can only calculate checksums for file-based resources.");
        }
        if (file != null && file.exists() && file.isDirectory()) {
            throw new BuildException("Checksum cannot be generated for directories");
        }
        if (file != null && totalproperty != null) {
            throw new BuildException("File and Totalproperty cannot co-exist.");
        }
        if (property != null && fileext != null) {
            throw new BuildException("Property and FileExt cannot co-exist.");
        }
        if (property != null) {
            if (forceOverwrite) {
                throw new BuildException(
                    "ForceOverwrite cannot be used when Property is specified");
            }
            int ct = 0;
            if (resources != null) {
                ct += resources.size();
            }
            if (file != null) {
                ct++;
            }
            if (ct > 1) {
                throw new BuildException(
                    "Multiple files cannot be used when Property is specified");
            }
        }
        if (verifyProperty != null) {
            isCondition = true;
        }
        if (verifyProperty != null && forceOverwrite) {
            throw new BuildException("VerifyProperty and ForceOverwrite cannot co-exist.");
        }
        if (isCondition && forceOverwrite) {
            throw new BuildException(
                "ForceOverwrite cannot be used when conditions are being used.");
        }
        messageDigest = null;
        if (provider != null) {
            try {
                messageDigest = MessageDigest.getInstance(algorithm, provider);
            } catch (NoSuchAlgorithmException | NoSuchProviderException noalgo) {
                throw new BuildException(noalgo, getLocation());
            }
        } else {
            try {
                messageDigest = MessageDigest.getInstance(algorithm);
            } catch (NoSuchAlgorithmException noalgo) {
                throw new BuildException(noalgo, getLocation());
            }
        }
        if (messageDigest == null) {
            throw new BuildException("Unable to create Message Digest", getLocation());
        }
        if (fileext == null) {
            fileext = "." + algorithm;
        } else if (fileext.trim().isEmpty()) {
            throw new BuildException("File extension when specified must not be an empty string");
        }
        try {
            if (resources != null) {
                for (Resource r : resources) {
                    File src = r.as(FileProvider.class).getFile();
                    if (totalproperty != null || todir != null) {
                        // Use '/' to calculate digest based on file name.
                        // This is required in order to get the same result
                        // on different platforms.
                        relativeFilePaths.put(src, r.getName().replace(File.separatorChar, '/'));
                    }
                    addToIncludeFileMap(src);
                }
            }
            if (file != null) {
                if (totalproperty != null || todir != null) {
                    relativeFilePaths.put(
                        file, file.getName().replace(File.separatorChar, '/'));
                }
                addToIncludeFileMap(file);
            }
            return generateChecksums();
        } finally {
            fileext = savedFileExt;
            includeFileMap.clear();
        }
    }

    /**
     * Add key-value pair to the hashtable upon which
     * to later operate upon.
     */
    private void addToIncludeFileMap(File file) throws BuildException {
        if (file.exists()) {
            if (property == null) {
                File checksumFile = getChecksumFile(file);
                if (forceOverwrite || isCondition
                    || (file.lastModified() > checksumFile.lastModified())) {
                    includeFileMap.put(file, checksumFile);
                } else {
                    log(file + " omitted as " + checksumFile + " is up to date.",
                        Project.MSG_VERBOSE);
                    if (totalproperty != null) {
                        // Read the checksum from disk.
                        String checksum = readChecksum(checksumFile);
                        byte[] digest = decodeHex(checksum.toCharArray());
                        allDigests.put(file, digest);
                    }
                }
            } else {
                includeFileMap.put(file, property);
            }
        } else {
            String message = "Could not find file "
                + file.getAbsolutePath()
                + " to generate checksum for.";
            log(message);
            throw new BuildException(message, getLocation());
        }
    }

    private File getChecksumFile(File file) {
        File directory;
        if (todir != null) {
            // A separate directory was explicitly declared
            String path = getRelativeFilePath(file);
            directory = new File(todir, path).getParentFile();
            // Create the directory, as it might not exist.
            directory.mkdirs();
        } else {
            // Just use the same directory as the file itself.
            // This directory will exist
            directory = file.getParentFile();
        }
        return new File(directory, file.getName() + fileext);
    }

    /**
     * Generate checksum(s) using the message digest created earlier.
     */
    private boolean generateChecksums() throws BuildException {
        boolean checksumMatches = true;
        InputStream fis = null;
        OutputStream fos = null;
        byte[] buf = new byte[readBufferSize];
        try {
            for (Map.Entry<File, Object> e : includeFileMap.entrySet()) {
                messageDigest.reset();
                File src = e.getKey();
                if (!isCondition) {
                    log("Calculating " + algorithm + " checksum for " + src, Project.MSG_VERBOSE);
                }
                fis = Files.newInputStream(src.toPath());
                DigestInputStream dis = new DigestInputStream(fis,
                                                              messageDigest);
                while (dis.read(buf, 0, readBufferSize) != -1) {
                    // Empty statement
                }
                dis.close();
                fis.close();
                fis = null;
                byte[] fileDigest = messageDigest.digest();
                if (totalproperty != null) {
                    allDigests.put(src, fileDigest);
                }
                String checksum = createDigestString(fileDigest);
                //can either be a property name string or a file
                Object destination = e.getValue();
                if (destination instanceof String) {
                    String prop = (String) destination;
                    if (isCondition) {
                        checksumMatches
                            = checksumMatches && checksum.equals(property);
                    } else {
                        getProject().setNewProperty(prop, checksum);
                    }
                } else if (destination instanceof File) {
                    if (isCondition) {
                        File existingFile = (File) destination;
                        if (existingFile.exists()) {
                            try {
                                String suppliedChecksum =
                                    readChecksum(existingFile);
                                checksumMatches = checksumMatches
                                    && checksum.equals(suppliedChecksum);
                            } catch (BuildException be) {
                                // file is on wrong format, swallow
                                checksumMatches = false;
                            }
                        } else {
                            checksumMatches = false;
                        }
                    } else {
                        File dest = (File) destination;
                        fos = Files.newOutputStream(dest.toPath());
                        fos.write(format.format(new Object[] {
                                                    checksum,
                                                    src.getName(),
                                                    FileUtils
                                                    .getRelativePath(dest
                                                                     .getParentFile(),
                                                                     src),
                                                    FileUtils
                                                    .getRelativePath(getProject()
                                                                     .getBaseDir(),
                                                                     src),
                                                    src.getAbsolutePath()
                                                }).getBytes());
                        fos.write(System.lineSeparator().getBytes());
                        fos.close();
                        fos = null;
                    }
                }
            }
            if (totalproperty != null) {
                // Calculate the total checksum
                // Convert the keys (source files) into a sorted array.
                File[] keyArray = allDigests.keySet().toArray(new File[0]);
                // File is Comparable, but sort-order is platform
                // dependent (case-insensitive on Windows)
                Arrays.sort(keyArray, Comparator.nullsFirst(
                    Comparator.comparing(this::getRelativeFilePath)));

                // Loop over the checksums and generate a total hash.
                messageDigest.reset();
                for (File src : keyArray) {
                    // Add the digest for the file content
                    byte[] digest = allDigests.get(src);
                    messageDigest.update(digest);

                    // Add the file path
                    String fileName = getRelativeFilePath(src);
                    messageDigest.update(fileName.getBytes());
                }
                String totalChecksum = createDigestString(messageDigest.digest());
                getProject().setNewProperty(totalproperty, totalChecksum);
            }
        } catch (Exception e) {
            throw new BuildException(e, getLocation());
        } finally {
            FileUtils.close(fis);
            FileUtils.close(fos);
        }
        return checksumMatches;
    }

    private String createDigestString(byte[] fileDigest) {
        StringBuilder checksumSb = new StringBuilder();
        for (byte digestByte : fileDigest) {
            checksumSb.append(String.format("%02x", BYTE_MASK & digestByte));
        }
        return checksumSb.toString();
    }

    /**
     * Converts an array of characters representing hexadecimal values into an
     * array of bytes of those same values. The returned array will be half the
     * length of the passed array, as it takes two characters to represent any
     * given byte. An exception is thrown if the passed char array has an odd
     * number of elements.
     *
     * NOTE: This code is copied from jakarta-commons codec.
     * @param data an array of characters representing hexadecimal values
     * @return the converted array of bytes
     * @throws BuildException on error
     */
    public static byte[] decodeHex(char[] data) throws BuildException {
        int l = data.length;

        if ((l & 0x01) != 0) {
            throw new BuildException("odd number of characters.");
        }

        byte[] out = new byte[l >> 1];

        // two characters form the hex value.
        for (int i = 0, j = 0; j < l; i++) {
            int f = Character.digit(data[j++], WORD) << NIBBLE;
            f |= Character.digit(data[j++], WORD);
            out[i] = (byte) (f & BYTE_MASK);
        }

        return out;
    }

    /**
     * reads the checksum from a file using the specified format.
     *
     * @since 1.7
     */
    private String readChecksum(File f) {
        try (BufferedReader diskChecksumReader =
            new BufferedReader(new FileReader(f))) {
            Object[] result = format.parse(diskChecksumReader.readLine());
            if (result == null || result.length == 0 || result[0] == null) {
                throw new BuildException("failed to find a checksum");
            }
            return (String) result[0];
        } catch (IOException | ParseException e) {
            throw new BuildException("Couldn't read checksum file " + f, e);
        }
    }

    /**
     * @since Ant 1.8.2
     */
    private String getRelativeFilePath(File f) {
        String path = relativeFilePaths.get(f);
        if (path == null) {
            //bug 37386. this should not occur, but it has, once.
            throw new BuildException(
                "Internal error: relativeFilePaths could not match file %s\nplease file a bug report on this",
                f);
        }
        return path;
    }

    /**
     * Helper class for the format attribute.
     *
     * @since 1.7
     */
    public static class FormatElement extends EnumeratedAttribute {
        private static HashMap<String, MessageFormat> formatMap = new HashMap<>();
        private static final String CHECKSUM = "CHECKSUM";
        private static final String MD5SUM = "MD5SUM";
        private static final String SVF = "SVF";

        static {
            formatMap.put(CHECKSUM, new MessageFormat("{0}"));
            formatMap.put(MD5SUM, new MessageFormat("{0} *{1}"));
            formatMap.put(SVF, new MessageFormat("MD5 ({1}) = {0}"));
        }

        /**
         * Get the default value - CHECKSUM.
         * @return the defaul value.
         */
        public static FormatElement getDefault() {
            FormatElement e = new FormatElement();
            e.setValue(CHECKSUM);
            return e;
        }

        /**
         * Convert this enumerated type to a <code>MessageFormat</code>.
         * @return a <code>MessageFormat</code> object.
         */
        public MessageFormat getFormat() {
            return formatMap.get(getValue());
        }

        /**
         * Get the valid values.
         * @return an array of values.
         */
        @Override
        public String[] getValues() {
            return new String[] {CHECKSUM, MD5SUM, SVF};
        }
    }
}
