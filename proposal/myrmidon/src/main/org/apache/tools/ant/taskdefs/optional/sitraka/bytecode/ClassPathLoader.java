/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional.sitraka.bytecode;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Core of the bytecode analyzer. It loads classes from a given classpath.
 *
 * @author <a href="sbailliez@imediation.com">Stephane Bailliez</a>
 */
public class ClassPathLoader
{

    public final static FileLoader NULL_LOADER = new NullLoader();

    /**
     * the list of files to look for
     */
    protected File[] files;

    /**
     * create a new instance with a given classpath. It must be urls separated
     * by the platform specific path separator.
     *
     * @param classPath the classpath to load all the classes from.
     */
    public ClassPathLoader( String classPath )
    {
        StringTokenizer st = new StringTokenizer( classPath, File.pathSeparator );
        Vector entries = new Vector();
        while( st.hasMoreTokens() )
        {
            File file = new File( st.nextToken() );
            entries.addElement( file );
        }
        files = new File[entries.size()];
        entries.copyInto( files );
    }

    /**
     * create a new instance with a given set of urls.
     *
     * @param entries valid file urls (either .jar, .zip or directory)
     */
    public ClassPathLoader( String[] entries )
    {
        files = new File[entries.length];
        for( int i = 0; i < entries.length; i++ )
        {
            files[i] = new File( entries[i] );
        }
    }

    /**
     * create a new instance with a given set of urls
     *
     * @param entries file urls to look for classes (.jar, .zip or directory)
     */
    public ClassPathLoader( File[] entries )
    {
        files = entries;
    }

    /**
     * useful methods to read the whole input stream in memory so that it can be
     * accessed faster. Processing rt.jar and tools.jar from JDK 1.3.1 brings
     * time from 50s to 7s.
     *
     * @param is Description of Parameter
     * @return The CachedStream value
     * @exception IOException Description of Exception
     */
    public static InputStream getCachedStream( InputStream is )
        throws IOException
    {
        is = new BufferedInputStream( is );
        byte[] buffer = new byte[8192];
        ByteArrayOutputStream baos = new ByteArrayOutputStream( 2048 );
        int n;
        baos.reset();
        while( ( n = is.read( buffer, 0, buffer.length ) ) != -1 )
        {
            baos.write( buffer, 0, n );
        }
        is.close();
        return new ByteArrayInputStream( baos.toByteArray() );
    }

    /**
     * return the whole set of classes in the classpath. Note that this method
     * can be very resource demanding since it must load all bytecode from all
     * classes in all resources in the classpath at a time. To process it in a
     * less resource demanding way, it is maybe better to use the <tt>loaders()
     * </tt> that will return loader one by one.
     *
     * @return the hashtable containing ALL classes that are found in the given
     *      classpath. Note that the first entry of a given classname will
     *      shadow classes with the same name (as a classloader does)
     * @exception IOException Description of Exception
     */
    public Hashtable getClasses()
        throws IOException
    {
        Hashtable map = new Hashtable();
        Enumeration enum = loaders();
        while( enum.hasMoreElements() )
        {
            FileLoader loader = ( FileLoader )enum.nextElement();
            System.out.println( "Processing " + loader.getFile() );
            long t0 = System.currentTimeMillis();
            ClassFile[] classes = loader.getClasses();
            long dt = System.currentTimeMillis() - t0;
            System.out.println( "" + classes.length + " classes loaded in " + dt + "ms" );
            for( int j = 0; j < classes.length; j++ )
            {
                String name = classes[j].getFullName();
                // do not allow duplicates entries to preserve 'classpath' behavior
                // first class in wins
                if( !map.containsKey( name ) )
                {
                    map.put( name, classes[j] );
                }
            }
        }
        return map;
    }

    /**
     * @return the set of <tt>FileLoader</tt> loaders matching the given
     *      classpath.
     */
    public Enumeration loaders()
    {
        return new LoaderEnumeration();
    }

    /**
     * the interface to implement to look up for specific resources
     *
     * @author RT
     */
    public interface FileLoader
    {
        /**
         * the file url that is looked for .class files
         *
         * @return The File value
         */
        public File getFile();

        /**
         * return the set of classes found in the file
         *
         * @return The Classes value
         * @exception IOException Description of Exception
         */
        public ClassFile[] getClasses()
            throws IOException;
    }

    /**
     * the loader enumeration that will return loaders
     *
     * @author RT
     */
    protected class LoaderEnumeration implements Enumeration
    {
        protected int index = 0;

        public boolean hasMoreElements()
        {
            return index < files.length;
        }

        public Object nextElement()
        {
            if( index >= files.length )
            {
                throw new NoSuchElementException();
            }
            File file = files[index++];
            if( !file.exists() )
            {
                return new NullLoader( file );
            }
            if( file.isDirectory() )
            {
                // it's a directory
                return new DirectoryLoader( file );
            }
            else if( file.getName().endsWith( ".zip" ) || file.getName().endsWith( ".jar" ) )
            {
                // it's a jar/zip file
                return new JarLoader( file );
            }
            return new NullLoader( file );
        }
    }
}

