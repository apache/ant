/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.condition.Condition;
import org.apache.tools.ant.types.FileSet;

/**
 * Used to create or verify file checksums.
 *
 * @author Magesh Umasankar
 *
 * @since Ant 1.5
 *
 * @ant.task category="control"
 */
public class Checksum extends MatchingTask implements Condition {
    /**
     * File for which checksum is to be calculated.
     */
    private File file = null;
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
     * Whether or not to create a new file.
     * Defaults to <code>false</code>.
     */
    private boolean forceOverwrite;
    /**
     * Contains the result of a checksum verification. ("true" or "false")
     */
    private String verifyProperty;
    /**
     * Vector to hold source file sets.
     */
    private Vector filesets = new Vector();
    /**
     * Stores SourceFile, DestFile pairs and SourceFile, Property String pairs.
     */
    private Hashtable includeFileMap = new Hashtable();
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
    private int readBufferSize = 8 * 1024;

    /**
     * Sets the file for which the checksum is to be calculated.
     */
    public void setFile(File file) {
        this.file = file;
    }

    /**
     * Specifies the algorithm to be used to compute the checksum.
     * Defaults to "MD5". Other popular algorithms like "SHA" may be used as well.
     */
    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    /**
     * Sets the MessageDigest algorithm provider to be used
     * to calculate the checksum.
     */
    public void setProvider(String provider) {
        this.provider = provider;
    }

    /**
     * Sets the file extension that is be to used to
     * create or identify destination file.
     */
    public void setFileext(String fileext) {
        this.fileext = fileext;
    }

    /**
     * Sets the property to hold the generated checksum.
     */
    public void setProperty(String property) {
        this.property = property;
    }

    /**
     * Sets the verify property.  This project property holds
     * the result of a checksum verification - "true" or "false"
     */
    public void setVerifyproperty(String verifyProperty) {
        this.verifyProperty = verifyProperty;
    }

    /**
     * Whether or not to overwrite existing file irrespective of 
     * whether it is newer than
     * the source file.  Defaults to false.
     */
    public void setForceOverwrite(boolean forceOverwrite) {
        this.forceOverwrite = forceOverwrite;
    }

    /**
     * The size of the read buffer to use.
     */
    public void setReadBufferSize(int size) {
        this.readBufferSize = size;
    }

    /**
     * Files to generate checksums for.
     */
    public void addFileset(FileSet set) {
        filesets.addElement(set);
    }

    /**
     * Calculate the checksum(s).
     */
    public void execute() throws BuildException {
        isCondition = false;
        boolean value = validateAndExecute();
        if (verifyProperty != null) {
            project.setNewProperty(verifyProperty,
                                new Boolean(value).toString());
        }
    }

    /**
     * Calculate the checksum(s)
     *
     * @return Returns true if the checksum verification test passed,
     * false otherwise.
     */
    public boolean eval() throws BuildException {
        isCondition = true;
        return validateAndExecute();
    }

