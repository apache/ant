/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Hashtable;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.Task;

/**
 * Creates a partial DTD for Ant from the currently known tasks.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @version $Revision$
 */

public class AntStructure extends Task
{
    private final String lSep = System.getProperty( "line.separator" );

    private final String BOOLEAN = "%boolean;";
    private final String TASKS = "%tasks;";
    private final String TYPES = "%types;";

    private Hashtable visited = new Hashtable();

    private File output;

    /**
     * The output file.
     *
     * @param output The new Output value
     */
    public void setOutput( File output )
    {
        this.output = output;
    }

    public void execute()
        throws TaskException
    {

        if( output == null )
        {
            throw new TaskException( "output attribute is required" );
        }

        PrintWriter out = null;
        try
        {
            try
            {
                out = new PrintWriter( new OutputStreamWriter( new FileOutputStream( output ), "UTF8" ) );
            }
            catch( UnsupportedEncodingException ue )
            {
                /*
                 * Plain impossible with UTF8, see
                 * http://java.sun.com/products/jdk/1.2/docs/guide/internat/encoding.doc.html
                 *
                 * fallback to platform specific anyway.
                 */
                out = new PrintWriter( new FileWriter( output ) );
            }

            printHead( out, project.getTaskDefinitions().keys(),
                       project.getDataTypeDefinitions().keys() );

            printTargetDecl( out );

            Enumeration dataTypes = project.getDataTypeDefinitions().keys();
            while( dataTypes.hasMoreElements() )
            {
                String typeName = (String)dataTypes.nextElement();
                printElementDecl( out, typeName,
                                  (Class)project.getDataTypeDefinitions().get( typeName ) );
            }

            Enumeration tasks = project.getTaskDefinitions().keys();
            while( tasks.hasMoreElements() )
            {
                String taskName = (String)tasks.nextElement();
                printElementDecl( out, taskName,
                                  (Class)project.getTaskDefinitions().get( taskName ) );
            }

            printTail( out );

        }
        catch( IOException ioe )
        {
            throw new TaskException( "Error writing " + output.getAbsolutePath(),
                                     ioe );
        }
        finally
        {
            if( out != null )
            {
                out.close();
            }
        }
    }

