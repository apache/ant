/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs.javacc;

import java.io.File;
import java.util.Enumeration;
import java.util.Hashtable;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.java.ExecuteJava;
import org.apache.myrmidon.framework.file.Path;
import org.apache.tools.todo.types.PathUtil;

/**
 * Taskdef for the JJTree compiler compiler.
 *
 * @author thomas.haas@softwired-inc.com
 * @author Michael Saunders <a href="mailto:michael@amtec.com">michael@amtec.com
 *      </a>
 */
public class JJTree
    extends AbstractTask
{
    // keys to optional attributes
    private final static String BUILD_NODE_FILES = "BUILD_NODE_FILES";
    private final static String MULTI = "MULTI";
    private final static String NODE_DEFAULT_VOID = "NODE_DEFAULT_VOID";
    private final static String NODE_FACTORY = "NODE_FACTORY";
    private final static String NODE_SCOPE_HOOK = "NODE_SCOPE_HOOK";
    private final static String NODE_USES_PARSER = "NODE_USES_PARSER";
    private final static String STATIC = "STATIC";
    private final static String VISITOR = "VISITOR";

    private final static String NODE_PACKAGE = "NODE_PACKAGE";
    private final static String VISITOR_EXCEPTION = "VISITOR_EXCEPTION";
    private final static String NODE_PREFIX = "NODE_PREFIX";

    private final Hashtable optionalAttrs = new Hashtable();

    // required attributes
    private File outputDirectory = null;
    private File target = null;
    private File javaccHome = null;

    public void setBuildnodefiles( boolean buildNodeFiles )
    {
        optionalAttrs.put( BUILD_NODE_FILES, new Boolean( buildNodeFiles ) );
    }

    public void setJavacchome( File javaccHome )
    {
        this.javaccHome = javaccHome;
    }

    public void setMulti( boolean multi )
    {
        optionalAttrs.put( MULTI, new Boolean( multi ) );
    }

    public void setNodedefaultvoid( boolean nodeDefaultVoid )
    {
        optionalAttrs.put( NODE_DEFAULT_VOID, new Boolean( nodeDefaultVoid ) );
    }

    public void setNodefactory( boolean nodeFactory )
    {
        optionalAttrs.put( NODE_FACTORY, new Boolean( nodeFactory ) );
    }

    public void setNodepackage( String nodePackage )
    {
        optionalAttrs.put( NODE_PACKAGE, new String( nodePackage ) );
    }

    public void setNodeprefix( String nodePrefix )
    {
        optionalAttrs.put( NODE_PREFIX, new String( nodePrefix ) );
    }

    public void setNodescopehook( boolean nodeScopeHook )
    {
        optionalAttrs.put( NODE_SCOPE_HOOK, new Boolean( nodeScopeHook ) );
    }

    public void setNodeusesparser( boolean nodeUsesParser )
    {
        optionalAttrs.put( NODE_USES_PARSER, new Boolean( nodeUsesParser ) );
    }

    public void setOutputdirectory( File outputDirectory )
    {
        this.outputDirectory = outputDirectory;
    }

    public void setStatic( boolean staticParser )
    {
        optionalAttrs.put( STATIC, new Boolean( staticParser ) );
    }

    public void setTarget( File target )
    {
        this.target = target;
    }

    public void setVisitor( boolean visitor )
    {
        optionalAttrs.put( VISITOR, new Boolean( visitor ) );
    }

    public void setVisitorException( String visitorException )
    {
        optionalAttrs.put( VISITOR_EXCEPTION, new String( visitorException ) );
    }

    public void execute()
        throws TaskException
    {
        final ExecuteJava exe = new ExecuteJava();
        exe.setClassName( "COM.sun.labs.jjtree.Main" );

        // load command line with optional attributes
        Enumeration iter = optionalAttrs.keys();
        while( iter.hasMoreElements() )
        {
            String name = (String)iter.nextElement();
            Object value = optionalAttrs.get( name );
            exe.getArguments().addArgument( "-" + name + ":" + value.toString() );
        }

        if( target == null || !target.isFile() )
        {
            throw new TaskException( "Invalid target: " + target );
        }

        // use the directory containing the target as the output directory
        if( outputDirectory == null )
        {
            outputDirectory = target.getParentFile();
        }
        if( !outputDirectory.isDirectory() )
        {
            throw new TaskException( "'outputdirectory' " + outputDirectory + " is not a directory." );
        }

        // convert backslashes to slashes, otherwise jjtree will put this as
        // comments and this seems to confuse javacc
        exe.getArguments().addArgument( "-OUTPUT_DIRECTORY:" + outputDirectory.getAbsolutePath().replace( '\\', '/' ) );

        String targetName = target.getName();
        final File javaFile = new File( outputDirectory,
                                        targetName.substring( 0, targetName.indexOf( ".jjt" ) ) + ".jj" );
        if( javaFile.exists() && target.lastModified() < javaFile.lastModified() )
        {
            getContext().verbose( "Target is already built - skipping (" + target + ")" );
            return;
        }
        exe.getArguments().addArgument( target );

        if( javaccHome == null || !javaccHome.isDirectory() )
        {
            throw new TaskException( "Javacchome not set." );
        }
        final Path classpath = exe.getClassPath();
        classpath.addLocation( new File( javaccHome, "JavaCC.zip" ) );
        PathUtil.addJavaRuntime( classpath );

        exe.setMaxMemory( "140M" );
        exe.getSysProperties().addVariable( "install.root", javaccHome.getAbsolutePath() );

        exe.executeForked( getContext() );
    }
}
