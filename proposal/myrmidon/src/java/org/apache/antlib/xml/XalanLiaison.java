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
import org.apache.myrmidon.api.TaskException;
import org.apache.xalan.xslt.XSLTInputSource;
import org.apache.xalan.xslt.XSLTProcessor;
import org.apache.xalan.xslt.XSLTProcessorFactory;
import org.apache.xalan.xslt.XSLTResultTarget;

/**
 * Concrete liaison for Xalan 1.x API.
 *
 * @author <a href="mailto:rubys@us.ibm.com">Sam Ruby</a>
 * @author <a href="mailto:sbailliez@apache.org">Stephane Bailliez</a>
 */
public class XalanLiaison
    implements XSLTLiaison
{
    private XSLTProcessor processor;
    private File stylesheet;

    public XalanLiaison()
        throws Exception
    {
        processor = XSLTProcessorFactory.getProcessor();
    }

    public void setOutputtype( String type )
        throws Exception
    {
        if( !type.equals( "xml" ) )
            throw new TaskException( "Unsupported output type: " + type );
    }

    public void setStylesheet( File stylesheet )
        throws Exception
    {
        this.stylesheet = stylesheet;
    }

    public void addParam( String name, String value )
    {
        processor.setStylesheetParam( name, value );
    }

    public void transform( File infile, File outfile )
        throws Exception
    {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        FileInputStream xslStream = null;
        try
        {
            xslStream = new FileInputStream( stylesheet );
            fis = new FileInputStream( infile );
            fos = new FileOutputStream( outfile );
            // systemid such as file:/// + getAbsolutePath() are considered
            // invalid here...
            XSLTInputSource xslSheet = new XSLTInputSource( xslStream );
            xslSheet.setSystemId( stylesheet.getAbsolutePath() );
            XSLTInputSource src = new XSLTInputSource( fis );
            src.setSystemId( infile.getAbsolutePath() );
            XSLTResultTarget res = new XSLTResultTarget( fos );
            processor.process( src, xslSheet, res );
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
}//-- XalanLiaison
