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
/**
 * jlink.java links together multiple .jar files Original code by Patrick
 * Beard. Modifications to work with ANT by Matthew Kuperus Heun.
 *
 */
package org.apache.tools.ant.taskdefs.optional.jlink;

import org.apache.tools.ant.util.FileUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Vector;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

// CheckStyle:TypeNameCheck OFF - bc
/**
 * jlink links together multiple .jar files.
 */
public class jlink {
    private static final int BUFFER_SIZE = 8192;
    private static final int VECTOR_INIT_SIZE = 10;

    private String outfile = null;

    private Vector mergefiles = new Vector(VECTOR_INIT_SIZE);

    private Vector addfiles = new Vector(VECTOR_INIT_SIZE);

    private boolean compression = false;

    // CheckStyle:VisibilityModifier OFF - bc

    byte[] buffer = new byte[BUFFER_SIZE];

    // CheckStyle:VisibilityModifier OFF - bc

    /** The file that will be created by this instance of jlink.
     * @param outfile the file to create.
     */
    public void setOutfile(String outfile) {
        if (outfile == null) {
            return;
        }
        this.outfile = outfile;
    }


    /**
     * Adds a file to be merged into the output.
     * @param fileToMerge the file to merge into the output.
     */
    public void addMergeFile(String fileToMerge) {
        if (fileToMerge == null) {
            return;
        }
        mergefiles.addElement(fileToMerge);
    }


    /** Adds a file to be added into the output.
     * @param fileToAdd the file to add to the output.
     */
    public void addAddFile(String fileToAdd) {
        if (fileToAdd == null) {
            return;
        }
        addfiles.addElement(fileToAdd);
    }


    /**
     * Adds several files to be merged into the output.
     * @param filesToMerge an array of files to merge into the output.
     */
    public void addMergeFiles(String[] filesToMerge) {
        if (filesToMerge == null) {
            return;
        }
        for (int i = 0; i < filesToMerge.length; i++) {
            addMergeFile(filesToMerge[i]);
        }
    }


    /**
     * Adds several file to be added into the output.
     * @param filesToAdd an array of files to add to the output.
     */
    public void addAddFiles(String[] filesToAdd) {
        if (filesToAdd == null) {
            return;
        }
        for (int i = 0; i < filesToAdd.length; i++) {
            addAddFile(filesToAdd[i]);
        }
    }


    /**
     * Determines whether output will be compressed.
     * @param compress if true use compression.
     */
    public void setCompression(boolean compress) {
        this.compression = compress;
    }


    /**
     * Performs the linking of files. Addfiles are added to the output as-is.
     * For example, a jar file is added to the output as a jar file. However,
     * mergefiles are first examined for their type. If it is a jar or zip
     * file, the contents will be extracted from the mergefile and entered
     * into the output. If a zip or jar file is encountered in a subdirectory
     * it will be added, not merged. If a directory is encountered, it becomes
     * the root entry of all the files below it. Thus, you can provide
     * multiple, disjoint directories, as addfiles: they will all be added in
     * a rational manner to outfile.
     * @throws Exception on error.
     */
    public void link() throws Exception {
        ZipOutputStream output = new ZipOutputStream(new FileOutputStream(outfile));

        if (compression) {
            output.setMethod(ZipOutputStream.DEFLATED);
            output.setLevel(Deflater.DEFAULT_COMPRESSION);
        } else {
            output.setMethod(ZipOutputStream.STORED);
        }

        Enumeration merges = mergefiles.elements();

        while (merges.hasMoreElements()) {
            String path = (String) merges.nextElement();
            File f = new File(path);

            if (f.getName().endsWith(".jar") || f.getName().endsWith(".zip")) {
                //Do the merge
                mergeZipJarContents(output, f);
            } else {
                //Add this file to the addfiles Vector and add it
                //later at the top level of the output file.
                addAddFile(path);
            }
        }

        Enumeration adds = addfiles.elements();

        while (adds.hasMoreElements()) {
            String name = (String) adds.nextElement();
            File f = new File(name);

            if (f.isDirectory()) {
                //System.out.println("in jlink: adding directory contents of " + f.getPath());
                addDirContents(output, f, f.getName() + '/', compression);
            } else {
                addFile(output, f, "", compression);
            }
        }
        FileUtils.close(output);
    }


    /**
     * The command line entry point for jlink.
     * @param args an array of arguments
     */
    public static void main(String[] args) {
        // jlink output input1 ... inputN
        if (args.length < 2) {
            System.out.println("usage: jlink output input1 ... inputN");
            System.exit(1);
        }
        jlink linker = new jlink();

        linker.setOutfile(args[0]);
        // To maintain compatibility with the command-line version,
        // we will only add files to be merged.
        for (int i = 1; i < args.length; i++) {
            linker.addMergeFile(args[i]);
        }
        try {
            linker.link();
        } catch (Exception ex) {
            System.err.print(ex.getMessage());
        }
    }