/**
 * a null loader to return when the file is not valid
 *
 * @author RT
 */
class NullLoader implements ClassPathLoader.FileLoader
{

    private File file;

    NullLoader()
    {
        this( null );
    }

    NullLoader( File file )
    {
        this.file = file;
    }

    public ClassFile[] getClasses()
        throws IOException
    {
        return new ClassFile[0];
    }

    public File getFile()
    {
        return file;
    }
}

/**
 * jar loader specified in looking for classes in jar and zip
 *
 * @author RT
 * @todo read the jar manifest in case there is a Class-Path entry.
 */
class JarLoader implements ClassPathLoader.FileLoader
{

    private File file;

    JarLoader( File file )
    {
        this.file = file;
    }

    public ClassFile[] getClasses()
        throws IOException
    {
        ZipFile zipFile = new ZipFile( file );
        Vector v = new Vector();
        Enumeration entries = zipFile.entries();
        while( entries.hasMoreElements() )
        {
            ZipEntry entry = ( ZipEntry )entries.nextElement();
            if( entry.getName().endsWith( ".class" ) )
            {
                InputStream is = ClassPathLoader.getCachedStream( zipFile.getInputStream( entry ) );
                ClassFile classFile = new ClassFile( is );
                is.close();
                v.addElement( classFile );
            }
        }
        ClassFile[] classes = new ClassFile[v.size()];
        v.copyInto( classes );
        return classes;
    }

    public File getFile()
    {
        return file;
    }
}

/**
 * directory loader that will look all classes recursively
 *
 * @author RT
 * @todo should discard classes which package name does not match the directory
 *      ?
 */
class DirectoryLoader implements ClassPathLoader.FileLoader
{

    private File directory;

    DirectoryLoader( File dir )
    {
        directory = dir;
    }

    /**
     * List files that obeys to a specific filter recursively from a given base
     * directory.
     *
     * @param directory the directory where to list the files from.
     * @param filter the file filter to apply
     * @param recurse tells whether or not the listing is recursive.
     * @return the list of <tt>File</tt> objects that applies to the given
     *      filter.
     */
    public static Vector listFiles( File directory, FilenameFilter filter, boolean recurse )
    {
        if( !directory.isDirectory() )
        {
            throw new IllegalArgumentException( directory + " is not a directory" );
        }
        Vector list = new Vector();
        listFilesTo( list, directory, filter, recurse );
        return list;
    }

    /**
     * List and add files to a given list. As a convenience it sends back the
     * instance of the list given as a parameter.
     *
     * @param list the list of files where the filtered files should be added
     * @param directory the directory where to list the files from.
     * @param filter the file filter to apply
     * @param recurse tells whether or not the listing is recursive.
     * @return the list instance that was passed as the <tt>list</tt> argument.
     */
    private static Vector listFilesTo( Vector list, File directory, FilenameFilter filter, boolean recurse )
    {
        String[] files = directory.list( filter );
        for( int i = 0; i < files.length; i++ )
        {
            list.addElement( new File( directory, files[i] ) );
        }
        files = null;// we don't need it anymore
        if( recurse )
        {
            String[] subdirs = directory.list( new DirectoryFilter() );
            for( int i = 0; i < subdirs.length; i++ )
            {
                listFilesTo( list, new File( directory, subdirs[i] ), filter, recurse );
            }
        }
        return list;
    }

    public ClassFile[] getClasses()
        throws IOException
    {
        Vector v = new Vector();
        Vector files = listFiles( directory, new ClassFilter(), true );
        for( int i = 0; i < files.size(); i++ )
        {
            File file = ( File )files.elementAt( i );
            InputStream is = null;
            try
            {
                is = ClassPathLoader.getCachedStream( new FileInputStream( file ) );
                ClassFile classFile = new ClassFile( is );
                is.close();
                is = null;
                v.addElement( classFile );
            }
            finally
            {
                if( is != null )
                {
                    try
                    {
                        is.close();
                    }
                    catch( IOException ignored )
                    {}
                }
            }
        }
        ClassFile[] classes = new ClassFile[v.size()];
        v.copyInto( classes );
        return classes;
    }

    public File getFile()
    {
        return directory;
    }

}

/**
 * Convenient filter that accepts only directory <tt>File</tt>
 *
 * @author RT
 */
class DirectoryFilter implements FilenameFilter
{

    public boolean accept( File directory, String name )
    {
        File pathname = new File( directory, name );
        return pathname.isDirectory();
    }
}

/**
 * convenient filter to accept only .class files
 *
 * @author RT
 */
class ClassFilter implements FilenameFilter
{

    public boolean accept( File dir, String name )
    {
        return name.endsWith( ".class" );
    }
}
