/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional.ejb;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.Environment;
import org.apache.tools.ant.types.Path;

/**
 * Websphere deployment tool that augments the ejbjar task.
 *
 * @author <mailto:msahu@interkeel.com>Maneesh Sahu</mailto>
 */

public class WebsphereDeploymentTool extends GenericDeploymentTool
{

    public final static String PUBLICID_EJB11
        = "-//Sun Microsystems, Inc.//DTD Enterprise JavaBeans 1.1//EN";

    public final static String PUBLICID_EJB20
        = "-//Sun Microsystems, Inc.//DTD Enterprise JavaBeans 2.0//EN";

    protected final static String SCHEMA_DIR = "Schema/";

    protected final static String WAS_EXT = "ibm-ejb-jar-ext.xmi";

    protected final static String WAS_BND = "ibm-ejb-jar-bnd.xmi";

    protected final static String WAS_CMP_MAP = "Map.mapxmi";

    protected final static String WAS_CMP_SCHEMA = "Schema.dbxmi";

    /**
     * Instance variable that stores the suffix for the websphere jarfile.
     */
    private String jarSuffix = ".jar";

    /**
     * Instance variable that determines whether generic ejb jars are kept.
     */
    private boolean keepgenerated = false;

    private String additionalArgs = "";

    private boolean keepGeneric = false;

    private String compiler = null;

    private boolean alwaysRebuild = true;

    private boolean ejbdeploy = true;

    /**
     * Indicates if the old CMP location convention is to be used.
     */
    private boolean newCMP = false;

    /**
     * The classpath to the websphere classes.
     */
    private Path wasClasspath = null;

    /**
     * true - Only output error messages, suppress informational messages
     */
    private boolean quiet = true;

    /**
     * the scratchdir for the ejbdeploy operation
     */
    private String tempdir = "_ejbdeploy_temp";

    /**
     * true - Only generate the deployment code, do not run RMIC or Javac
     */
    private boolean codegen;

    /**
     * The name of the database to create. (For top-down mapping only)
     */
    private String dbName;

    /**
     * The name of the schema to create. (For top-down mappings only)
     */
    private String dbSchema;

    /**
     * The DB Vendor name, the EJB is persisted against
     */
    private String dbVendor;

    /**
     * Instance variable that stores the location of the ejb 1.1 DTD file.
     */
    private String ejb11DTD;

    /**
     * true - Disable informational messages
     */
    private boolean noinform;

    /**
     * true - Disable the validation steps
     */
    private boolean novalidate;

    /**
     * true - Disable warning and informational messages
     */
    private boolean nowarn;

    /**
     * Additional options for RMIC
     */
    private String rmicOptions;

    /**
     * true - Enable internal tracing
     */
    private boolean trace;

    /**
     * true- Use the WebSphere 3.5 compatible mapping rules
     */
    private boolean use35MappingRules;

    /**
     * sets some additional args to send to ejbdeploy.
     *
     * @param args The new Args value
     */
    public void setArgs( String args )
    {
        this.additionalArgs = args;
    }

    /**
     * (true) Only generate the deployment code, do not run RMIC or Javac
     *
     * @param codegen The new Codegen value
     */
    public void setCodegen( boolean codegen )
    {
        this.codegen = codegen;
    }

    /**
     * The compiler (switch <code>-compiler</code>) to use
     *
     * @param compiler The new Compiler value
     */
    public void setCompiler( String compiler )
    {
        this.compiler = compiler;
    }

    /**
     * Sets the name of the Database to create
     *
     * @param dbName The new Dbname value
     */
    public void setDbname( String dbName )
    {
        this.dbName = dbName;
    }

    /**
     * Sets the name of the schema to create
     *
     * @param dbSchema The new Dbschema value
     */
    public void setDbschema( String dbSchema )
    {
        this.dbSchema = dbSchema;
    }

    /**
     * Sets the DB Vendor for the Entity Bean mapping
     *
     * @param dbvendor The new Dbvendor value
     */
    public void setDbvendor( DBVendor dbvendor )
    {
        this.dbVendor = dbvendor.getValue();
    }

    /**
     * Setter used to store the location of the Sun's Generic EJB DTD. This can
     * be a file on the system or a resource on the classpath.
     *
     * @param inString the string to use as the DTD location.
     */
    public void setEJBdtd( String inString )
    {
        this.ejb11DTD = inString;
    }

