/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.depend;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.types.DirectoryScanner;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.Path;

/**
 * Generate a dependency file for a given set of classes
 *
 * @author Conor MacNeill
 */
public class Depend extends MatchingTask
{

    /**
     * constants used with the cache file
     */
    private final static String CACHE_FILE_NAME = "dependencies.txt";
    private final static String CLASSNAME_PREPEND = "||:";

    /**
     * indicates that the dependency relationships should be extended beyond
     * direct dependencies to include all classes. So if A directly affects B
     * abd B directly affects C, then A indirectly affects C.
     */
    private boolean closure = false;

    /**
     * Flag which controls whether the reversed dependencies should be dumped to
     * the log
     */
    private boolean dump = false;

    /**
     * A map which gives for every class a list of te class which it affects.
     */
    private Hashtable affectedClassMap;

    /**
     * The directory which contains the dependency cache.
     */
    private File cache;

    /**
     * A map which gives information about a class
     */
    private Hashtable classFileInfoMap;

    /**
     * A map which gives the list of jars and classes from the classpath that a
     * class depends upon
     */
    private Hashtable classpathDependencies;

    /**
     * The classpath to look for additional dependencies
     */
    private Path dependClasspath;

    /**
     * The path where compiled class files exist.
     */
    private Path destPath;

    /**
     * The list of classes which are out of date.
     */
    private Hashtable outOfDateClasses;

    /**
     * The path where source files exist
     */
    private Path srcPath;

    public void setCache( File cache )
    {
        this.cache = cache;
    }

    /**
     * Set the classpath to be used for this dependency check.
     *
     * @param classpath The new Classpath value
     */
    public void setClasspath( Path classpath )
        throws TaskException
    {
        if( dependClasspath == null )
        {
            dependClasspath = classpath;
        }
        else
        {
            dependClasspath.append( classpath );
        }
    }

    public void setClosure( boolean closure )
    {
        this.closure = closure;
    }

    /**
     * Set the destination directory where the compiled java files exist.
     *
     * @param destPath The new DestDir value
     */
    public void setDestDir( Path destPath )
    {
        this.destPath = destPath;
    }

    /**
     * Flag to indicate whether the reverse dependency list should be dumped to
     * debug
     *
     * @param dump The new Dump value
     */
    public void setDump( boolean dump )
    {
        this.dump = dump;
    }

    /**
     * Set the source dirs to find the source Java files.
     *
     * @param srcPath The new Srcdir value
     */
    public void setSrcdir( Path srcPath )
    {
        this.srcPath = srcPath;
    }

    /**
     * Gets the classpath to be used for this dependency check.
     *
     * @return The Classpath value
     */
    public Path getClasspath()
    {
        return dependClasspath;
    }

    /**
     * Creates a nested classpath element.
     *
     * @return Description of the Returned Value
     */
    public Path createClasspath()
        throws TaskException
    {
        if( dependClasspath == null )
        {
            dependClasspath = new Path();
        }
        Path path1 = dependClasspath;
        final Path path = new Path();
        path1.addPath( path );
        return path;
    }

