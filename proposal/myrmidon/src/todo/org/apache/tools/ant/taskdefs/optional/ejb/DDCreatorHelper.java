/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.ejb;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import javax.ejb.deployment.DeploymentDescriptor;

/**
 * A helper class which performs the actual work of the ddcreator task. This
 * class is run with a classpath which includes the weblogic tools and the home
 * and remote interface class files referenced in the deployment descriptors
 * being built.
 *
 * @author <a href="mailto:conor@cortexebusiness.com.au">Conor MacNeill</a> ,
 *      Cortex ebusiness Pty Limited
 */
public class DDCreatorHelper
{

    /**
     * The descriptor text files for which a serialised descriptor is to be
     * created.
     */
    String[] descriptors;
    /**
     * The root directory of the tree containing the textual deployment
     * desciptors.
     */
    private File descriptorDirectory;

    /**
     * The directory where generated serialised desployment descriptors are
     * written.
     */
    private File generatedFilesDirectory;

    /**
     * Initialise the helper with the command arguments.
     *
     * @param args Description of Parameter
     */
    private DDCreatorHelper( String[] args )
    {
        int index = 0;
        descriptorDirectory = new File( args[ index++ ] );
        generatedFilesDirectory = new File( args[ index++ ] );

        descriptors = new String[ args.length - index ];
        for( int i = 0; index < args.length; ++i )
        {
            descriptors[ i ] = args[ index++ ];
        }
    }

    /**
     * The main method. The main method creates an instance of the
     * DDCreatorHelper, passing it the args which it then processes.
     *
     * @param args The command line arguments
     * @exception Exception Description of Exception
     */
    public static void main( String[] args )
        throws Exception
    {
        DDCreatorHelper helper = new DDCreatorHelper( args );
        helper.process();
    }

    /**
     * Do the actual work. The work proceeds by examining each descriptor given.
     * If the serialised file does not exist or is older than the text
     * description, the weblogic DDCreator tool is invoked directly to build the
     * serialised descriptor.
     *
     * @exception Exception Description of Exception
     */
    private void process()
        throws Exception
    {
        for( int i = 0; i < descriptors.length; ++i )
        {
            String descriptorName = descriptors[ i ];
            File descriptorFile = new File( descriptorDirectory, descriptorName );

            int extIndex = descriptorName.lastIndexOf( "." );
            String serName = null;
            if( extIndex != -1 )
            {
                serName = descriptorName.substring( 0, extIndex ) + ".ser";
            }
            else
            {
                serName = descriptorName + ".ser";
            }
            File serFile = new File( generatedFilesDirectory, serName );

            // do we need to regenerate the file
            if( !serFile.exists() || serFile.lastModified() < descriptorFile.lastModified()
                || regenerateSerializedFile( serFile ) )
            {

                String[] args = {"-noexit",
                                 "-d", serFile.getParent(),
                                 "-outputfile", serFile.getName(),
                                 descriptorFile.getPath()};
                try
                {
                    weblogic.ejb.utils.DDCreator.main( args );
                }
                catch( Exception e )
                {
                    // there was an exception - run with no exit to get proper error
                    String[] newArgs = {"-d", generatedFilesDirectory.getPath(),
                                        "-outputfile", serFile.getName(),
                                        descriptorFile.getPath()};
                    weblogic.ejb.utils.DDCreator.main( newArgs );
                }
            }
        }
    }

    /**
     * EJBC will fail if the serialized descriptor file does not match the bean
     * classes. You can test for this by trying to load the deployment
     * descriptor. If it fails, the serialized file needs to be regenerated
     * because the associated class files don't match.
     *
     * @param serFile Description of Parameter
     * @return Description of the Returned Value
     */
    private boolean regenerateSerializedFile( File serFile )
    {
        try
        {

            FileInputStream fis = new FileInputStream( serFile );
            ObjectInputStream ois = new ObjectInputStream( fis );
            DeploymentDescriptor dd = (DeploymentDescriptor)ois.readObject();
            fis.close();

            // Since the descriptor read properly, everything should be o.k.
            return false;
        }
        catch( Exception e )
        {

            // Weblogic will throw an error if the deployment descriptor does
            // not match the class files.
            return true;
        }
    }
}
