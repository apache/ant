/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.ejb;

import java.io.File;
import java.util.Hashtable;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.Project;

public class WeblogicTOPLinkDeploymentTool extends WeblogicDeploymentTool
{

    private final static String TL_DTD_LOC = "http://www.objectpeople.com/tlwl/dtd/toplink-cmp_2_5_1.dtd";
    private String toplinkDTD;

    private String toplinkDescriptor;

    /**
     * Setter used to store the name of the toplink descriptor.
     *
     * @param inString the string to use as the descriptor name.
     */
    public void setToplinkdescriptor( String inString )
    {
        this.toplinkDescriptor = inString;
    }

    /**
     * Setter used to store the location of the toplink DTD file. This is
     * expected to be an URL (file or otherwise). If running this on NT using a
     * file URL, the safest thing would be to not use a drive spec in the URL
     * and make sure the file resides on the drive that ANT is running from.
     * This will keep the setting in the build XML platform independent.
     *
     * @param inString the string to use as the DTD location.
     */
    public void setToplinkdtd( String inString )
    {
        this.toplinkDTD = inString;
    }

    /**
     * Called to validate that the tool parameters have been configured.
     *
     * @exception TaskException Description of Exception
     */
    public void validateConfigured()
        throws TaskException
    {
        super.validateConfigured();
        if( toplinkDescriptor == null )
        {
            throw new TaskException( "The toplinkdescriptor attribute must be specified" );
        }
    }

    protected DescriptorHandler getDescriptorHandler( File srcDir )
    {
        DescriptorHandler handler = super.getDescriptorHandler( srcDir );
        if( toplinkDTD != null )
        {
            handler.registerDTD( "-//The Object People, Inc.//DTD TOPLink for WebLogic CMP 2.5.1//EN",
                                 toplinkDTD );
        }
        else
        {
            handler.registerDTD( "-//The Object People, Inc.//DTD TOPLink for WebLogic CMP 2.5.1//EN",
                                 TL_DTD_LOC );
        }
        return handler;
    }

    /**
     * Add any vendor specific files which should be included in the EJB Jar.
     *
     * @param ejbFiles The feature to be added to the VendorFiles attribute
     * @param ddPrefix The feature to be added to the VendorFiles attribute
     */
    protected void addVendorFiles( Hashtable ejbFiles, String ddPrefix )
    {
        super.addVendorFiles( ejbFiles, ddPrefix );
        // Then the toplink deployment descriptor

        // Setup a naming standard here?.


        File toplinkDD = new File( getConfig().descriptorDir, ddPrefix + toplinkDescriptor );

        if( toplinkDD.exists() )
        {
            ejbFiles.put( META_DIR + toplinkDescriptor,
                          toplinkDD );
        }
        else
        {
            log( "Unable to locate toplink deployment descriptor. It was expected to be in " +
                 toplinkDD.getPath(), Project.MSG_WARN );
        }
    }
}