    /*
     * Actually performs the merging of f into the output.
     * f should be a zip or jar file.
     */
    private void mergeZipJarContents(ZipOutputStream output, File f) throws IOException {
        //Check to see that the file with name "name" exists.
        if (!f.exists()) {
            return;
        }
        ZipFile zipf = new ZipFile(f);
        Enumeration entries = zipf.entries();

        while (entries.hasMoreElements()) {
            ZipEntry inputEntry = (ZipEntry) entries.nextElement();
            //Ignore manifest entries.  They're bound to cause conflicts between
            //files that are being merged.  User should supply their own
            //manifest file when doing the merge.
            String inputEntryName = inputEntry.getName();
            int index = inputEntryName.indexOf("META-INF");

            if (index < 0) {
                //META-INF not found in the name of the entry. Go ahead and process it.
                try {
                    output.putNextEntry(processEntry(zipf, inputEntry));
                } catch (ZipException ex) {
                    //If we get here, it could be because we are trying to put a
                    //directory entry that already exists.
                    //For example, we're trying to write "com", but a previous
                    //entry from another mergefile was called "com".
                    //In that case, just ignore the error and go on to the
                    //next entry.
                    String mess = ex.getMessage();

                    if (mess.indexOf("duplicate") >= 0) {
                        //It was the duplicate entry.
                        continue;
                    } else {
                        // I hate to admit it, but we don't know what happened
                        // here.  Throw the Exception.
                        throw ex;
                    }
                }

                InputStream in = zipf.getInputStream(inputEntry);
                int len = buffer.length;
                int count = -1;

                while ((count = in.read(buffer, 0, len)) > 0) {
                    output.write(buffer, 0, count);
                }
                in.close();
                output.closeEntry();
            }
        }
        zipf.close();
    }


    /*
     * Adds contents of a directory to the output.
     */
    private void addDirContents(ZipOutputStream output, File dir, String prefix,
                                boolean compress) throws IOException {
        String[] contents = dir.list();

        for (int i = 0; i < contents.length; ++i) {
            String name = contents[i];
            File file = new File(dir, name);

            if (file.isDirectory()) {
                addDirContents(output, file, prefix + name + '/', compress);
            } else {
                addFile(output, file, prefix, compress);
            }
        }
    }


    /*
     * Gets the name of an entry in the file.  This is the real name
     * which for a class is the name of the package with the class
     * name appended.
     */
    private String getEntryName(File file, String prefix) {
        String name = file.getName();

        if (!name.endsWith(".class")) {
            // see if the file is in fact a .class file, and determine its actual name.
            InputStream input = null;
            try {
                input = new FileInputStream(file);
                String className = ClassNameReader.getClassName(input);

                if (className != null) {
                    return className.replace('.', '/') + ".class";
                }
            } catch (IOException ioe) {
                //do nothing
            } finally {
                if (input != null) {
                    try {
                        input.close();
                    } catch (IOException e) {
                        //do nothing
                    }
                }
            }
        }
        System.out.println("From " + file.getPath() + " and prefix " + prefix
                           + ", creating entry " + prefix + name);
        return (prefix + name);
    }


    /*
     * Adds a file to the output stream.
     */
    private void addFile(ZipOutputStream output, File file, String prefix,
                         boolean compress) throws IOException {
        //Make sure file exists
        if (!file.exists()) {
            return;
        }
        ZipEntry entry = new ZipEntry(getEntryName(file, prefix));

        entry.setTime(file.lastModified());
        entry.setSize(file.length());
        if (!compress) {
            entry.setCrc(calcChecksum(file));
        }
        FileInputStream input = new FileInputStream(file);

        addToOutputStream(output, input, entry);
    }


    /*
     * A convenience method that several other methods might call.
     */
    private void addToOutputStream(ZipOutputStream output, InputStream input,
                                   ZipEntry ze) throws IOException {
        try {
            output.putNextEntry(ze);
        } catch (ZipException zipEx) {
            //This entry already exists. So, go with the first one.
            input.close();
            return;
        }

        int numBytes = -1;

        while ((numBytes = input.read(buffer)) > 0) {
            output.write(buffer, 0, numBytes);
        }
        output.closeEntry();
        input.close();
    }


    /*
     * A method that does the work on a given entry in a mergefile.
     * The big deal is to set the right parameters in the ZipEntry
     * on the output stream.
     */
    private ZipEntry processEntry(ZipFile zip, ZipEntry inputEntry) {
        /*
          First, some notes.
          On MRJ 2.2.2, getting the size, compressed size, and CRC32 from the
          ZipInputStream does not work for compressed (deflated) files.  Those calls return -1.
          For uncompressed (stored) files, those calls do work.
          However, using ZipFile.getEntries() works for both compressed and
          uncompressed files.

          Now, from some simple testing I did, it seems that the value of CRC-32 is
          independent of the compression setting. So, it should be easy to pass this
          information on to the output entry.
        */
        String name = inputEntry.getName();

        if (!(inputEntry.isDirectory() || name.endsWith(".class"))) {
            try {
                InputStream input = zip.getInputStream(zip.getEntry(name));
                String className = ClassNameReader.getClassName(input);

                input.close();
                if (className != null) {
                    name = className.replace('.', '/') + ".class";
                }
            } catch (IOException ioe) {
                //do nothing
            }
        }
        ZipEntry outputEntry = new ZipEntry(name);

        outputEntry.setTime(inputEntry.getTime());
        outputEntry.setExtra(inputEntry.getExtra());
        outputEntry.setComment(inputEntry.getComment());
        outputEntry.setTime(inputEntry.getTime());
        if (compression) {
            outputEntry.setMethod(ZipEntry.DEFLATED);
            //Note, don't need to specify size or crc for compressed files.
        } else {
            outputEntry.setMethod(ZipEntry.STORED);
            outputEntry.setCrc(inputEntry.getCrc());
            outputEntry.setSize(inputEntry.getSize());
        }
        return outputEntry;
    }


    /*
     * Necessary in the case where you add a entry that
     * is not compressed.
     */
    private long calcChecksum(File f) throws IOException {
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(f));

        return calcChecksum(in);
    }


    /*
     * Necessary in the case where you add a entry that
     * is not compressed.
     */
    private long calcChecksum(InputStream in) throws IOException {
        CRC32 crc = new CRC32();
        int len = buffer.length;
        int count = -1;
        int haveRead = 0;

        while ((count = in.read(buffer, 0, len)) > 0) {
            haveRead += count;
            crc.update(buffer, 0, count);
        }
        in.close();
        return crc.getValue();
    }


}


