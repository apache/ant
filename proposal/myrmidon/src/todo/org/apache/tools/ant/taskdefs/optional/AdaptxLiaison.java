/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.XSLTLiaison;
import org.exolab.adaptx.xslt.XSLTProcessor;
import org.exolab.adaptx.xslt.XSLTReader;
import org.exolab.adaptx.xslt.XSLTStylesheet;

/**
 * @author <a href="mailto:blandin@intalio.com">Arnaud Blandin</a>
 * @version $Revision$ $Date$
 */
public class AdaptxLiaison implements XSLTLiaison
{

    protected XSLTProcessor processor;
    protected XSLTStylesheet xslSheet;

    public AdaptxLiaison()
    {
        processor = new XSLTProcessor();
    }

    public void setOutputtype( String type )
        throws Exception
    {
        if( !type.equals( "xml" ) )
            throw new BuildException( "Unsupported output type: " + type );
    }

    public void setStylesheet( File fileName )
        throws Exception
    {
        XSLTReader xslReader = new XSLTReader();
        xslSheet = xslReader.read( fileName.getAbsolutePath() );
    }

    public void addParam( String name, String expression )
    {
        processor.setProperty( name, expression );
    }

    public void transform( File infile, File outfile )
        throws Exception
    {
        FileOutputStream fos = new FileOutputStream( outfile );
        OutputStreamWriter out = new OutputStreamWriter( fos, "UTF8" );
        processor.process( infile.getAbsolutePath(), xslSheet, out );
    }

}//-- AdaptxLiaison
