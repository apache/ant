/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional.ejb;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import javax.xml.parsers.SAXParser;
import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.depend.Dependencies;
import org.apache.tools.ant.util.depend.Filter;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * A deployment tool which creates generic EJB jars. Generic jars contains only
 * those classes and META-INF entries specified in the EJB 1.1 standard This
 * class is also used as a framework for the creation of vendor specific
 * deployment tools. A number of template methods are provided through which the
 * vendor specific tool can hook into the EJB creation process.
 *
 * @author RT
 */
public class GenericDeploymentTool implements EJBDeploymentTool
{
    /**
     * Private constants that are used when constructing the standard jarfile
     */
    protected final static String META_DIR = "META-INF/";
    protected final static String EJB_DD = "ejb-jar.xml";

    /**
     * Instance variable that stores the suffix for the generated jarfile.
     */
    private String genericJarSuffix = "-generic.jar";

    /**
     * The classloader generated from the given classpath to load the super
     * classes and super interfaces.
     */
    private ClassLoader classpathLoader = null;

    /**
     * List of files have been loaded into the EJB jar
     */
    private List addedfiles;

    /**
     * The classpath to use with this deployment tool. This is appended to any
     * paths from the ejbjar task itself.
     */
    private Path classpath;

    /**
     * The configuration from the containing task. This config combined with the
     * settings of the individual attributes here constitues the complete config
     * for this deployment tool.
     */
    private EjbJar.Config config;

    /**
     * Stores a handle to the directory to put the Jar files in
     */
    private File destDir;

    /**
     * Handler used to parse the EJB XML descriptor
     */
    private DescriptorHandler handler;

    /**
     * The task to which this tool belongs. This is used to access services
     * provided by the ant core, such as logging.
     */
    private Task task;

    /**
     * Set the classpath to be used for this compilation.
     *
     * @param classpath The new Classpath value
     */
    public void setClasspath( Path classpath )
    {
        this.classpath = classpath;
    }

    /**
     * Setter used to store the value of destination directory prior to
     * execute() being called.
     *
     * @param inDir the destination directory.
     */
    public void setDestdir( File inDir )
    {
        this.destDir = inDir;
    }

    /**
     * Setter used to store the suffix for the generated jar file.
     *
     * @param inString the string to use as the suffix.
     */
    public void setGenericJarSuffix( String inString )
    {
        this.genericJarSuffix = inString;
    }

    /**
     * Set the task which owns this tool
     *
     * @param task The new Task value
     */
    public void setTask( Task task )
    {
        this.task = task;
    }

    /**
     * Get the prefix for vendor deployment descriptors. This will contain the
     * path and the start of the descriptor name, depending on the naming scheme
     *
     * @param baseName Description of Parameter
     * @param descriptorFileName Description of Parameter
     * @return The VendorDDPrefix value
     */
    public String getVendorDDPrefix( String baseName, String descriptorFileName )
    {
        String ddPrefix = null;

        if( config.namingScheme.getValue().equals( EjbJar.NamingScheme.DESCRIPTOR ) )
        {
            ddPrefix = baseName + config.baseNameTerminator;
        }
        else if( config.namingScheme.getValue().equals( EjbJar.NamingScheme.BASEJARNAME ) ||
            config.namingScheme.getValue().equals( EjbJar.NamingScheme.EJB_NAME ) ||
            config.namingScheme.getValue().equals( EjbJar.NamingScheme.DIRECTORY ) )
        {
            String canonicalDescriptor = descriptorFileName.replace( '\\', '/' );
            int index = canonicalDescriptor.lastIndexOf( '/' );
            if( index == -1 )
            {
                ddPrefix = "";
            }
            else
            {
                ddPrefix = descriptorFileName.substring( 0, index + 1 );
            }
        }
        return ddPrefix;
    }

    /**
     * Configure this tool for use in the ejbjar task.
     *
     * @param config Description of Parameter
     */
    public void configure( EjbJar.Config config )
    {
        this.config = config;

        classpathLoader = null;
    }

    /**
     * Add the classpath for the user classes
     *
     * @return Description of the Returned Value
     */
    public Path createClasspath()
    {
        if( classpath == null )
        {
            classpath = new Path( task.getProject() );
        }
        return classpath.createPath();
    }

