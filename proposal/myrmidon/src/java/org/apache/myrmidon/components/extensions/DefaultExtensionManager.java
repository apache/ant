/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.extensions;

import java.io.File;
import org.apache.avalon.excalibur.extension.DefaultPackageRepository;
import org.apache.avalon.excalibur.extension.Extension;
import org.apache.avalon.excalibur.extension.OptionalPackage;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.excalibur.util.StringUtil;
import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.framework.parameters.ParameterException;
import org.apache.avalon.framework.parameters.Parameterizable;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.myrmidon.interfaces.extensions.ExtensionManager;

/**
 * PhoenixPackageRepository
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class DefaultExtensionManager
    extends DefaultPackageRepository
    implements LogEnabled, Parameterizable, Initializable, Disposable, ExtensionManager
{
    private static final Resources REZ =
        ResourceManager.getPackageResources( DefaultExtensionManager.class );

    /**
     * The standard location of tools.jar for IBM/Sun JDKs.
     */
    private static final String TOOLS_JAR =
        File.separator + "lib" + File.separator + "tools.jar";

    /**
     * The path relative to JRE in which tools.jar is located.
     */
    private static final String DEBIAN_TOOLS_JAR =
        File.separator + ".." + File.separator + "j2sdk1.3" +
        File.separator + "lib" + File.separator + "tools.jar";

    private Logger m_logger;
    private String m_path;

    public DefaultExtensionManager()
    {
        super( new File[ 0 ] );
    }

    public DefaultExtensionManager( final File[] path )
    {
        super( path );
    }

    public void enableLogging( final Logger logger )
    {
        m_logger = logger;
    }

    public void parameterize( final Parameters parameters )
        throws ParameterException
    {
        m_path = parameters.getParameter( "myrmidon.ext.path" );
    }

    public void initialize()
        throws Exception
    {
        final String[] pathElements = StringUtil.split( m_path, File.pathSeparator );
        final File[] dirs = new File[ pathElements.length ];
        for( int i = 0; i < dirs.length; i++ )
        {
            dirs[ i ] = new File( pathElements[ i ] );
        }

        setPath( dirs );

        scanPath();

        // Add the JVM's tools.jar as an extension
        final Extension extension = createToolsExtension();
        final File jar = getToolsJar();
        final Extension[] available = new Extension[]{extension};
        final Extension[] required = new Extension[ 0 ];
        final OptionalPackage toolsPackage = new OptionalPackage( jar, available, required );
        cacheOptionalPackage( toolsPackage );
    }

    public void dispose()
    {
        clearCache();
    }

    /**
     * Locates the optional package which best matches a required extension.
     *
     * @param extension the extension to locate an optional package
     * @return the optional package, or null if not found.
     */
    public OptionalPackage getOptionalPackage( final Extension extension )
    {
        final OptionalPackage[] packages = getOptionalPackages( extension );

        if( null == packages || 0 == packages.length ) return null;

        //TODO: Use heurisitic to find which is best package

        return packages[ 0 ];
    }

    protected void debug( final String message )
    {
        m_logger.debug( message );
    }

    private File getToolsJar()
        throws Exception
    {
        final String javaHome = System.getProperty( "java.home" );
        String jdkHome;
        if( javaHome.endsWith( "jre" ) )
        {
            jdkHome = javaHome.substring( 0, javaHome.length() - 4 );
        }
        else
        {
            jdkHome = javaHome;
        }

        //We need to search through a few locations to locate tools.jar
        File tools = new File( jdkHome + TOOLS_JAR );
        if( tools.exists() )
        {
            return tools;
        }

        //The path to tools.jar. In some cases $JRE_HOME is not equal to
        //$JAVA_HOME/jre. For example, on Debian, IBM's j2sdk1.3 .deb puts
        //the JRE in /usr/lib/j2sdk1.3, and the JDK in /usr/lib/j2re1.3,
        //tools.jar=${java.home}/../j2sdk1.3/lib/tools.jar
        tools = new File( jdkHome + DEBIAN_TOOLS_JAR );

        if( !tools.exists() )
        {
            final String message = REZ.getString( "extension.missing-tools.error" );
            throw new Exception( message );
        }

        return tools;
    }

    private static Extension createToolsExtension()
    {
        return new Extension( "com.sun.tools",
                              "1.0",
                              "com.sun",
                              "1.0",
                              "com.sun",
                              "com.sun",
                              null );
    }
}
