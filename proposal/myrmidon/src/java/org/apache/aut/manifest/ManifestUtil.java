/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.manifest;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.BufferedReader;
import java.util.jar.Attributes;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.taskdefs.manifest.Manifest;
import org.apache.tools.ant.taskdefs.manifest.Section;

/**
 * Utility methods for manifest stuff.
 *
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public final class ManifestUtil
{
    /**
     * The Name Attribute is the first in a named section
     */
    public final static String ATTRIBUTE_NAME = "Name";
    /**
     * The From Header is disallowed in a Manifest
     */
    public final static String ATTRIBUTE_FROM = "From";
    /**
     * The Class-Path Header is special - it can be duplicated
     */
    public final static String ATTRIBUTE_CLASSPATH = Attributes.Name.CLASS_PATH.toString();
    /**
     * Default Manifest version if one is not specified
     */
    public final static String DEFAULT_MANIFEST_VERSION = "1.0";
    /**
     * The max length of a line in a Manifest
     */
    public final static int MAX_LINE_LENGTH = 70;

    public static Attribute buildAttribute( final String line )
        throws ManifestException
    {
        final Attribute attribute = new Attribute();
        parse( attribute, line );
        return attribute;
    }

    public static Manifest buildManifest( final Reader reader )
        throws ManifestException, IOException
    {
        final Manifest manifest = new Manifest();
        BufferedReader bufferedReader = new BufferedReader( reader );
        // This should be the manifest version
        final Section mainSection = manifest.getMainSection();
        String nextSectionName = mainSection.read( bufferedReader );
        final String readManifestVersion =
            mainSection.getAttributeValue( Attributes.Name.MANIFEST_VERSION.toString() );
        if( readManifestVersion != null )
        {
            manifest.setManifestVersion( readManifestVersion );
            mainSection.removeAttribute( Attributes.Name.MANIFEST_VERSION.toString() );
        }

        String line = null;
        while( ( line = bufferedReader.readLine() ) != null )
        {
            if( line.length() == 0 )
            {
                continue;
            }

            Section section = new Section();
            if( nextSectionName == null )
            {
                Attribute sectionName = ManifestUtil.buildAttribute( line );
                if( !sectionName.getName().equalsIgnoreCase( ManifestUtil.ATTRIBUTE_NAME ) )
                {
                    throw new ManifestException( "Manifest sections should start with a \"" + ManifestUtil.ATTRIBUTE_NAME +
                                                 "\" attribute and not \"" + sectionName.getName() + "\"" );
                }
                nextSectionName = sectionName.getValue();
            }
            else
            {
                // we have already started reading this section
                // this line is the first attribute. set it and then let the normal
                // read handle the rest
                Attribute firstAttribute = ManifestUtil.buildAttribute( line );
                section.addAttributeAndCheck( firstAttribute );
            }

            section.setName( nextSectionName );
            nextSectionName = section.read( bufferedReader );
            manifest.addSection( section );
        }

        return manifest;
    }

    /**
     * Construct a manifest from Ant's default manifest file.
     */
    public static Manifest getDefaultManifest()
        throws ManifestException
    {
        try
        {
            final InputStream input = getInputStream();
            final InputStreamReader reader = getReader( input );
            return buildManifest( reader );
        }
        catch( final IOException ioe )
        {
            throw new ManifestException( "Unable to read default manifest", ioe );
        }
    }

    private static InputStream getInputStream()
        throws ManifestException
    {
        final String location = "default.mf";
        final InputStream input = ManifestUtil.class.getResourceAsStream( location );
        if( null == input )
        {
            throw new ManifestException( "Could not find default manifest: " + location );
        }
        return input;
    }

    private static InputStreamReader getReader( final InputStream input )
    {
        try
        {
            return new InputStreamReader( input, "ASCII" );
        }
        catch( final UnsupportedEncodingException uee )
        {
            return new InputStreamReader( input );
        }
    }

    /**
     * Parse a line into name and value pairs
     *
     * @param line the line to be parsed
     * @throws ManifestException if the line does not contain a colon
     *      separating the name and value
     */
    public static void parse( final Attribute attribute, final String line )
        throws ManifestException
    {
        final int index = line.indexOf( ": " );
        if( index == -1 )
        {
            throw new ManifestException( "Manifest line \"" + line + "\" is not valid as it does not " +
                                         "contain a name and a value separated by ': ' " );
        }
        final String name = line.substring( 0, index );
        final String value = line.substring( index + 2 );
        attribute.setName( name );
        attribute.setValue( value );
    }

    public static void write( final Attribute attribute, final PrintWriter writer )
        throws IOException
    {
        final String name = attribute.getName();
        final String value = attribute.getValue();
        String line = name + ": " + value;
        while( line.getBytes().length > MAX_LINE_LENGTH )
        {
            // try to find a MAX_LINE_LENGTH byte section
            int breakIndex = MAX_LINE_LENGTH;
            String section = line.substring( 0, breakIndex );
            while( section.getBytes().length > MAX_LINE_LENGTH && breakIndex > 0 )
            {
                breakIndex--;
                section = line.substring( 0, breakIndex );
            }
            if( breakIndex == 0 )
            {
                throw new IOException( "Unable to write manifest line " + name + ": " + value );
            }
            writer.println( section );
            line = " " + line.substring( breakIndex );
        }
        writer.println( line );
    }

    /**
     * Write the manifest out to a print writer.
     *
     * @param writer the Writer to which the manifest is written
     * @throws IOException if the manifest cannot be written
     */
    public static void write( Manifest manifest, PrintWriter writer )
        throws IOException
    {
        final String sigVersionKey = Attributes.Name.SIGNATURE_VERSION.toString();

        writer.println( Attributes.Name.MANIFEST_VERSION + ": " + manifest.getManifestVersion() );

        final String signatureVersion =
            manifest.getMainSection().getAttributeValue( sigVersionKey );
        if( signatureVersion != null )
        {
            writer.println( Attributes.Name.SIGNATURE_VERSION + ": " + signatureVersion );
            manifest.getMainSection().removeAttribute( sigVersionKey );
        }
        manifest.getMainSection().write( writer );
        if( signatureVersion != null )
        {
            try
            {
                manifest.getMainSection().addAttribute( new Attribute( sigVersionKey, signatureVersion ) );
            }
            catch( ManifestException e )
            {
                // shouldn't happen - ignore
            }
        }

        final Section[] sections = manifest.getSections();
        for( int i = 0; i < sections.length; i++ )
        {
            sections[ i ].write( writer );
        }
    }
}
