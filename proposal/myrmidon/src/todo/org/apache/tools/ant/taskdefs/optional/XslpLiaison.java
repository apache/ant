/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional;
import com.kvisco.xsl.XSLProcessor;
import com.kvisco.xsl.XSLReader;
import com.kvisco.xsl.XSLStylesheet;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.XSLTLiaison;



/**
 * Concrete liaison for XSLP
 *
 * @author <a href="mailto:rubys@us.ibm.com">Sam Ruby</a>
 * @author <a href="mailto:sbailliez@apache.org">Stephane Bailliez</a>
 */
public class XslpLiaison implements XSLTLiaison
{

    protected XSLProcessor processor;
    protected XSLStylesheet xslSheet;

    public XslpLiaison()
    {
        processor = new XSLProcessor();
        // uh ?! I'm forced to do that otherwise a setProperty crashes with NPE !
        // I don't understand why the property map is static though...
        // how can we do multithreading w/ multiple identical parameters ?
        processor.getProperty( "dummy-to-init-properties-map" );
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
        XSLReader xslReader = new XSLReader();
        // a file:/// + getAbsolutePath() does not work here
        // it is really the pathname
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
        // XSLP does not support encoding...we're in hot water.
        OutputStreamWriter out = new OutputStreamWriter( fos, "UTF8" );
        processor.process( infile.getAbsolutePath(), xslSheet, out );
    }

}//-- XSLPLiaison