    public void processDescriptor( String descriptorFileName, SAXParser saxParser )
    {

        checkConfiguration( descriptorFileName, saxParser );

        try
        {
            handler = getDescriptorHandler( config.srcDir );

            // Retrive the files to be added to JAR from EJB descriptor
            Hashtable ejbFiles = parseEjbFiles( descriptorFileName, saxParser );

            // Add any support classes specified in the build file
            addSupportClasses( ejbFiles );

            // Determine the JAR filename (without filename extension)
            String baseName = getJarBaseName( descriptorFileName );

            String ddPrefix = getVendorDDPrefix( baseName, descriptorFileName );

            // First the regular deployment descriptor
            ejbFiles.put( META_DIR + EJB_DD,
                          new File( config.descriptorDir, descriptorFileName ) );

            // now the vendor specific files, if any
            addVendorFiles( ejbFiles, ddPrefix );

            // add any dependent files
            checkAndAddDependants( ejbFiles );

            // Lastly create File object for the Jar files. If we are using
            // a flat destination dir, then we need to redefine baseName!
            if( config.flatDestDir && baseName.length() != 0 )
            {
                int startName = baseName.lastIndexOf( File.separator );
                if( startName == -1 )
                {
                    startName = 0;
                }

                int endName = baseName.length();
                baseName = baseName.substring( startName, endName );
            }

            File jarFile = getVendorOutputJarFile( baseName );

            // Check to see if we need a build and start doing the work!
            if( needToRebuild( ejbFiles, jarFile ) )
            {
                // Log that we are going to build...
                log( "building "
                     + jarFile.getName()
                     + " with "
                     + String.valueOf( ejbFiles.size() )
                     + " files",
                     Project.MSG_INFO );

                // Use helper method to write the jarfile
                String publicId = getPublicId();
                writeJar( baseName, jarFile, ejbFiles, publicId );

            }
            else
            {
                // Log that the file is up to date...
                log( jarFile.toString() + " is up to date.",
                     Project.MSG_VERBOSE );
            }

        }
        catch( SAXException se )
        {
            String msg = "SAXException while parsing '"
                + descriptorFileName.toString()
                + "'. This probably indicates badly-formed XML."
                + "  Details: "
                + se.getMessage();
            throw new TaskException( msg, se );
        }
        catch( IOException ioe )
        {
            String msg = "IOException while parsing'"
                + descriptorFileName.toString()
                + "'.  This probably indicates that the descriptor"
                + " doesn't exist. Details: "
                + ioe.getMessage();
            throw new TaskException( msg, ioe );
        }
    }

    /**
     * Called to validate that the tool parameters have been configured.
     *
     * @throws TaskException If the Deployment Tool's configuration isn't valid
     */
    public void validateConfigured()
        throws TaskException
    {
        if( ( destDir == null ) || ( !destDir.isDirectory() ) )
        {
            String msg = "A valid destination directory must be specified "
                + "using the \"destdir\" attribute.";
            throw new TaskException( msg );
        }
    }

    /**
     * Returns a Classloader object which parses the passed in generic EjbJar
     * classpath. The loader is used to dynamically load classes from
     * javax.ejb.* and the classes being added to the jar.
     *
     * @return The ClassLoaderForBuild value
     */
    protected ClassLoader getClassLoaderForBuild()
        throws TaskException
    {
        if( classpathLoader != null )
        {
            return classpathLoader;
        }

        Path combinedClasspath = getCombinedClasspath();

        // only generate a new ClassLoader if we have a classpath
        if( combinedClasspath == null )
        {
            classpathLoader = getClass().getClassLoader();
        }
        else
        {
            classpathLoader = new AntClassLoader( getTask().getProject(), combinedClasspath );
        }

        return classpathLoader;
    }

    /**
     * Get the classpath by combining the one from the surrounding task, if any
     * and the one from this tool.
     *
     * @return The CombinedClasspath value
     */
    protected Path getCombinedClasspath()
    {
        Path combinedPath = classpath;
        if( config.classpath != null )
        {
            if( combinedPath == null )
            {
                combinedPath = config.classpath;
            }
            else
            {
                combinedPath.append( config.classpath );
            }
        }

        return combinedPath;
    }

    /**
     * Get the basename terminator.
     *
     * @return The Config value
     */
    protected EjbJar.Config getConfig()
    {
        return config;
    }

