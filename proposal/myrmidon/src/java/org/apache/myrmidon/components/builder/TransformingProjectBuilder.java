/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.builder;

import java.io.InputStream;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.SAXConfigurationHandler;
import org.apache.myrmidon.interfaces.builder.ProjectException;

/**
 * A Project Builder which performs an XSL transformation on a project.
 *
 * @author <a href="mailto:darrell@apache.org">Darrell DeBoer</a>
 * @version $Revision$ $Date$
 *
 * @ant.type type="project-builder" name="ant-transform"
 */
public class TransformingProjectBuilder
    extends DefaultProjectBuilder
{
    private static final Resources REZ =
        ResourceManager.getPackageResources( TransformingProjectBuilder.class );

    private static final String STYLESHEET = "ant1convert.xsl";
    private Transformer m_transformer;

    /**
     * Builds a project Configuration from a project file, applying the
     * ant1 conversion stylesheet.
     * @param systemID the XML system id for the project file
     * @return the project configuration
     * @throws ProjectException if a parse error occurs
     */
    protected Configuration parseProject( String systemID )
        throws ProjectException
    {
        if( getLogger().isDebugEnabled() )
        {
            final String message = REZ.getString( "ant.project-convert.notice" );
            getLogger().debug( message );
        }

        try
        {
            // Create a XSLT source for the build file.
            Source source = new StreamSource( systemID );

            // Create a configuration handler for the output.
            final SAXConfigurationHandler handler = new SAXConfigurationHandler();
            Result result = new SAXResult( handler );

            // Perform the transformation.
            getTransformer().transform( source, result );

            return handler.getConfiguration();
        }
        catch( Exception e )
        {
            throw new ProjectException( REZ.getString( "ant.project-convert.error" ),
                                        e );
        }
    }

    /**
     * Lazy load a Transformer with the conversion stylesheet.
     * @return the initialised Transformer
     * @throws TransformerConfigurationException
     */
    private Transformer getTransformer()
        throws TransformerConfigurationException
    {
        // Lazy loading of stylesheet source.
        if( m_transformer == null )
        {
            InputStream stylesheet =
                this.getClass().getResourceAsStream( STYLESHEET );
            StreamSource stylesheetSource = new StreamSource( stylesheet );
            TransformerFactory xformFactory = TransformerFactory.newInstance();
            m_transformer = xformFactory.newTransformer( stylesheetSource );
        }
        return m_transformer;
    }
}