    /**
     * Decide, wether ejbdeploy should be called or not
     *
     * @param ejbdeploy
     */
    public void setEjbdeploy( boolean ejbdeploy )
    {
        this.ejbdeploy = ejbdeploy;
    }

    /**
     * Sets whether -keepgenerated is passed to ejbdeploy (that is, the .java
     * source files are kept).
     *
     * @param inValue either 'true' or 'false'
     */
    public void setKeepgenerated( String inValue )
    {
        this.keepgenerated = Boolean.valueOf( inValue ).booleanValue();
    }

    /**
     * Setter used to store the value of keepGeneric
     *
     * @param inValue a string, either 'true' or 'false'.
     */
    public void setKeepgeneric( boolean inValue )
    {
        this.keepGeneric = inValue;
    }

    /**
     * Set the value of the newCMP scheme. The old CMP scheme locates the
     * websphere CMP descriptor based on the naming convention where the
     * websphere CMP file is expected to be named with the bean name as the
     * prefix. Under this scheme the name of the CMP descriptor does not match
     * the name actually used in the main websphere EJB descriptor. Also,
     * descriptors which contain multiple CMP references could not be used.
     *
     * @param newCMP The new NewCMP value
     */
    public void setNewCMP( boolean newCMP )
    {
        this.newCMP = newCMP;
    }

    /**
     * (true) Disable informational messages
     *
     * @param noinfom The new Noinform value
     */
    public void setNoinform( boolean noinfom )
    {
        this.noinform = noinform;
    }

    /**
     * (true) Disable the validation steps
     *
     * @param novalidate The new Novalidate value
     */
    public void setNovalidate( boolean novalidate )
    {
        this.novalidate = novalidate;
    }

    /**
     * (true) Disable warning and informational messages
     *
     * @param nowarn The new Nowarn value
     */
    public void setNowarn( boolean nowarn )
    {
        this.nowarn = nowarn;
    }

    /**
     * Set the value of the oldCMP scheme. This is an antonym for newCMP
     *
     * @param oldCMP The new OldCMP value
     */
    public void setOldCMP( boolean oldCMP )
    {
        this.newCMP = !oldCMP;
    }

    /**
     * (true) Only output error messages, suppress informational messages
     *
     * @param quiet The new Quiet value
     */
    public void setQuiet( boolean quiet )
    {
        this.quiet = quiet;
    }

    /**
     * Set the rebuild flag to false to only update changes in the jar rather
     * than rerunning ejbdeploy
     *
     * @param rebuild The new Rebuild value
     */
    public void setRebuild( boolean rebuild )
    {
        this.alwaysRebuild = rebuild;
    }

    /**
     * Setter used to store the suffix for the generated websphere jar file.
     *
     * @param inString the string to use as the suffix.
     */
    public void setSuffix( String inString )
    {
        this.jarSuffix = inString;
    }

    /**
     * Sets the temporary directory for the ejbdeploy task
     *
     * @param tempdir The new Tempdir value
     */
    public void setTempdir( String tempdir )
    {
        this.tempdir = tempdir;
    }

    /**
     * (true) Enable internal tracing
     *
     * @param trace The new Trace value
     */
    public void setTrace( boolean trace )
    {
        this.trace = trace;
    }

    /**
     * (true) Use the WebSphere 3.5 compatible mapping rules
     *
     * @param attr The new Use35 value
     */
    public void setUse35( boolean attr )
    {
        use35MappingRules = attr;
    }

    public void setWASClasspath( Path wasClasspath )
    {
        this.wasClasspath = wasClasspath;
    }

    /**
     * Get the classpath to the websphere classpaths
     *
     * @return Description of the Returned Value
     */
    public Path createWASClasspath()
    {
        if( wasClasspath == null )
        {
            wasClasspath = new Path( getTask().getProject() );
        }
        return wasClasspath.createPath();
    }

    /**
     * Called to validate that the tool parameters have been configured.
     *
     * @exception TaskException Description of Exception
     */
    public void validateConfigured()
        throws TaskException
    {
        super.validateConfigured();
    }

    /**
     * Helper method invoked by isRebuildRequired to get a ClassLoader for a Jar
     * File passed to it.
     *
     * @param classjar java.io.File representing jar file to get classes from.
     * @return The ClassLoaderFromJar value
     * @exception IOException Description of Exception
     */
    protected ClassLoader getClassLoaderFromJar( File classjar )
        throws IOException, TaskException
    {
        Path lookupPath = new Path( getTask().getProject() );
        lookupPath.setLocation( classjar );
        Path classpath = getCombinedClasspath();
        if( classpath != null )
        {
            lookupPath.append( classpath );
        }
        return new AntClassLoader( getTask().getProject(), lookupPath );
    }