    protected DescriptorHandler getDescriptorHandler( File srcDir )
    {
        DescriptorHandler handler = new DescriptorHandler( getTask(), srcDir );

        registerKnownDTDs( handler );

        // register any DTDs supplied by the user
        for( Iterator i = getConfig().dtdLocations.iterator(); i.hasNext(); )
        {
            EjbJar.DTDLocation dtdLocation = (EjbJar.DTDLocation)i.next();
            handler.registerDTD( dtdLocation.getPublicId(), dtdLocation.getLocation() );
        }
        return handler;
    }

    /**
     * Get the desitination directory.
     *
     * @return The DestDir value
     */
    protected File getDestDir()
    {
        return destDir;
    }

    /**
     * Using the EJB descriptor file name passed from the <code>ejbjar</code>
     * task, this method returns the "basename" which will be used to name the
     * completed JAR file.
     *
     * @param descriptorFileName String representing the file name of an EJB
     *      descriptor to be processed
     * @return The "basename" which will be used to name the completed JAR file
     */
    protected String getJarBaseName( String descriptorFileName )
    {

        String baseName = "";

        // Work out what the base name is
        if( config.namingScheme.getValue().equals( EjbJar.NamingScheme.BASEJARNAME ) )
        {
            String canonicalDescriptor = descriptorFileName.replace( '\\', '/' );
            int index = canonicalDescriptor.lastIndexOf( '/' );
            if( index != -1 )
            {
                baseName = descriptorFileName.substring( 0, index + 1 );
            }
            baseName += config.baseJarName;
        }
        else if( config.namingScheme.getValue().equals( EjbJar.NamingScheme.DESCRIPTOR ) )
        {
            int lastSeparatorIndex = descriptorFileName.lastIndexOf( File.separator );
            int endBaseName = -1;
            if( lastSeparatorIndex != -1 )
            {
                endBaseName = descriptorFileName.indexOf( config.baseNameTerminator,
                                                          lastSeparatorIndex );
            }
            else
            {
                endBaseName = descriptorFileName.indexOf( config.baseNameTerminator );
            }

            if( endBaseName != -1 )
            {
                baseName = descriptorFileName.substring( 0, endBaseName );
            }
            baseName = descriptorFileName.substring( 0, endBaseName );
        }
        else if( config.namingScheme.getValue().equals( EjbJar.NamingScheme.DIRECTORY ) )
        {
            int lastSeparatorIndex = descriptorFileName.lastIndexOf( File.separator );
            String dirName = descriptorFileName.substring( 0, lastSeparatorIndex );
            int dirSeparatorIndex = dirName.lastIndexOf( File.separator );
            if( dirSeparatorIndex != -1 )
            {
                dirName = dirName.substring( dirSeparatorIndex + 1 );
            }

            baseName = dirName;
        }
        else if( config.namingScheme.getValue().equals( EjbJar.NamingScheme.EJB_NAME ) )
        {
            baseName = handler.getEjbName();
        }
        return baseName;
    }

    protected Location getLocation()
    {
        return getTask().getLocation();
    }

    /**
     * Returns the Public ID of the DTD specified in the EJB descriptor. Not
     * every vendor-specific <code>DeploymentTool</code> will need to reference
     * this value or may want to determine this value in a vendor-specific way.
     *
     * @return Public ID of the DTD specified in the EJB descriptor.
     */
    protected String getPublicId()
    {
        return handler.getPublicId();
    }

    /**
     * Get the task for this tool.
     *
     * @return The Task value
     */
    protected Task getTask()
    {
        return task;
    }

