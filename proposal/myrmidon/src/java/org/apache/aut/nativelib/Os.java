/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.nativelib;

import java.util.Locale;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;

/**
 * Class to help determining the OS.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:umagesh@apache.org">Magesh Umasankar</a>
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 */
public class Os
{
    private static final Resources REZ =
        ResourceManager.getPackageResources( Os.class );

    private final static String OS_NAME =
        System.getProperty( "os.name" ).toLowerCase( Locale.US );
    private final static String OS_ARCH =
        System.getProperty( "os.arch" ).toLowerCase( Locale.US );
    private final static String OS_VERSION =
        System.getProperty( "os.version" ).toLowerCase( Locale.US );
    private final static String PATH_SEP =
        System.getProperty( "path.separator" );

    /**
     * Private constructor to block instantiation.
     */
    private Os()
    {
    }

    /**
     * Determines if the OS on which Ant is executing matches the given OS
     * version.
     */
    public static boolean isVersion( final String version )
    {
        return isOs( null, null, null, version );
    }

    /**
     * Determines if the OS on which Ant is executing matches the given OS
     * architecture.
     *
     * @param arch Description of Parameter
     * @return The Arch value
     */
    public static boolean isArch( final String arch )
    {
        return isOs( null, null, arch, null );
    }

    /**
     * Determines if the OS on which Ant is executing matches the given OS
     * family.
     *
     * @param family Description of Parameter
     * @return The Family value
     * @since 1.5
     */
    public static boolean isFamily( final String family )
    {
        return isOs( family, null, null, null );
    }

    /**
     * Determines if the OS on which Ant is executing matches the given OS name.
     *
     * @param name Description of Parameter
     * @return The Name value
     * @since 1.7
     */
    public static boolean isName( final String name )
    {
        return isOs( null, name, null, null );
    }

    /**
     * Determines if the OS on which Ant is executing matches the given OS
     * family, name, architecture and version
     *
     * @param family The OS family
     * @param name The OS name
     * @param arch The OS architecture
     * @param version The OS version
     * @return The Os value
     */
    public static boolean isOs( final String family,
                                final String name,
                                final String arch,
                                final String version )
    {
        if( family != null || name != null || arch != null || version != null )
        {
            final boolean isFamily = familyMatches( family );
            final boolean isName = nameMatches( name );
            final boolean isArch = archMatches( arch );
            final boolean isVersion = versionMatches( version );

            return isFamily && isName && isArch && isVersion;
        }
        else
        {
            return false;
        }
    }

    private static boolean versionMatches( final String version )
    {
        boolean isVersion = true;
        if( version != null )
        {
            isVersion = version.equals( OS_VERSION );
        }
        return isVersion;
    }

    private static boolean archMatches( final String arch )
    {
        boolean isArch = true;
        if( arch != null )
        {
            isArch = arch.equals( OS_ARCH );
        }
        return isArch;
    }

    private static boolean nameMatches( final String name )
    {
        boolean isName = true;
        if( name != null )
        {
            isName = name.equals( OS_NAME );
        }
        return isName;
    }

    private static boolean familyMatches( final String family )
    {
        boolean isFamily = true;
        if( family != null )
        {
            if( family.equals( "windows" ) )
            {
                isFamily = OS_NAME.indexOf( "windows" ) > -1;
            }
            else if( family.equals( "os/2" ) )
            {
                isFamily = OS_NAME.indexOf( "os/2" ) > -1;
            }
            else if( family.equals( "netware" ) )
            {
                isFamily = OS_NAME.indexOf( "netware" ) > -1;
            }
            else if( family.equals( "dos" ) )
            {
                isFamily = PATH_SEP.equals( ";" ) && !isFamily( "netware" );
            }
            else if( family.equals( "mac" ) )
            {
                isFamily = OS_NAME.indexOf( "mac" ) > -1;
            }
            else if( family.equals( "unix" ) )
            {
                isFamily = PATH_SEP.equals( ":" ) &&
                    ( !isFamily( "mac" ) || OS_NAME.endsWith( "x" ) );
            }
            else
            {
                final String message = REZ.getString( "unknown-os-family", family );
                throw new IllegalArgumentException( message );
            }
        }
        return isFamily;
    }
}
