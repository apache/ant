/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000 The Apache Software Foundation.  All rights
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

/**
 * jlink.java
 * links together multiple .jar files
 * 
 * Original code by Patrick Beard. Modifications to work
 * with ANT by Matthew Kuperus Heun.
 *
 * @author <a href="mailto:beard@netscape.com>Patrick C. Beard</a>.
 * @author <a href="mailto:matthew.k.heun@gaerospace.com>Matthew Kuperus Heun</a>
 */
package org.apache.tools.ant.taskdefs.optional.jlink;

import java.io .*;
import java.util .Enumeration;
import java.util .Vector;
import java.util.zip .*;

public class jlink extends Object{

    /**
     * The file that will be created by this instance of jlink.
     */
    public  void setOutfile( String outfile ) {
        if ( outfile == null ) {
            return ;
        }
        this .outfile = outfile;
    }

    /**
     * Adds a file to be merged into the output.
     */
    public  void addMergeFile( String mergefile ) {
        if ( mergefile == null ) {
            return ;
        }
        mergefiles .addElement( mergefile );
    }

    /**
     * Adds a file to be added into the output.
     */
    public  void addAddFile( String addfile ) {
        if ( addfile == null ) {
            return ;
        }
        addfiles .addElement( addfile );
    }

    /**
     * Adds several files to be merged into the output.
     */
    public  void addMergeFiles( String[] mergefiles ) {
        if ( mergefiles == null ) {
            return ;
        }
        for ( int i = 0; i < mergefiles .length; i++ ) {
            addMergeFile( mergefiles[i] );
        }
    }

    /**
     * Adds several file to be added into the output.
     */
    public  void addAddFiles( String[] addfiles ) {
        if ( addfiles == null ) {
            return ;
        }
        for ( int i = 0; i < addfiles .length; i++ ) {
            addAddFile( addfiles[i] );
        }
    }

    /**
     * Determines whether output will be compressed.
     */
    public  void setCompression( boolean compress ) {
        this .compression = compress;
    }

    /**
     * Performs the linking of files.
     * Addfiles are added to the output as-is. For example, a 
     * jar file is added to the output as a jar file.
     * However, mergefiles are first examined for their type.
     * If it is a jar or zip file, the contents will be extracted
     * from the mergefile and entered into the output.
     * If a zip or jar file is encountered in a subdirectory
     * it will be added, not merged.
     * If a directory is encountered, it becomes the root
     * entry of all the files below it.  Thus, you can
     * provide multiple, disjoint directories, as
     * addfiles: they will all be added in a rational 
     * manner to outfile.
     */
    public  void link() throws Exception {
        ZipOutputStream output = new ZipOutputStream( new FileOutputStream( outfile ) );
        if ( compression ) {
            output .setMethod( ZipOutputStream .DEFLATED );
            output .setLevel( Deflater .DEFAULT_COMPRESSION );
        } else {
            output .setMethod( ZipOutputStream .STORED );
        }
        Enumeration merges = mergefiles .elements();
        while ( merges .hasMoreElements() ) {
            String path = (String) merges .nextElement();
            File f = new File( path );
            if ( f.getName().endsWith( ".jar" ) || f.getName().endsWith( ".zip" ) ) {
                                //Do the merge
                mergeZipJarContents( output, f );
            }
            else {
                //Add this file to the addfiles Vector and add it 
                //later at the top level of the output file.
                addAddFile( path );
            }
        }
        Enumeration adds = addfiles .elements();
        while ( adds .hasMoreElements() ) {
            String name = (String) adds .nextElement();
            File f = new File( name );
            if ( f .isDirectory() ) {
                //System.out.println("in jlink: adding directory contents of " + f.getPath());
                addDirContents( output, f, f.getName() + '/', compression );
            }
            else {
                addFile( output, f, "", compression );
            }
        }
        if ( output != null ) {
            try  {
                output .close();
            } catch( IOException ioe ) {}
        }
    }

    public static  void main( String[] args ) {
        // jlink output input1 ... inputN
        if ( args .length < 2 ) {
            System .out .println( "usage: jlink output input1 ... inputN" );
            System .exit( 1 );
        }
        jlink linker = new jlink();
        linker .setOutfile( args[0] );
        //To maintain compatibility with the command-line version, we will only add files to be merged.
        for ( int i = 1; i < args .length; i++ ) {
            linker .addMergeFile( args[i] );
        }
        try  {
            linker .link();
        } catch( Exception ex ) {
            System .err .print( ex .getMessage() );
        }
    }