    /**
     * Utility method that encapsulates the logic of adding a file entry to a
     * .jar file. Used by execute() to add entries to the jar file as it is
     * constructed.
     *
     * @param jStream A JarOutputStream into which to write the jar entry.
     * @param inputFile A File from which to read the contents the file being
     *      added.
     * @param logicalFilename A String representing the name, including all
     *      relevant path information, that should be stored for the entry being
     *      added.
     * @exception TaskException Description of Exception
     */
    protected void addFileToJar( JarOutputStream jStream,
                                 File inputFile,
                                 String logicalFilename )
        throws TaskException
    {
        FileInputStream iStream = null;
        try
        {
            if( !addedfiles.contains( logicalFilename ) )
            {
                iStream = new FileInputStream( inputFile );
                // Create the zip entry and add it to the jar file
                ZipEntry zipEntry = new ZipEntry( logicalFilename.replace( '\\', '/' ) );
                jStream.putNextEntry( zipEntry );

                // Create the file input stream, and buffer everything over
                // to the jar output stream
                byte[] byteBuffer = new byte[ 2 * 1024 ];
                int count = 0;
                do
                {
                    jStream.write( byteBuffer, 0, count );
                    count = iStream.read( byteBuffer, 0, byteBuffer.length );
                } while( count != -1 );

                //add it to list of files in jar
                addedfiles.add( logicalFilename );
            }
        }
        catch( IOException ioe )
        {
            log( "WARNING: IOException while adding entry " +
                 logicalFilename + " to jarfile from " + inputFile.getPath() + " " +
                 ioe.getClass().getName() + "-" + ioe.getMessage(), Project.MSG_WARN );
        }
        finally
        {
            // Close up the file input stream for the class file
            if( iStream != null )
            {
                try
                {
                    iStream.close();
                }
                catch( IOException closeException )
                {
                }
            }
        }
    }

    /**
     * Adds any classes the user specifies using <i>support</i> nested elements
     * to the <code>ejbFiles</code> Hashtable.
     *
     * @param ejbFiles Hashtable of EJB classes (and other) files that will be
     *      added to the completed JAR file
     */
    protected void addSupportClasses( Hashtable ejbFiles )
    {
        // add in support classes if any
        Project project = task.getProject();
        for( Iterator i = config.supportFileSets.iterator(); i.hasNext(); )
        {
            FileSet supportFileSet = (FileSet)i.next();
            File supportBaseDir = supportFileSet.getDir( project );
            DirectoryScanner supportScanner = supportFileSet.getDirectoryScanner( project );
            supportScanner.scan();
            String[] supportFiles = supportScanner.getIncludedFiles();
            for( int j = 0; j < supportFiles.length; ++j )
            {
                ejbFiles.put( supportFiles[ j ], new File( supportBaseDir, supportFiles[ j ] ) );
            }
        }
    }

    /**
     * Add any vendor specific files which should be included in the EJB Jar.
     *
     * @param ejbFiles The feature to be added to the VendorFiles attribute
     * @param ddPrefix The feature to be added to the VendorFiles attribute
     */
    protected void addVendorFiles( Hashtable ejbFiles, String ddPrefix )
    {
        // nothing to add for generic tool.
    }// end of writeJar

    /**
     * Add all available classes, that depend on Remote, Home, Bean, PK
     *
     * @param checkEntries files, that are extracted from the deployment
     *      descriptor
     * @exception TaskException Description of Exception
     */
    protected void checkAndAddDependants( Hashtable checkEntries )
        throws TaskException
    {
        Dependencies visitor = new Dependencies();
        Set set = new TreeSet();
        Set newSet = new HashSet();
        final String base = config.srcDir.getAbsolutePath() + File.separator;

        Iterator i = checkEntries.keySet().iterator();
        while( i.hasNext() )
        {
            String entryName = (String)i.next();
            if( entryName.endsWith( ".class" ) )
                newSet.add( entryName.substring( 0, entryName.length() - ".class".length() ).replace( File.separatorChar, '/' ) );
        }
        set.addAll( newSet );

        do
        {
            i = newSet.iterator();
            while( i.hasNext() )
            {
                String fileName = base + ( (String)i.next() ).replace( '/', File.separatorChar ) + ".class";

                try
                {
                    JavaClass javaClass = new ClassParser( fileName ).parse();
                    javaClass.accept( visitor );
                }
                catch( IOException e )
                {
                    log( "exception: " + e.getMessage(), Project.MSG_INFO );
                }
            }
            newSet.clear();
            newSet.addAll( visitor.getDependencies() );
            visitor.clearDependencies();

            Dependencies.applyFilter( newSet,
                                      new Filter()
                                      {
                                          public boolean accept( Object object )
                                          {
                                              String fileName = base + ( (String)object ).replace( '/', File.separatorChar ) + ".class";
                                              return new File( fileName ).exists();
                                          }
                                      } );
            newSet.removeAll( set );
            set.addAll( newSet );
        } while( newSet.size() > 0 );

        i = set.iterator();
        while( i.hasNext() )
        {
            String next = ( (String)i.next() ).replace( '/', File.separatorChar );
            checkEntries.put( next + ".class", new File( base + next + ".class" ) );
            log( "dependent class: " + next + ".class" + " - " + base + next + ".class", Project.MSG_VERBOSE );
        }
    }