    protected DescriptorHandler getDescriptorHandler( File srcDir )
    {
        DescriptorHandler handler = new DescriptorHandler( getTask(), srcDir );

        // register all the DTDs, both the ones that are known and


        // any supplied by the user

        handler.registerDTD( PUBLICID_EJB11, ejb11DTD );
        for( Iterator i = getConfig().dtdLocations.iterator(); i.hasNext(); )
        {
            EjbJar.DTDLocation dtdLocation = (EjbJar.DTDLocation)i.next();
            handler.registerDTD( dtdLocation.getPublicId(), dtdLocation.getLocation() );
        }
        return handler;
    }

    /**
     * Gets the options for the EJB Deploy operation
     *
     * @return String
     */
    protected String getOptions()
    {
        // Set the options


        StringBuffer options = new StringBuffer();
        if( dbVendor != null )
        {
            options.append( " -dbvendor " ).append( dbVendor );
        }
        if( dbName != null )
        {
            options.append( " -dbname \"" ).append( dbName ).append( "\"" );
        }
        if( dbSchema != null )
        {
            options.append( " -dbschema \"" ).append( dbSchema ).append( "\"" );
        }
        if( codegen )
        {
            options.append( " -codegen" );
        }
        if( quiet )
        {
            options.append( " -quiet" );
        }
        if( novalidate )
        {
            options.append( " -novalidate" );
        }
        if( nowarn )
        {
            options.append( " -nowarn" );
        }
        if( noinform )
        {
            options.append( " -noinform" );
        }
        if( trace )
        {
            options.append( " -trace" );
        }
        if( use35MappingRules )
        {
            options.append( " -35" );
        }
        if( rmicOptions != null )
        {
            options.append( " -rmic \"" ).append( rmicOptions ).append( "\"" );
        }
        return options.toString();
    }

    protected DescriptorHandler getWebsphereDescriptorHandler( final File srcDir )
    {
        DescriptorHandler handler =
            new DescriptorHandler( getTask(), srcDir )
            {
                protected void processElement()
                {
                }
            };
        for( Iterator i = getConfig().dtdLocations.iterator(); i.hasNext(); )
        {
            EjbJar.DTDLocation dtdLocation = (EjbJar.DTDLocation)i.next();
            handler.registerDTD( dtdLocation.getPublicId(), dtdLocation.getLocation() );
        }
        return handler;
    }

