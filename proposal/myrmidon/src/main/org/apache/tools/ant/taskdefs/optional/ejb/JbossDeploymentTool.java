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
import org.apache.tools.ant.Project;

/**
 * The deployment tool to add the jboss specific deployment descriptor to the
 * ejb jar file. Jboss only requires one additional file jboss.xml and does not
 * require any additional compilation.
 *
 * @author <a href="mailto:p.austin@talk21.com">Paul Austin</a>
 * @version 1.0
 * @see EjbJar#createJboss
 */
public class JbossDeploymentTool extends GenericDeploymentTool
{
    protected final static String JBOSS_DD = "jboss.xml";
    protected final static String JBOSS_CMPD = "jaws.xml";

    /**
     * Instance variable that stores the suffix for the jboss jarfile.
     */
    private String jarSuffix = ".jar";

    /**
     * Add any vendor specific files which should be included in the EJB Jar.
     *
     * @param ejbFiles The feature to be added to the VendorFiles attribute
     * @param ddPrefix The feature to be added to the VendorFiles attribute
     */
    protected void addVendorFiles( Hashtable ejbFiles, String ddPrefix )
    {
        File jbossDD = new File( getConfig().descriptorDir, ddPrefix + JBOSS_DD );
        if( jbossDD.exists() )
        {
            ejbFiles.put( META_DIR + JBOSS_DD, jbossDD );
        }
        else
        {
            log( "Unable to locate jboss deployment descriptor. It was expected to be in " + jbossDD.getPath(), Project.MSG_WARN );
            return;
        }

        File jbossCMPD = new File( getConfig().descriptorDir, ddPrefix + JBOSS_CMPD );
        if( jbossCMPD.exists() )
        {
            ejbFiles.put( META_DIR + JBOSS_CMPD, jbossCMPD );
        }
    }

    /**
     * Get the vendor specific name of the Jar that will be output. The
     * modification date of this jar will be checked against the dependent bean
     * classes.
     *
     * @param baseName Description of Parameter
     * @return The VendorOutputJarFile value
     */
    File getVendorOutputJarFile( String baseName )
    {
        return new File( getDestDir(), baseName + jarSuffix );
    }
}