    /**
     * This method is called as the first step in the processDescriptor method
     * to allow vendor-specific subclasses to validate the task configuration
     * prior to processing the descriptor. If the configuration is invalid, a
     * TaskException should be thrown.
     *
     * @param descriptorFileName String representing the file name of an EJB
     *      descriptor to be processed
     * @param saxParser SAXParser which may be used to parse the XML descriptor
     * @exception TaskException Description of Exception
     * @thows TaskException Thrown if the configuration is invalid
     */
    protected void checkConfiguration( String descriptorFileName,
                                       SAXParser saxParser )
        throws TaskException
    {

        /*
         * For the GenericDeploymentTool, do nothing.  Vendor specific
         * subclasses should throw a TaskException if the configuration is
         * invalid for their server.
         */
    }

    protected void log( String message, int level )
    {
        getTask().log( message, level );
    }

    /**
     * This method checks the timestamp on each file listed in the <code>
     * ejbFiles</code> and compares them to the timestamp on the <code>jarFile
     * </code>. If the <code>jarFile</code>'s timestamp is more recent than each
     * EJB file, <code>true</code> is returned. Otherwise, <code>false
     * </code> is returned. TODO: find a way to check the manifest-file, that is
     * found by naming convention
     *
     * @param ejbFiles Hashtable of EJB classes (and other) files that will be
     *      added to the completed JAR file
     * @param jarFile JAR file which will contain all of the EJB classes (and
     *      other) files
     * @return boolean indicating whether or not the <code>jarFile</code> is up
     *      to date
     */
    protected boolean needToRebuild( Hashtable ejbFiles, File jarFile )
    {
        if( jarFile.exists() )
        {
            long lastBuild = jarFile.lastModified();

            if( config.manifest != null && config.manifest.exists() &&
                config.manifest.lastModified() > lastBuild )
            {
                log( "Build needed because manifest " + config.manifest + " is out of date",
                     Project.MSG_VERBOSE );
                return true;
            }

            Iterator fileIter = ejbFiles.values().iterator();

            // Loop through the files seeing if any has been touched
            // more recently than the destination jar.
            while( fileIter.hasNext() )
            {
                File currentFile = (File)fileIter.next();
                if( lastBuild < currentFile.lastModified() )
                {
                    log( "Build needed because " + currentFile.getPath() + " is out of date",
                         Project.MSG_VERBOSE );
                    return true;
                }
            }
            return false;
        }

        return true;
    }

    /**
     * This method returns a list of EJB files found when the specified EJB
     * descriptor is parsed and processed.
     *
     * @param descriptorFileName String representing the file name of an EJB
     *      descriptor to be processed
     * @param saxParser SAXParser which may be used to parse the XML descriptor
     * @return Hashtable of EJB class (and other) files to be added to the
     *      completed JAR file
     * @throws SAXException Any SAX exception, possibly wrapping another
     *      exception
     * @throws IOException An IOException from the parser, possibly from a the
     *      byte stream or character stream
     */
    protected Hashtable parseEjbFiles( String descriptorFileName, SAXParser saxParser )
        throws IOException, SAXException
    {
        FileInputStream descriptorStream = null;
        Hashtable ejbFiles = null;

        try
        {

            /*
             * Parse the ejb deployment descriptor.  While it may not
             * look like much, we use a SAXParser and an inner class to
             * get hold of all the classfile names for the descriptor.
             */
            descriptorStream = new FileInputStream( new File( config.descriptorDir, descriptorFileName ) );
            saxParser.parse( new InputSource( descriptorStream ), handler );

            ejbFiles = handler.getFiles();

        }
        finally
        {
            if( descriptorStream != null )
            {
                try
                {
                    descriptorStream.close();
                }
                catch( IOException closeException )
                {
                }
            }
        }

        return ejbFiles;
    }

    /**
     * Register the locations of all known DTDs. vendor-specific subclasses
     * should override this method to define the vendor-specific locations of
     * the EJB DTDs
     *
     * @param handler Description of Parameter
     */
    protected void registerKnownDTDs( DescriptorHandler handler )
    {
        // none to register for generic
    }

    /**
     * Returns true, if the meta-inf dir is being explicitly set, false
     * otherwise.
     *
     * @return Description of the Returned Value
     */
    protected boolean usingBaseJarName()
    {
        return config.baseJarName != null;
    }

