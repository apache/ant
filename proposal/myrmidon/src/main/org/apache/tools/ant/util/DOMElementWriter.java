/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.util;

import java.io.IOException;
import java.io.Writer;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
 * Writes a DOM tree to a given Writer. <p>
 *
 * Utility class used by {@link org.apache.tools.ant.XmlLogger XmlLogger} and
 * org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter
 * XMLJUnitResultFormatter}.</p>
 *
 * @author The original author of XmlLogger
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:sbailliez@imediation.com">Stephane Bailliez</tt>
 */
public class DOMElementWriter
{

    private static String lSep = System.getProperty( "line.separator" );
    private StringBuffer sb = new StringBuffer();

    /**
     * Don't try to be too smart but at least recognize the predefined entities.
     */
    protected String[] knownEntities = {"gt", "amp", "lt", "apos", "quot"};

    /**
     * Is the given argument a character or entity reference?
     *
     * @param ent Description of Parameter
     * @return The Reference value
     */
    public boolean isReference( String ent )
    {
        if( !( ent.charAt( 0 ) == '&' ) || !ent.endsWith( ";" ) )
        {
            return false;
        }

        if( ent.charAt( 1 ) == '#' )
        {
            if( ent.charAt( 2 ) == 'x' )
            {
                try
                {
                    Integer.parseInt( ent.substring( 3, ent.length() - 1 ), 16 );
                    return true;
                }
                catch( NumberFormatException nfe )
                {
                    return false;
                }
            }
            else
            {
                try
                {
                    Integer.parseInt( ent.substring( 2, ent.length() - 1 ) );
                    return true;
                }
                catch( NumberFormatException nfe )
                {
                    return false;
                }
            }
        }

        String name = ent.substring( 1, ent.length() - 1 );
        for( int i = 0; i < knownEntities.length; i++ )
        {
            if( name.equals( knownEntities[ i ] ) )
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Escape &lt;, &gt; &amp; &apos; and &quot; as their entities.
     *
     * @param value Description of Parameter
     * @return Description of the Returned Value
     */
    public String encode( String value )
    {
        sb.setLength( 0 );
        for( int i = 0; i < value.length(); i++ )
        {
            char c = value.charAt( i );
            switch( c )
            {
                case '<':
                    sb.append( "&lt;" );
                    break;
                case '>':
                    sb.append( "&gt;" );
                    break;
                case '\'':
                    sb.append( "&apos;" );
                    break;
                case '\"':
                    sb.append( "&quot;" );
                    break;
                case '&':
                    int nextSemi = value.indexOf( ";", i );
                    if( nextSemi < 0
                        || !isReference( value.substring( i, nextSemi + 1 ) ) )
                    {
                        sb.append( "&amp;" );
                    }
                    else
                    {
                        sb.append( '&' );
                    }
                    break;
                default:
                    sb.append( c );
                    break;
            }
        }
        return sb.toString();
    }

    /**
     * Writes a DOM tree to a stream.
     *
     * @param element the Root DOM element of the tree
     * @param out where to send the output
     * @param indent number of
     * @param indentWith strings, that should be used to indent the
     *      corresponding tag.
     * @exception IOException Description of Exception
     */
    public void write( Element element, Writer out, int indent,
                       String indentWith )
        throws IOException
    {

        // Write indent characters
        for( int i = 0; i < indent; i++ )
        {
            out.write( indentWith );
        }

        // Write element
        out.write( "<" );
        out.write( element.getTagName() );

        // Write attributes
        NamedNodeMap attrs = element.getAttributes();
        for( int i = 0; i < attrs.getLength(); i++ )
        {
            Attr attr = (Attr)attrs.item( i );
            out.write( " " );
            out.write( attr.getName() );
            out.write( "=\"" );
            out.write( encode( attr.getValue() ) );
            out.write( "\"" );
        }
        out.write( ">" );

        // Write child elements and text
        boolean hasChildren = false;
        NodeList children = element.getChildNodes();
        for( int i = 0; i < children.getLength(); i++ )
        {
            Node child = children.item( i );

            switch( child.getNodeType() )
            {

                case Node.ELEMENT_NODE:
                    if( !hasChildren )
                    {
                        out.write( lSep );
                        hasChildren = true;
                    }
                    write( (Element)child, out, indent + 1, indentWith );
                    break;
                case Node.TEXT_NODE:
                    out.write( encode( child.getNodeValue() ) );
                    break;
                case Node.CDATA_SECTION_NODE:
                    out.write( "<![CDATA[" );
                    out.write( ( (Text)child ).getData() );
                    out.write( "]]>" );
                    break;
                case Node.ENTITY_REFERENCE_NODE:
                    out.write( '&' );
                    out.write( child.getNodeName() );
                    out.write( ';' );
                    break;
                case Node.PROCESSING_INSTRUCTION_NODE:
                    out.write( "<?" );
                    out.write( child.getNodeName() );
                    String data = child.getNodeValue();
                    if( data != null && data.length() > 0 )
                    {
                        out.write( ' ' );
                        out.write( data );
                    }
                    out.write( "?>" );
                    break;
            }
        }

        // If we had child elements, we need to indent before we close
        // the element, otherwise we're on the same line and don't need
        // to indent
        if( hasChildren )
        {
            for( int i = 0; i < indent; i++ )
            {
                out.write( indentWith );
            }
        }

        // Write element close
        out.write( "</" );
        out.write( element.getTagName() );
        out.write( ">" );
        out.write( lSep );
        out.flush();
    }
}
