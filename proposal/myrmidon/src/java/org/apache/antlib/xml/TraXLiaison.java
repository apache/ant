/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.avalon.framework.logger.AbstractLogEnabled;

/**
 * Concrete liaison for XSLT processor implementing TraX. (ie JAXP 1.1)
 *
 * @author <a href="mailto:rubys@us.ibm.com">Sam Ruby</a>
 * @author <a href="mailto:dims@yahoo.com">Davanum Srinivas</a>
 * @author <a href="mailto:sbailliez@apache.org">Stephane Bailliez</a>
 */
public class TraXLiaison
    extends AbstractLogEnabled
    implements XSLTLiaison, ErrorListener
{
    /**
     * The trax TransformerFactory
     */
    private TransformerFactory tfactory;

    /**
     * stylesheet stream, close it asap
     */
    private FileInputStream xslStream;

    /**
     * Stylesheet template
     */
    private Templates templates;

    /**
     * transformer
     */
    private Transformer transformer;

    public TraXLiaison()
        throws Exception
    {
        tfactory = TransformerFactory.newInstance();
        tfactory.setErrorListener( this );
    }

    public void setOutputtype( String type )
        throws Exception
    {
        transformer.setOutputProperty( OutputKeys.METHOD, type );
    }

    //------------------- IMPORTANT
    // 1) Don't use the StreamSource(File) ctor. It won't work with
    // xalan prior to 2.2 because of systemid bugs.

    // 2) Use a stream so that you can close it yourself quickly
    // and avoid keeping the handle until the object is garbaged.
    // (always keep control), otherwise you won't be able to delete
    // the file quickly on windows.

    // 3) Always set the systemid to the source for imports, includes...
    // in xsl and xml...

    public void setStylesheet( File stylesheet )
        throws Exception
    {
        xslStream = new FileInputStream( stylesheet );
        StreamSource src = new StreamSource( xslStream );
        src.setSystemId( getSystemId( stylesheet ) );
        templates = tfactory.newTemplates( src );
        transformer = templates.newTransformer();
        transformer.setErrorListener( this );
    }

    public void addParam( String name, String value )
    {
        transformer.setParameter( name, value );
    }

    public void error( TransformerException e )
    {
        logError( e, "Error" );
    }

    public void fatalError( TransformerException e )
    {
        logError( e, "Fatal Error" );
    }

    public void transform( File infile, File outfile )
        throws Exception
    {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try
        {
            fis = new FileInputStream( infile );
            fos = new FileOutputStream( outfile );
            StreamSource src = new StreamSource( fis );
            src.setSystemId( getSystemId( infile ) );
            StreamResult res = new StreamResult( fos );
            // not sure what could be the need of this...
            res.setSystemId( getSystemId( outfile ) );

            transformer.transform( src, res );
        }
        finally
        {
            // make sure to close all handles, otherwise the garbage
            // collector will close them...whenever possible and
            // Windows may complain about not being able to delete files.
            try
            {
                if( xslStream != null )
                {
                    xslStream.close();
                }
            }
            catch( IOException ignored )
            {
            }
            try
            {
                if( fis != null )
                {
                    fis.close();
                }
            }
            catch( IOException ignored )
            {
            }
            try
            {
                if( fos != null )
                {
                    fos.close();
                }
            }
            catch( IOException ignored )
            {
            }
        }
    }

    public void warning( TransformerException e )
    {
        logError( e, "Warning" );
    }

    // make sure that the systemid is made of '/' and not '\' otherwise
    // crimson will complain that it cannot resolve relative entities
    // because it grabs the base uri via lastIndexOf('/') without
    // making sure it is really a /'ed path
    protected String getSystemId( File file )
    {
        String path = file.getAbsolutePath();
        path = path.replace( '\\', '/' );
        return FILE_PROTOCOL_PREFIX + path;
    }

    private void logError( TransformerException e, String type )
    {
        StringBuffer msg = new StringBuffer();
        if( e.getLocator() != null )
        {
            if( e.getLocator().getSystemId() != null )
            {
                String url = e.getLocator().getSystemId();
                if( url.startsWith( "file:///" ) )
                    url = url.substring( 8 );
                msg.append( url );
            }
            else
            {
                msg.append( "Unknown file" );
            }
            if( e.getLocator().getLineNumber() != -1 )
            {
                msg.append( ":" + e.getLocator().getLineNumber() );
                if( e.getLocator().getColumnNumber() != -1 )
                {
                    msg.append( ":" + e.getLocator().getColumnNumber() );
                }
            }
        }
        msg.append( ": " + type + "! " );
        msg.append( e.getMessage() );
        if( e.getCause() != null )
        {
            msg.append( " Cause: " + e.getCause() );
        }

        getLogger().info( msg.toString() );
    }
}