    /*
     * Actually performs the merging of f into the output.
     * f should be a zip or jar file.
     */
    private void mergeZipJarContents( ZipOutputStream output, File f ) throws IOException {
        //Check to see that the file with name "name" exists.
        if ( ! f .exists() ) {
            return ;
        }
        ZipFile zipf = new ZipFile( f );
        Enumeration entries = zipf.entries();
        while (entries.hasMoreElements()){
            ZipEntry inputEntry = (ZipEntry) entries.nextElement();
            //Ignore manifest entries.  They're bound to cause conflicts between
            //files that are being merged.  User should supply their own
            //manifest file when doing the merge.
            String inputEntryName = inputEntry.getName();
            int index = inputEntryName.indexOf("META-INF");
            if (index < 0){
                //META-INF not found in the name of the entry. Go ahead and process it.
                try {
                    output.putNextEntry(processEntry(zipf, inputEntry));
                } catch (ZipException ex){
                    //If we get here, it could be because we are trying to put a
                    //directory entry that already exists.
                    //For example, we're trying to write "com", but a previous
                    //entry from another mergefile was called "com".
                    //In that case, just ignore the error and go on to the
                    //next entry.
                    String mess = ex.getMessage();
                    if (mess.indexOf("duplicate") >= 0){
                                //It was the duplicate entry.
                        continue;
                    } else {
                                //I hate to admit it, but we don't know what happened here.  Throw the Exception.
                        throw ex;
                    }
                }
                InputStream in = zipf.getInputStream(inputEntry);
                int len = buffer.length;
                int count = -1;
                while ((count = in.read(buffer, 0, len)) > 0){
                    output.write(buffer, 0, count);
                }
                in.close();
                output.closeEntry();
            }
        }
        zipf .close();
    }

    /*
     * Adds contents of a directory to the output.
     */
    private void addDirContents( ZipOutputStream output, File dir, String prefix, boolean compress ) throws IOException {
        String[] contents = dir .list();
        for ( int i = 0; i < contents .length; ++i ) {
            String name = contents[i];
            File file = new File( dir, name );
            if ( file .isDirectory() ) {
                addDirContents( output, file, prefix + name + '/', compress );
            }
            else {
                addFile( output, file, prefix, compress );
            }
        }
    }

    /*
     * Gets the name of an entry in the file.  This is the real name
     * which for a class is the name of the package with the class
     * name appended.
     */
    private String getEntryName( File file, String prefix ) {
        String name = file .getName();
        if ( ! name .endsWith( ".class" ) ) {
            // see if the file is in fact a .class file, and determine its actual name.
            try  {
                InputStream input = new FileInputStream( file );
                String className = ClassNameReader .getClassName( input );
                input .close();
                if ( className != null ) {
                    return className .replace( '.', '/' ) + ".class";
                }
            } catch( IOException ioe ) {}
        }
        System.out.println("From " + file.getPath() + " and prefix " + prefix + ", creating entry " + prefix+name);
        return (prefix + name);
    }

    /*
     * Adds a file to the output stream.
     */
    private void addFile( ZipOutputStream output, File file, String prefix, boolean compress) throws IOException {
        //Make sure file exists
        long checksum = 0;
        if ( ! file .exists() ) {
            return ;
        }
        ZipEntry entry = new ZipEntry( getEntryName( file, prefix ) );
        entry .setTime( file .lastModified() );
        entry .setSize( file .length() );
        if (! compress){
            entry.setCrc(calcChecksum(file));
        }
        FileInputStream input = new FileInputStream( file );
        addToOutputStream(output, input, entry);
    }
        
    /*
     * A convenience method that several other methods might call.
     */
    private void addToOutputStream(ZipOutputStream output, InputStream input, ZipEntry ze) throws IOException{
        try {
            output.putNextEntry(ze);            
        } catch (ZipException zipEx) {
            //This entry already exists. So, go with the first one.
            input.close();
            return;
        }
        int numBytes = -1;
        while((numBytes = input.read(buffer)) > 0){
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
    private ZipEntry processEntry( ZipFile zip, ZipEntry inputEntry ) throws IOException{
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
        String name = inputEntry .getName();
        if ( ! (inputEntry .isDirectory() || name .endsWith( ".class" )) ) {
            try  {
                InputStream input = zip.getInputStream( zip .getEntry( name ) );
                String className = ClassNameReader .getClassName( input );
                input .close();
                if ( className != null ) {
                    name = className .replace( '.', '/' ) + ".class";
                }
            } catch( IOException ioe ) {}
        }
        ZipEntry outputEntry = new ZipEntry( name );
        outputEntry.setTime(inputEntry .getTime() );
        outputEntry.setExtra(inputEntry.getExtra());
        outputEntry.setComment(inputEntry.getComment());
        outputEntry.setTime(inputEntry.getTime());
        if (compression){
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
        return calcChecksum(in, f.length());
    }

    /*
     * Necessary in the case where you add a entry that
     * is not compressed.
     */
    private long calcChecksum(InputStream in, long size) throws IOException{
        CRC32 crc = new CRC32();
        int len = buffer.length;
        int count = -1;
        int haveRead = 0; 
        while((count=in.read(buffer, 0, len)) > 0){
            haveRead += count;
            crc.update(buffer, 0, count);
        }
        in.close();
        return crc.getValue();
    }

    private  String outfile = null;

    private  Vector mergefiles = new Vector( 10 );

    private  Vector addfiles = new Vector( 10 );

    private  boolean compression = false;
        
    byte[] buffer = new byte[8192];

}