    /**
     * Does the work.
     *
     * @exception TaskException Thrown in unrecovrable error.
     */
    public void execute()
        throws TaskException
    {
        try
        {
            long start = System.currentTimeMillis();
            String[] srcPathList = srcPath.list();
            if( srcPathList.length == 0 )
            {
                throw new TaskException( "srcdir attribute must be set!" );
            }

            if( destPath == null )
            {
                destPath = srcPath;
            }

            if( cache != null && cache.exists() && !cache.isDirectory() )
            {
                throw new TaskException( "The cache, if specified, must point to a directory" );
            }

            if( cache != null && !cache.exists() )
            {
                cache.mkdirs();
            }

            determineDependencies();

            if( dump )
            {
                getLogger().debug( "Reverse Dependency Dump for " + affectedClassMap.size() + " classes:" );
                for( Enumeration e = affectedClassMap.keys(); e.hasMoreElements(); )
                {
                    String className = (String)e.nextElement();
                    getLogger().debug( " Class " + className + " affects:" );
                    Hashtable affectedClasses = (Hashtable)affectedClassMap.get( className );
                    for( Enumeration e2 = affectedClasses.keys(); e2.hasMoreElements(); )
                    {
                        String affectedClass = (String)e2.nextElement();
                        ClassFileInfo info = (ClassFileInfo)affectedClasses.get( affectedClass );
                        getLogger().debug( "    " + affectedClass + " in " + info.absoluteFile.getPath() );
                    }
                }

                if( classpathDependencies != null )
                {
                    getLogger().debug( "Classpath file dependencies (Forward):" );
                    for( Enumeration e = classpathDependencies.keys(); e.hasMoreElements(); )
                    {
                        String className = (String)e.nextElement();
                        getLogger().debug( " Class " + className + " depends on:" );
                        Hashtable dependencies = (Hashtable)classpathDependencies.get( className );
                        for( Enumeration e2 = dependencies.elements(); e2.hasMoreElements(); )
                        {
                            File classpathFile = (File)e2.nextElement();
                            getLogger().debug( "    " + classpathFile.getPath() );
                        }
                    }
                }

            }

            // we now need to scan for out of date files. When we have the list
            // we go through and delete all class files which are affected by these files.
            outOfDateClasses = new Hashtable();
            for( int i = 0; i < srcPathList.length; i++ )
            {
                File srcDir = (File)resolveFile( srcPathList[ i ] );
                if( srcDir.exists() )
                {
                    DirectoryScanner ds = this.getDirectoryScanner( srcDir );
                    String[] files = ds.getIncludedFiles();
                    scanDir( srcDir, files );
                }
            }

            // now check classpath file dependencies
            if( classpathDependencies != null )
            {
                for( Enumeration e = classpathDependencies.keys(); e.hasMoreElements(); )
                {
                    String className = (String)e.nextElement();
                    if( !outOfDateClasses.containsKey( className ) )
                    {
                        ClassFileInfo info = (ClassFileInfo)classFileInfoMap.get( className );

                        // if we have no info about the class - it may have been deleted already and we
                        // are using cached info.
                        if( info != null )
                        {
                            Hashtable dependencies = (Hashtable)classpathDependencies.get( className );
                            for( Enumeration e2 = dependencies.elements(); e2.hasMoreElements(); )
                            {
                                File classpathFile = (File)e2.nextElement();
                                if( classpathFile.lastModified() > info.absoluteFile.lastModified() )
                                {
                                    getLogger().debug( "Class " + className + " is out of date with respect to " + classpathFile );
                                    outOfDateClasses.put( className, className );
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            // we now have a complete list of classes which are out of date
            // We scan through the affected classes, deleting any affected classes.
            int count = deleteAllAffectedFiles();

            long duration = ( System.currentTimeMillis() - start ) / 1000;
            getLogger().info( "Deleted " + count + " out of date files in " + duration + " seconds" );
        }
        catch( Exception e )
        {
            throw new TaskException( "Error", e );
        }
    }

    /**
     * Scans the directory looking for source files that are newer than their
     * class files. The results are returned in the class variable compileList
     *
     * @param srcDir Description of Parameter
     * @param files Description of Parameter
     */
    protected void scanDir( File srcDir, String files[] )
    {

        long now = System.currentTimeMillis();

        for( int i = 0; i < files.length; i++ )
        {
            File srcFile = new File( srcDir, files[ i ] );
            if( files[ i ].endsWith( ".java" ) )
            {
                String filePath = srcFile.getPath();
                String className = filePath.substring( srcDir.getPath().length() + 1,
                                                       filePath.length() - ".java".length() );
                className = ClassFileUtils.convertSlashName( className );
                ClassFileInfo info = (ClassFileInfo)classFileInfoMap.get( className );
                if( info == null )
                {
                    // there was no class file. add this class to the list
                    outOfDateClasses.put( className, className );
                }
                else
                {
                    if( srcFile.lastModified() > info.absoluteFile.lastModified() )
                    {
                        outOfDateClasses.put( className, className );
                    }
                }
            }
        }
    }

    /**
     * Get the list of class files we are going to analyse.
     *
     * @param classLocations a path structure containing all the directories
     *      where classes can be found.
     * @return a vector containing the classes to analyse.
     */
    private ArrayList getClassFiles( Path classLocations )
        throws TaskException
    {
        // break the classLocations into its components.
        String[] classLocationsList = classLocations.list();

        ArrayList classFileList = new ArrayList();

        for( int i = 0; i < classLocationsList.length; ++i )
        {
            File dir = new File( classLocationsList[ i ] );
            if( dir.isDirectory() )
            {
                addClassFiles( classFileList, dir, dir );
            }
        }

        return classFileList;
    }

    /**
     * Add the list of class files from the given directory to the class file
     * vector, including any subdirectories.
     *
     * @param classFileList The feature to be added to the ClassFiles attribute
     * @param dir The feature to be added to the ClassFiles attribute
     * @param root The feature to be added to the ClassFiles attribute
     */
    private void addClassFiles( ArrayList classFileList, File dir, File root )
    {
        String[] filesInDir = dir.list();

        if( filesInDir != null )
        {
            int length = filesInDir.length;

            for( int i = 0; i < length; ++i )
            {
                File file = new File( dir, filesInDir[ i ] );
                if( file.isDirectory() )
                {
                    addClassFiles( classFileList, file, root );
                }
                else if( file.getName().endsWith( ".class" ) )
                {
                    ClassFileInfo info = new ClassFileInfo();
                    info.absoluteFile = file;
                    info.relativeName = file.getPath().substring( root.getPath().length() + 1,
                                                                  file.getPath().length() - 6 );
                    info.className = ClassFileUtils.convertSlashName( info.relativeName );
                    classFileList.add( info );
                }
            }
        }
    }

    private int deleteAffectedFiles( String className )
    {
        int count = 0;

        Hashtable affectedClasses = (Hashtable)affectedClassMap.get( className );
        if( affectedClasses != null )
        {
            for( Enumeration e = affectedClasses.keys(); e.hasMoreElements(); )
            {
                String affectedClassName = (String)e.nextElement();
                ClassFileInfo affectedClassInfo = (ClassFileInfo)affectedClasses.get( affectedClassName );
                if( affectedClassInfo.absoluteFile.exists() )
                {
                    getLogger().debug( "Deleting file " + affectedClassInfo.absoluteFile.getPath() + " since " + className + " out of date" );
                    affectedClassInfo.absoluteFile.delete();
                    count++;
                    if( closure )
                    {
                        count += deleteAffectedFiles( affectedClassName );
                    }
                    else
                    {
                        // without closure we may delete an inner class but not the
                        // top level class which would not trigger a recompile.

                        if( affectedClassName.indexOf( "$" ) != -1 )
                        {
                            // need to delete the main class
                            String topLevelClassName
                                = affectedClassName.substring( 0, affectedClassName.indexOf( "$" ) );
                            getLogger().debug( "Top level class = " + topLevelClassName );
                            ClassFileInfo topLevelClassInfo
                                = (ClassFileInfo)classFileInfoMap.get( topLevelClassName );
                            if( topLevelClassInfo != null &&
                                topLevelClassInfo.absoluteFile.exists() )
                            {
                                getLogger().debug( "Deleting file " + topLevelClassInfo.absoluteFile.getPath() + " since " + "one of its inner classes was removed" );
                                topLevelClassInfo.absoluteFile.delete();
                                count++;
                                if( closure )
                                {
                                    count += deleteAffectedFiles( topLevelClassName );
                                }
                            }
                        }
                    }
                }
            }
        }
        return count;
    }

    private int deleteAllAffectedFiles()
    {
        int count = 0;
        for( Enumeration e = outOfDateClasses.elements(); e.hasMoreElements(); )
        {
            String className = (String)e.nextElement();
            count += deleteAffectedFiles( className );
            ClassFileInfo classInfo = (ClassFileInfo)classFileInfoMap.get( className );
            if( classInfo != null && classInfo.absoluteFile.exists() )
            {
                classInfo.absoluteFile.delete();
                count++;
            }
        }
        return count;
    }

    /**
     * Determine the dependencies between classes. Class dependencies are
     * determined by examining the class references in a class file to other
     * classes
     *
     * @exception IOException Description of Exception
     */
    private void determineDependencies()
        throws IOException, TaskException
    {
        affectedClassMap = new Hashtable();
        classFileInfoMap = new Hashtable();
        boolean cacheDirty = false;

        Hashtable dependencyMap = new Hashtable();
        File depCacheFile = null;
        boolean depCacheFileExists = true;
        long depCacheFileLastModified = Long.MAX_VALUE;

        // read the dependency cache from the disk
        if( cache != null )
        {
            dependencyMap = readCachedDependencies();
            depCacheFile = new File( cache, CACHE_FILE_NAME );
            depCacheFileExists = depCacheFile.exists();
            depCacheFileLastModified = depCacheFile.lastModified();
        }
        for( Iterator e = getClassFiles( destPath ).iterator(); e.hasNext(); )
        {
            ClassFileInfo info = (ClassFileInfo)e.next();
            getLogger().debug( "Adding class info for " + info.className );
            classFileInfoMap.put( info.className, info );

            ArrayList dependencyList = null;

            if( cache != null )
            {
                // try to read the dependency info from the map if it is not out of date
                if( depCacheFileExists && depCacheFileLastModified > info.absoluteFile.lastModified() )
                {
                    // depFile exists and is newer than the class file
                    // need to get dependency list from the map.
                    dependencyList = (ArrayList)dependencyMap.get( info.className );
                }
            }

            if( dependencyList == null )
            {
                // not cached - so need to read directly from the class file
                FileInputStream inFileStream = null;
                try
                {
                    inFileStream = new FileInputStream( info.absoluteFile );
                    ClassFile classFile = new ClassFile();
                    classFile.read( inFileStream );

                    dependencyList = classFile.getClassRefs();
                    if( dependencyList != null )
                    {
                        cacheDirty = true;
                        dependencyMap.put( info.className, dependencyList );
                    }

                }
                finally
                {
                    if( inFileStream != null )
                    {
                        inFileStream.close();
                    }
                }
            }

            // This class depends on each class in the dependency list. For each
            // one of those, add this class into their affected classes list
            for( Iterator depEnum = dependencyList.iterator(); depEnum.hasNext(); )
            {
                String dependentClass = (String)depEnum.next();

                Hashtable affectedClasses = (Hashtable)affectedClassMap.get( dependentClass );
                if( affectedClasses == null )
                {
                    affectedClasses = new Hashtable();
                    affectedClassMap.put( dependentClass, affectedClasses );
                }

                affectedClasses.put( info.className, info );
            }
        }

        classpathDependencies = null;
        if( dependClasspath != null )
        {
            // now determine which jars each class depends upon
            classpathDependencies = new Hashtable();
            final ClassLoader classLoader = new URLClassLoader( dependClasspath.toURLs() );

            Hashtable classpathFileCache = new Hashtable();
            Object nullFileMarker = new Object();
            for( Enumeration e = dependencyMap.keys(); e.hasMoreElements(); )
            {
                String className = (String)e.nextElement();
                ArrayList dependencyList = (ArrayList)dependencyMap.get( className );
                Hashtable dependencies = new Hashtable();
                classpathDependencies.put( className, dependencies );
                for( Iterator e2 = dependencyList.iterator(); e2.hasNext(); )
                {
                    String dependency = (String)e2.next();
                    Object classpathFileObject = classpathFileCache.get( dependency );
                    if( classpathFileObject == null )
                    {
                        classpathFileObject = nullFileMarker;

                        if( !dependency.startsWith( "java." ) && !dependency.startsWith( "javax." ) )
                        {
                            final String name = dependency.replace( '.', '/' ) + ".class";
                            URL classURL = classLoader.getResource( name );
                            if( classURL != null )
                            {
                                if( classURL.getProtocol().equals( "jar" ) )
                                {
                                    String jarFilePath = classURL.getFile();
                                    if( jarFilePath.startsWith( "file:" ) )
                                    {
                                        int classMarker = jarFilePath.indexOf( '!' );
                                        jarFilePath = jarFilePath.substring( 5, classMarker );
                                    }
                                    classpathFileObject = new File( jarFilePath );
                                }
                                else if( classURL.getProtocol().equals( "file" ) )
                                {
                                    String classFilePath = classURL.getFile();
                                    classpathFileObject = new File( classFilePath );
                                }
                                getLogger().debug( "Class " + className + " depends on " + classpathFileObject + " due to " + dependency );
                            }
                        }
                        classpathFileCache.put( dependency, classpathFileObject );
                    }
                    if( classpathFileObject != null && classpathFileObject != nullFileMarker )
                    {
                        // we need to add this jar to the list for this class.
                        File jarFile = (File)classpathFileObject;
                        dependencies.put( jarFile, jarFile );
                    }
                }
            }
        }

        // write the dependency cache to the disk
        if( cache != null && cacheDirty )
        {
            writeCachedDependencies( dependencyMap );
        }
    }

    /**
     * Read the dependencies from cache file
     *
     * @return Description of the Returned Value
     * @exception IOException Description of Exception
     */
    private Hashtable readCachedDependencies()
        throws IOException
    {
        Hashtable dependencyMap = new Hashtable();

        if( cache != null )
        {
            File depFile = new File( cache, CACHE_FILE_NAME );
            BufferedReader in = null;
            if( depFile.exists() )
            {
                try
                {
                    in = new BufferedReader( new FileReader( depFile ) );
                    String line = null;
                    ArrayList dependencyList = null;
                    String className = null;
                    int prependLength = CLASSNAME_PREPEND.length();
                    while( ( line = in.readLine() ) != null )
                    {
                        if( line.startsWith( CLASSNAME_PREPEND ) )
                        {
                            dependencyList = new ArrayList();
                            className = line.substring( prependLength );
                            dependencyMap.put( className, dependencyList );
                        }
                        else
                        {
                            dependencyList.add( line );
                        }
                    }
                }
                finally
                {
                    if( in != null )
                    {
                        in.close();
                    }
                }
            }
        }

        return dependencyMap;
    }

    /**
     * Write the dependencies to cache file
     *
     * @param dependencyMap Description of Parameter
     * @exception IOException Description of Exception
     */
    private void writeCachedDependencies( Hashtable dependencyMap )
        throws IOException
    {
        if( cache != null )
        {
            PrintWriter pw = null;
            try
            {
                cache.mkdirs();
                File depFile = new File( cache, CACHE_FILE_NAME );

                pw = new PrintWriter( new FileWriter( depFile ) );
                for( Enumeration deps = dependencyMap.keys(); deps.hasMoreElements(); )
                {
                    String className = (String)deps.nextElement();

                    pw.println( CLASSNAME_PREPEND + className );

                    ArrayList dependencyList = (ArrayList)dependencyMap.get( className );
                    int size = dependencyList.size();
                    for( int x = 0; x < size; x++ )
                    {
                        pw.println( dependencyList.get( x ) );
                    }
                }
            }
            finally
            {
                if( pw != null )
                {
                    pw.close();
                }
            }
        }
    }

    /**
     * A class (struct) user to manage information about a class
     *
     * @author RT
     */
    private static class ClassFileInfo
    {
        /**
         * The file where the class file is stored in the file system
         */
        public File absoluteFile;

        /**
         * The Java class name of this class
         */
        public String className;

        /**
         * The location of the file relative to its base directory - the root of
         * the package namespace
         */
        public String relativeName;
    }
}