    /**
     * Validate attributes and get down to business.
     */
    private boolean validateAndExecute() throws BuildException {
        String savedFileExt = fileext;

        if (file == null && filesets.size() == 0) {
            throw new BuildException(
                "Specify at least one source - a file or a fileset.");
        }

        if (file != null && file.exists() && file.isDirectory()) {
            throw new BuildException(
                "Checksum cannot be generated for directories");
        }

        if (property != null && fileext != null) {
            throw new BuildException(
                "Property and FileExt cannot co-exist.");
        }

        if (property != null) {
            if (forceOverwrite) {
                throw new BuildException(
                    "ForceOverwrite cannot be used when Property is specified");
            }

            if (file != null) {
                if (filesets.size() > 0) {
                    throw new BuildException("Multiple files cannot be used " 
                        + "when Property is specified");
                }
            } else {
                if (filesets.size() > 1) {
                    throw new BuildException("Multiple files cannot be used " 
                        + "when Property is specified");
                }
            }
        }

        if (verifyProperty != null) {
            isCondition = true;
        }

        if (verifyProperty != null && forceOverwrite) {
            throw new BuildException(
                "VerifyProperty and ForceOverwrite cannot co-exist.");
        }

        if (isCondition && forceOverwrite) {
            throw new BuildException("ForceOverwrite cannot be used when " 
                + "conditions are being used.");
        }

        messageDigest = null;
        if (provider != null) {
            try {
                messageDigest = MessageDigest.getInstance(algorithm, provider);
            } catch (NoSuchAlgorithmException noalgo) {
                throw new BuildException(noalgo, location);
            } catch (NoSuchProviderException noprovider) {
                throw new BuildException(noprovider, location);
            }
        } else {
            try {
                messageDigest = MessageDigest.getInstance(algorithm);
            } catch (NoSuchAlgorithmException noalgo) {
                throw new BuildException(noalgo, location);
            }
        }

        if (messageDigest == null) {
            throw new BuildException("Unable to create Message Digest",
                location);
        }

        if (fileext == null) {
            fileext = "." + algorithm;
        } else if (fileext.trim().length() == 0) {
            throw new BuildException(
                "File extension when specified must not be an empty string");
        }

        try {
            addToIncludeFileMap(file);
            
            int sizeofFileSet = filesets.size();
            for (int i = 0; i < sizeofFileSet; i++) {
                FileSet fs = (FileSet) filesets.elementAt(i);
                DirectoryScanner ds = fs.getDirectoryScanner(project);
                String[] srcFiles = ds.getIncludedFiles();
                for (int j = 0; j < srcFiles.length; j++) {
                    File src = new File(fs.getDir(project), srcFiles[j]);
                    addToIncludeFileMap(src);
                }
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
        if (file != null) {
            if (file.exists()) {
                if (property == null) {
                    File dest 
                        = new File(file.getParent(), file.getName() + fileext);
                    if (forceOverwrite || isCondition ||
                        (file.lastModified() > dest.lastModified())) {
                        includeFileMap.put(file, dest);
                    } else {
                        log(file + " omitted as " + dest + " is up to date.",
                            Project.MSG_VERBOSE);
                    }
                } else {
                    includeFileMap.put(file, property);
                }
            } else {
                String message = "Could not find file "
                                 + file.getAbsolutePath()
                                 + " to generate checksum for.";
                log(message);
                throw new BuildException(message, location);
            }
        }
    }

    /**
     * Generate checksum(s) using the message digest created earlier.
     */
    private boolean generateChecksums() throws BuildException {
        boolean checksumMatches = true;
        FileInputStream fis = null;
        FileOutputStream fos = null;
        byte[] buf = new byte[readBufferSize];
        try {
            for (Enumeration e = includeFileMap.keys(); e.hasMoreElements();) {
                messageDigest.reset();
                File src = (File) e.nextElement();
                if (!isCondition) {
                    log("Calculating " + algorithm + " checksum for " + src);
                }
                fis = new FileInputStream(src);
                DigestInputStream dis = new DigestInputStream(fis,
                                                              messageDigest);
                while (dis.read(buf, 0, readBufferSize) != -1) {
                    ;
                }
                dis.close();
                fis.close();
                fis = null;
                byte[] fileDigest = messageDigest.digest ();
                StringBuffer checksumSb = new StringBuffer();
                for (int i = 0; i < fileDigest.length; i++) {
                    String hexStr = Integer.toHexString(0x00ff & fileDigest[i]);
                    if (hexStr.length() < 2) {
                        checksumSb.append("0");
                    }
                    checksumSb.append(hexStr);
                }
                String checksum = checksumSb.toString();
                //can either be a property name string or a file
                Object destination = includeFileMap.get(src);
                if (destination instanceof java.lang.String) {
                    String prop = (String) destination;
                    if (isCondition) {
                        checksumMatches = checksumMatches &&
                            checksum.equals(property);
                    } else {
                        project.setNewProperty(prop, checksum);
                    }
                } else if (destination instanceof java.io.File) {
                    if (isCondition) {
                        File existingFile = (File) destination;
                        if (existingFile.exists()) {
                            fis = new FileInputStream(existingFile);
                            InputStreamReader isr = new InputStreamReader(fis);
                            BufferedReader br = new BufferedReader(isr);
                            String suppliedChecksum = br.readLine();
                            fis.close();
                            fis = null;
                            br.close();
                            isr.close();
                            checksumMatches = checksumMatches &&
                                checksum.equals(suppliedChecksum);
                        } else {
                            checksumMatches = false;
                        }
                    } else {
                        File dest = (File) destination;
                        fos = new FileOutputStream(dest);
                        fos.write(checksum.getBytes());
                        fos.close();
                        fos = null;
                    }
                }
            }
        } catch (Exception e) {
            throw new BuildException(e, location);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {}
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {}
            }
        }
        return checksumMatches;
    }
}