    /**
     * Method used to encapsulate the writing of the JAR file. Iterates over the
     * filenames/java.io.Files in the Hashtable stored on the instance variable
     * ejbFiles.
     *
     * @param baseName Description of Parameter
     * @param jarfile Description of Parameter
     * @param files Description of Parameter
     * @param publicId Description of Parameter
     * @exception TaskException Description of Exception
     */
    protected void writeJar( String baseName, File jarfile, Hashtable files,
                             String publicId )
        throws TaskException
    {

        JarOutputStream jarStream = null;
        try
        {
            // clean the addedfiles Vector
            addedfiles = new ArrayList();

            /*
             * If the jarfile already exists then whack it and recreate it.
             * Should probably think of a more elegant way to handle this
             * so that in case of errors we don't leave people worse off
             * than when we started =)
             */
            if( jarfile.exists() )
            {
                jarfile.delete();
            }
            jarfile.getParentFile().mkdirs();
            jarfile.createNewFile();

            InputStream in = null;
            Manifest manifest = null;
            try
            {
                File manifestFile = new File( getConfig().descriptorDir, baseName + "-manifest.mf" );
                if( manifestFile.exists() )
                {
                    in = new FileInputStream( manifestFile );
                }
                else if( config.manifest != null )
                {
                    in = new FileInputStream( config.manifest );
                    if( in == null )
                    {
                        throw new TaskException( "Could not find manifest file: " + config.manifest );
                    }
                }
                else
                {
                    String defaultManifest = "/org/apache/tools/ant/defaultManifest.mf";
                    in = this.getClass().getResourceAsStream( defaultManifest );
                    if( in == null )
                    {
                        throw new TaskException( "Could not find default manifest: " + defaultManifest );
                    }
                }

                manifest = new Manifest( in );
            }
            catch( IOException e )
            {
                throw new TaskException( "Unable to read manifest", e );
            }
            finally
            {
                if( in != null )
                {
                    in.close();
                }
            }

            // Create the streams necessary to write the jarfile

            jarStream = new JarOutputStream( new FileOutputStream( jarfile ), manifest );
            jarStream.setMethod( JarOutputStream.DEFLATED );

            // Loop through all the class files found and add them to the jar
            for( Iterator entryIterator = files.keySet().iterator(); entryIterator.hasNext(); )
            {
                String entryName = (String)entryIterator.next();
                File entryFile = (File)files.get( entryName );

                log( "adding file '" + entryName + "'",
                     Project.MSG_VERBOSE );

                addFileToJar( jarStream, entryFile, entryName );

                // See if there are any inner classes for this class and add them in if there are
                InnerClassFilenameFilter flt = new InnerClassFilenameFilter( entryFile.getName() );
                File entryDir = entryFile.getParentFile();
                String[] innerfiles = entryDir.list( flt );
                for( int i = 0, n = innerfiles.length; i < n; i++ )
                {

                    //get and clean up innerclass name
                    int entryIndex = entryName.lastIndexOf( entryFile.getName() ) - 1;
                    if( entryIndex < 0 )
                    {
                        entryName = innerfiles[ i ];
                    }
                    else
                    {
                        entryName = entryName.substring( 0, entryIndex ) + File.separatorChar + innerfiles[ i ];
                    }
                    // link the file
                    entryFile = new File( config.srcDir, entryName );

                    log( "adding innerclass file '" + entryName + "'",
                         Project.MSG_VERBOSE );

                    addFileToJar( jarStream, entryFile, entryName );

                }
            }
        }
        catch( IOException ioe )
        {
            String msg = "IOException while processing ejb-jar file '"
                + jarfile.toString()
                + "'. Details: "
                + ioe.getMessage();
            throw new TaskException( msg, ioe );
        }
        finally
        {
            if( jarStream != null )
            {
                try
                {
                    jarStream.close();
                }
                catch( IOException closeException )
                {
                }
            }
        }
    }

    /**
     * Get the vendor specific name of the Jar that will be output. The
     * modification date of this jar will be checked against the dependent bean
     * classes.
     *
     * @param baseName Description of Parameter
     * @return The VendorOutputJarFile value
     */
    File getVendorOutputJarFile( String baseName )
    {
        return new File( destDir, baseName + genericJarSuffix );
    }
}
