/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.components.builder;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.avalon.framework.configuration.SAXConfigurationHandler;
import org.apache.avalon.excalibur.io.FileUtil;

/**
 * Default implementation to construct project from a build file.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class XSLProjectBuilder
    extends DefaultProjectBuilder
{
    protected void process( final String sourceID,
                            final SAXConfigurationHandler handler )
        throws Exception
    {
        final String xslSheet = FileUtil.removeExtension( sourceID ) + ".xsl";

        // Create a transform factory instance.
        final TransformerFactory factory = TransformerFactory.newInstance();

        // Create a transformer for the stylesheet.
        final Transformer transformer = factory.newTransformer( new StreamSource( xslSheet ) );

        final SAXResult result = new SAXResult( handler );

        //Make a debug option for this
        //transformer.transform( new StreamSource( sourceID ), new StreamResult( System.out ) );

        transformer.transform( new StreamSource( sourceID ), result );
    }
}