    /**
     * Helper method to check to see if a websphere EBJ1.1 jar needs to be
     * rebuilt using ejbdeploy. Called from writeJar it sees if the "Bean"
     * classes are the only thing that needs to be updated and either updates
     * the Jar with the Bean classfile or returns true, saying that the whole
     * websphere jar needs to be regened with ejbdeploy. This allows faster
     * build times for working developers. <p>
     *
     * The way websphere ejbdeploy works is it creates wrappers for the publicly
     * defined methods as they are exposed in the remote interface. If the
     * actual bean changes without changing the the method signatures then only
     * the bean classfile needs to be updated and the rest of the websphere jar
     * file can remain the same. If the Interfaces, ie. the method signatures
     * change or if the xml deployment dicriptors changed, the whole jar needs
     * to be rebuilt with ejbdeploy. This is not strictly true for the xml
     * files. If the JNDI name changes then the jar doesnt have to be rebuild,
     * but if the resources references change then it does. At this point the
     * websphere jar gets rebuilt if the xml files change at all.
     *
     * @param genericJarFile java.io.File The generic jar file.
     * @param websphereJarFile java.io.File The websphere jar file to check to
     *      see if it needs to be rebuilt.
     * @return The RebuildRequired value
     */
    protected boolean isRebuildRequired( File genericJarFile, File websphereJarFile )
        throws TaskException
    {
        boolean rebuild = false;
        JarFile genericJar = null;
        JarFile wasJar = null;
        File newwasJarFile = null;
        JarOutputStream newJarStream = null;
        try
        {
            log( "Checking if websphere Jar needs to be rebuilt for jar " + websphereJarFile.getName(),
                 Project.MSG_VERBOSE );

            // Only go forward if the generic and the websphere file both exist


            if( genericJarFile.exists() && genericJarFile.isFile()
                && websphereJarFile.exists() && websphereJarFile.isFile() )
            {
                //open jar files


                genericJar = new JarFile( genericJarFile );
                wasJar = new JarFile( websphereJarFile );
                Hashtable genericEntries = new Hashtable();
                Hashtable wasEntries = new Hashtable();
                Hashtable replaceEntries = new Hashtable();



                //get the list of generic jar entries

                for( Enumeration e = genericJar.entries(); e.hasMoreElements(); )
                {
                    JarEntry je = (JarEntry)e.nextElement();
                    genericEntries.put( je.getName().replace( '\\', '/' ), je );
                }

                //get the list of websphere jar entries


                for( Enumeration e = wasJar.entries(); e.hasMoreElements(); )
                {
                    JarEntry je = (JarEntry)e.nextElement();
                    wasEntries.put( je.getName(), je );
                }

                //Cycle Through generic and make sure its in websphere

                ClassLoader genericLoader = getClassLoaderFromJar( genericJarFile );
                for( Enumeration e = genericEntries.keys(); e.hasMoreElements(); )
                {
                    String filepath = (String)e.nextElement();
                    if( wasEntries.containsKey( filepath ) )
                    {// File name/path match
                        // Check files see if same
                        JarEntry genericEntry = (JarEntry)genericEntries.get( filepath );
                        JarEntry wasEntry = (JarEntry)wasEntries.get( filepath );
                        if( ( genericEntry.getCrc() != wasEntry.getCrc() ) || // Crc's Match

                            ( genericEntry.getSize() != wasEntry.getSize() ) )
                        {// Size Match
                            if( genericEntry.getName().endsWith( ".class" ) )
                            {
                                //File are different see if its an object or an interface
                                String classname = genericEntry.getName().replace( File.separatorChar, '.' );
                                classname = classname.substring( 0, classname.lastIndexOf( ".class" ) );
                                Class genclass = genericLoader.loadClass( classname );
                                if( genclass.isInterface() )
                                {
                                    //Interface changed   rebuild jar.


                                    log( "Interface " + genclass.getName() + " has changed", Project.MSG_VERBOSE );
                                    rebuild = true;
                                    break;
                                }
                                else
                                {
                                    //Object class Changed   update it.
                                    replaceEntries.put( filepath, genericEntry );
                                }
                            }
                            else
                            {
                                // is it the manifest. If so ignore it

                                if( !genericEntry.getName().equals( "META-INF/MANIFEST.MF" ) )
                                {
                                    //File other then class changed   rebuild


                                    log( "Non class file " + genericEntry.getName() + " has changed", Project.MSG_VERBOSE );
                                    rebuild = true;
                                }
                                break;
                            }
                        }
                    }
                    else
                    {// a file doesnt exist rebuild
                        log( "File " + filepath + " not present in websphere jar", Project.MSG_VERBOSE );
                        rebuild = true;
                        break;
                    }
                }
                if( !rebuild )
                {
                    log( "No rebuild needed - updating jar", Project.MSG_VERBOSE );
                    newwasJarFile = new File( websphereJarFile.getAbsolutePath() + ".temp" );
                    if( newwasJarFile.exists() )
                    {
                        newwasJarFile.delete();
                    }
                    newJarStream = new JarOutputStream( new FileOutputStream( newwasJarFile ) );
                    newJarStream.setLevel( 0 );



                    //Copy files from old websphere jar

                    for( Enumeration e = wasEntries.elements(); e.hasMoreElements(); )
                    {
                        byte[] buffer = new byte[ 1024 ];
                        int bytesRead;
                        InputStream is;
                        JarEntry je = (JarEntry)e.nextElement();
                        if( je.getCompressedSize() == -1 ||
                            je.getCompressedSize() == je.getSize() )
                        {
                            newJarStream.setLevel( 0 );
                        }
                        else
                        {
                            newJarStream.setLevel( 9 );
                        }



                        // Update with changed Bean class

                        if( replaceEntries.containsKey( je.getName() ) )
                        {
                            log( "Updating Bean class from generic Jar " + je.getName(),
                                 Project.MSG_VERBOSE );

                            // Use the entry from the generic jar


                            je = (JarEntry)replaceEntries.get( je.getName() );
                            is = genericJar.getInputStream( je );
                        }
                        else
                        {//use fle from original websphere jar


                            is = wasJar.getInputStream( je );
                        }
                        newJarStream.putNextEntry( new JarEntry( je.getName() ) );
                        while( ( bytesRead = is.read( buffer ) ) != -1 )
                        {
                            newJarStream.write( buffer, 0, bytesRead );
                        }
                        is.close();
                    }
                }
                else
                {
                    log( "websphere Jar rebuild needed due to changed interface or XML", Project.MSG_VERBOSE );
                }
            }
            else
            {
                rebuild = true;
            }
        }
        catch( ClassNotFoundException cnfe )
        {
            String cnfmsg = "ClassNotFoundException while processing ejb-jar file"
                + ". Details: "
                + cnfe.getMessage();
            throw new TaskException( cnfmsg, cnfe );
        }
        catch( IOException ioe )
        {
            String msg = "IOException while processing ejb-jar file "
                + ". Details: "
                + ioe.getMessage();
            throw new TaskException( msg, ioe );
        }
        finally
        {
            // need to close files and perhaps rename output


            if( genericJar != null )
            {
                try
                {
                    genericJar.close();
                }
                catch( IOException closeException )
                {
                }
            }
            if( wasJar != null )
            {
                try
                {
                    wasJar.close();
                }
                catch( IOException closeException )
                {
                }
            }
            if( newJarStream != null )
            {
                try
                {
                    newJarStream.close();
                }
                catch( IOException closeException )
                {
                }
                websphereJarFile.delete();
                newwasJarFile.renameTo( websphereJarFile );
                if( !websphereJarFile.exists() )
                {
                    rebuild = true;
                }
            }
        }
        return rebuild;
    }