    /**
     * Does this String match the XML-NMTOKEN production?
     *
     * @param s Description of Parameter
     * @return The Nmtoken value
     */
    protected boolean isNmtoken( String s )
    {
        for( int i = 0; i < s.length(); i++ )
        {
            char c = s.charAt( i );
            // XXX - we are ommitting CombiningChar and Extender here
            if( !Character.isLetterOrDigit( c ) &&
                c != '.' && c != '-' &&
                c != '_' && c != ':' )
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Do the Strings all match the XML-NMTOKEN production? <p>
     *
     * Otherwise they are not suitable as an enumerated attribute, for example.
     * </p>
     *
     * @param s Description of Parameter
     * @return Description of the Returned Value
     */
    protected boolean areNmtokens( String[] s )
    {
        for( int i = 0; i < s.length; i++ )
        {
            if( !isNmtoken( s[ i ] ) )
            {
                return false;
            }
        }
        return true;
    }

    private void printElementDecl( PrintWriter out, String name, Class element )
        throws TaskException
    {

        if( visited.containsKey( name ) )
        {
            return;
        }
        visited.put( name, "" );

        /*
        IntrospectionHelper ih = null;
        try
        {
            ih = IntrospectionHelper.getHelper( element );
        }
        catch( Throwable t )
        {
            // FIXME: failed to load the class properly.
            // should we print a warning here?
            return;
        }

        StringBuffer sb = new StringBuffer( "<!ELEMENT " );
        sb.append( name ).append( " " );

        if( org.apache.tools.ant.types.Reference.class.equals( element ) )
        {
            sb.append( "EMPTY>" ).append( lSep );
            sb.append( "<!ATTLIST " ).append( name );
            sb.append( lSep ).append( "          id ID #IMPLIED" );
            sb.append( lSep ).append( "          refid IDREF #IMPLIED" );
            sb.append( ">" ).append( lSep );
            out.println( sb );
            return;
        }

        Vector v = new Vector();
        if( ih.supportsCharacters() )
        {
            v.addElement( "#PCDATA" );
        }

        if( TaskContainer.class.isAssignableFrom( element ) )
        {
            v.addElement( TASKS );
        }

        Enumeration enum = ih.getNestedElements();
        while( enum.hasMoreElements() )
        {
            v.addElement( (String)enum.nextElement() );
        }

        if( v.isEmpty() )
        {
            sb.append( "EMPTY" );
        }
        else
        {
            sb.append( "(" );
            for( int i = 0; i < v.size(); i++ )
            {
                if( i != 0 )
                {
                    sb.append( " | " );
                }
                sb.append( v.elementAt( i ) );
            }
            sb.append( ")" );
            if( v.size() > 1 || !v.elementAt( 0 ).equals( "#PCDATA" ) )
            {
                sb.append( "*" );
            }
        }
        sb.append( ">" );
        out.println( sb );

        sb.setLength( 0 );
        sb.append( "<!ATTLIST " ).append( name );
        sb.append( lSep ).append( "          id ID #IMPLIED" );

        enum = ih.getAttributes();
        while( enum.hasMoreElements() )
        {
            String attrName = (String)enum.nextElement();
            if( "id".equals( attrName ) )
                continue;

            sb.append( lSep ).append( "          " ).append( attrName ).append( " " );
            Class type = ih.getAttributeType( attrName );
            if( type.equals( java.lang.Boolean.class ) ||
                type.equals( java.lang.Boolean.TYPE ) )
            {
                sb.append( BOOLEAN ).append( " " );
            }
            else if( org.apache.tools.ant.types.Reference.class.isAssignableFrom( type ) )
            {
                sb.append( "IDREF " );
            }
            else if( org.apache.tools.ant.types.EnumeratedAttribute.class.isAssignableFrom( type ) )
            {
                try
                {
                    EnumeratedAttribute ea =
                        (EnumeratedAttribute)type.newInstance();
                    String[] values = ea.getValues();
                    if( values == null
                        || values.length == 0
                        || !areNmtokens( values ) )
                    {
                        sb.append( "CDATA " );
                    }
                    else
                    {
                        sb.append( "(" );
                        for( int i = 0; i < values.length; i++ )
                        {
                            if( i != 0 )
                            {
                                sb.append( " | " );
                            }
                            sb.append( values[ i ] );
                        }
                        sb.append( ") " );
                    }
                }
                catch( InstantiationException ie )
                {
                    sb.append( "CDATA " );
                }
                catch( IllegalAccessException ie )
                {
                    sb.append( "CDATA " );
                }
            }
            else
            {
                sb.append( "CDATA " );
            }
            sb.append( "#IMPLIED" );
        }
        sb.append( ">" ).append( lSep );
        out.println( sb );

        for( int i = 0; i < v.size(); i++ )
        {
            String nestedName = (String)v.elementAt( i );
            if( !"#PCDATA".equals( nestedName ) &&
                !TASKS.equals( nestedName ) &&
                !TYPES.equals( nestedName )
            )
            {
                printElementDecl( out, nestedName, ih.getElementType( nestedName ) );
            }
        }
        */
    }

    private void printHead( PrintWriter out, Enumeration tasks,
                            Enumeration types )
    {
        out.println( "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" );
        out.println( "<!ENTITY % boolean \"(true|false|on|off|yes|no)\">" );
        out.print( "<!ENTITY % tasks \"" );
        boolean first = true;
        while( tasks.hasMoreElements() )
        {
            String taskName = (String)tasks.nextElement();
            if( !first )
            {
                out.print( " | " );
            }
            else
            {
                first = false;
            }
            out.print( taskName );
        }
        out.println( "\">" );
        out.print( "<!ENTITY % types \"" );
        first = true;
        while( types.hasMoreElements() )
        {
            String typeName = (String)types.nextElement();
            if( !first )
            {
                out.print( " | " );
            }
            else
            {
                first = false;
            }
            out.print( typeName );
        }
        out.println( "\">" );

        out.println( "" );

        out.print( "<!ELEMENT project (target | property | taskdef | " );
        out.print( TYPES );
        out.println( ")*>" );
        out.println( "<!ATTLIST project" );
        out.println( "          name    CDATA #REQUIRED" );
        out.println( "          default CDATA #REQUIRED" );
        out.println( "          basedir CDATA #IMPLIED>" );
        out.println( "" );
    }

    private void printTail( PrintWriter out )
    {
    }

    private void printTargetDecl( PrintWriter out )
    {
        out.print( "<!ELEMENT target (" );
        out.print( TASKS );
        out.print( " | " );
        out.print( TYPES );
        out.println( ")*>" );
        out.println( "" );

        out.println( "<!ATTLIST target" );
        out.println( "          id          ID    #IMPLIED" );
        out.println( "          name        CDATA #REQUIRED" );
        out.println( "          if          CDATA #IMPLIED" );
        out.println( "          unless      CDATA #IMPLIED" );
        out.println( "          depends     CDATA #IMPLIED" );
        out.println( "          description CDATA #IMPLIED>" );
        out.println( "" );
    }

}