    /**
     * Add any vendor specific files which should be included in the EJB Jar.
     *
     * @param ejbFiles The feature to be added to the VendorFiles attribute
     * @param baseName The feature to be added to the VendorFiles attribute
     */
    protected void addVendorFiles( Hashtable ejbFiles, String baseName )
    {
        String ddPrefix = ( usingBaseJarName() ? "" : baseName );
        String dbPrefix = ( dbVendor == null ) ? "" : dbVendor + "-";



        // Get the Extensions document

        File websphereEXT = new File( getConfig().descriptorDir, ddPrefix + WAS_EXT );
        if( websphereEXT.exists() )
        {
            ejbFiles.put( META_DIR + WAS_EXT,
                          websphereEXT );
        }
        else
        {
            log( "Unable to locate websphere extensions. It was expected to be in " +
                 websphereEXT.getPath(), Project.MSG_VERBOSE );
        }
        File websphereBND = new File( getConfig().descriptorDir, ddPrefix + WAS_BND );
        if( websphereBND.exists() )
        {
            ejbFiles.put( META_DIR + WAS_BND,
                          websphereBND );
        }
        else
        {
            log( "Unable to locate websphere bindings. It was expected to be in " +
                 websphereBND.getPath(), Project.MSG_VERBOSE );
        }
        if( !newCMP )
        {
            log( "The old method for locating CMP files has been DEPRECATED.", Project.MSG_VERBOSE );
            log( "Please adjust your websphere descriptor and set newCMP=\"true\" " +
                 "to use the new CMP descriptor inclusion mechanism. ", Project.MSG_VERBOSE );
        }
        else
        {
            // We attempt to put in the MAP and Schema files of CMP beans


            try
            {
                // Add the Map file


                File websphereMAP = new File( getConfig().descriptorDir,
                                              ddPrefix + dbPrefix + WAS_CMP_MAP );
                if( websphereMAP.exists() )
                {
                    ejbFiles.put( META_DIR + WAS_CMP_MAP,
                                  websphereMAP );
                }
                else
                {
                    log( "Unable to locate the websphere Map: " +
                         websphereMAP.getPath(), Project.MSG_VERBOSE );
                }
                File websphereSchema = new File( getConfig().descriptorDir,
                                                 ddPrefix + dbPrefix + WAS_CMP_SCHEMA );
                if( websphereSchema.exists() )
                {
                    ejbFiles.put( META_DIR + SCHEMA_DIR + WAS_CMP_SCHEMA,
                                  websphereSchema );
                }
                else
                {
                    log( "Unable to locate the websphere Schema: " +
                         websphereSchema.getPath(), Project.MSG_VERBOSE );
                }

                // Theres nothing else to see here...keep moving sonny
            }
            catch( Exception e )
            {
                String msg = "Exception while adding Vendor specific files: " +
                    e.toString();
                throw new TaskException( msg, e );
            }
        }
    }

    /**
     * Method used to encapsulate the writing of the JAR file. Iterates over the
     * filenames/java.io.Files in the Hashtable stored on the instance variable
     * ejbFiles.
     *
     * @param baseName Description of Parameter
     * @param jarFile Description of Parameter
     * @param files Description of Parameter
     * @param publicId Description of Parameter
     * @exception TaskException Description of Exception
     */
    protected void writeJar( String baseName, File jarFile, Hashtable files, String publicId )
        throws TaskException
    {
        if( ejbdeploy )
        {
            // create the -generic.jar, if required


            File genericJarFile = super.getVendorOutputJarFile( baseName );
            super.writeJar( baseName, genericJarFile, files, publicId );

            // create the output .jar, if required
            if( alwaysRebuild || isRebuildRequired( genericJarFile, jarFile ) )
            {
                buildWebsphereJar( genericJarFile, jarFile );
            }
            if( !keepGeneric )
            {
                log( "deleting generic jar " + genericJarFile.toString(),
                     Project.MSG_VERBOSE );
                genericJarFile.delete();
            }
        }
        else
        {
            // create the "undeployed" output .jar, if required


            super.writeJar( baseName, jarFile, files, publicId );
        }

        /*
         * / need to create a generic jar first.
         * File genericJarFile = super.getVendorOutputJarFile(baseName);
         * super.writeJar(baseName, genericJarFile, files, publicId);
         * if (alwaysRebuild || isRebuildRequired(genericJarFile, jarFile)) {
         * buildWebsphereJar(genericJarFile, jarFile);
         * }
         * if (!keepGeneric) {
         * log("deleting generic jar " + genericJarFile.toString(),
         * Project.MSG_VERBOSE);
         * genericJarFile.delete();
         * }
         */
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
        return new File( getDestDir(), baseName + jarSuffix );
    }// end getOptions

    /**
     * Helper method invoked by execute() for each websphere jar to be built.
     * Encapsulates the logic of constructing a java task for calling
     * websphere.ejbdeploy and executing it.
     *
     * @param sourceJar java.io.File representing the source (EJB1.1) jarfile.
     * @param destJar java.io.File representing the destination, websphere
     *      jarfile.
     */
    private void buildWebsphereJar( File sourceJar, File destJar )
        throws TaskException
    {
        try
        {
            if( ejbdeploy )
            {
                String args =
                    " " + sourceJar.getPath() +
                    " " + tempdir +
                    " " + destJar.getPath() +
                    " " + getOptions();
                if( getCombinedClasspath() != null && getCombinedClasspath().toString().length() > 0 )
                    args += " -cp " + getCombinedClasspath();



                // Why do my ""'s get stripped away???

                log( "EJB Deploy Options: " + args, Project.MSG_VERBOSE );
                Java javaTask = (Java)getTask().getProject().createTask( "java" );

                // Set the JvmArgs


                javaTask.createJvmarg().setValue( "-Xms64m" );
                javaTask.createJvmarg().setValue( "-Xmx128m" );



                // Set the Environment variable

                Environment.Variable var = new Environment.Variable();
                var.setKey( "websphere.lib.dir" );
                var.setValue( getTask().getProject().getProperty( "websphere.home" ) + "/lib" );
                javaTask.addSysproperty( var );



                // Set the working directory

                javaTask.setDir( new File( getTask().getProject().getProperty( "websphere.home" ) ) );



                // Set the Java class name
                javaTask.setClassname( "com.ibm.etools.ejbdeploy.EJBDeploy" );
                Commandline.Argument arguments = javaTask.createArg();
                arguments.setLine( args );
                Path classpath = wasClasspath;
                if( classpath == null )
                {
                    classpath = getCombinedClasspath();
                }
                if( classpath != null )
                {
                    javaTask.setClasspath( classpath );
                    javaTask.setFork( true );
                }
                else
                {
                    javaTask.setFork( true );
                }
                log( "Calling websphere.ejbdeploy for " + sourceJar.toString(),
                     Project.MSG_VERBOSE );
                javaTask.execute();
            }
        }
        catch( Exception e )
        {
            // Have to catch this because of the semantics of calling main()


            String msg = "Exception while calling ejbdeploy. Details: " + e.toString();
            throw new TaskException( msg, e );
        }
    }

    /**
     * Enumerated attribute with the values for the database vendor types
     *
     * @author RT
     */

    public static class DBVendor extends EnumeratedAttribute
    {

        public String[] getValues()
        {
            return new String[]{
                "SQL92", "SQL99", "DB2UDBWIN_V71", "DB2UDBOS390_V6", "DB2UDBAS400_V4R5",
                "ORACLE_V8", "INFORMIX_V92", "SYBASE_V1192", "MSSQLSERVER_V7", "MYSQL_V323"
            };
        }

    }

}

