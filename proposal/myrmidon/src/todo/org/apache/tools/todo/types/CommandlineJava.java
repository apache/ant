/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.types;

import java.io.File;
import org.apache.aut.nativelib.Os;
import org.apache.avalon.excalibur.util.StringUtil;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.todo.types.Argument;
import org.apache.tools.todo.types.Commandline;

/**
 * A representation of a Java command line that is nothing more than a composite
 * of 2 <tt>Commandline</tt> . 1 for the vm/options and 1 for the
 * classname/arguments. It provides specific methods for a java command line.
 *
 * @author thomas.haas@softwired-inc.com
 * @author <a href="sbailliez@apache.org">Stephane Bailliez</a>
 */
public class CommandlineJava
    implements Cloneable
{
    private Commandline m_vmCommand = new Commandline();
    private Commandline m_javaCommand = new Commandline();
    private SysProperties m_sysProperties = new SysProperties();
    private Path m_classpath;
    private String m_maxMemory;

    /**
     * Indicate whether it will execute a jar file or not, in this case the
     * first vm option must be a -jar and the 'executable' is a jar file.
     */
    private boolean executeJar;

    public CommandlineJava()
    {
        setVm( getJavaExecutableName() );
    }

    /**
     * set the classname to execute
     *
     * @param classname the fully qualified classname.
     */
    public void setClassname( String classname )
    {
        m_javaCommand.setExecutable( classname );
        executeJar = false;
    }

    /**
     * set a jar file to execute via the -jar option.
     *
     * @param jarpathname The new Jar value
     */
    public void setJar( String jarpathname )
    {
        m_javaCommand.setExecutable( jarpathname );
        executeJar = true;
    }

    /**
     * -mx or -Xmx depending on VM version
     *
     * @param max The new Maxmemory value
     */
    public void setMaxmemory( String max )
    {
        this.m_maxMemory = max;
    }

    public void setSystemProperties()
        throws TaskException
    {
        m_sysProperties.setSystem();
    }

    public void setVm( String vm )
    {
        m_vmCommand.setExecutable( vm );
    }

    /**
     * @return the name of the class to run or <tt>null</tt> if there is no
     *      class.
     * @see #getJar()
     */
    public String getClassname()
    {
        if( !executeJar )
        {
            return m_javaCommand.getExecutable();
        }
        return null;
    }

    public Path getClasspath()
    {
        return m_classpath;
    }

    /**
     * get the command line to run a java vm.
     *
     * @return the list of all arguments necessary to run the vm.
     */
    public String[] getCommandline()
        throws TaskException
    {
        String[] result = new String[ size() ];
        int pos = 0;
        String[] vmArgs = getActualVMCommand().getCommandline();
        // first argument is the java.exe path...
        result[ pos++ ] = vmArgs[ 0 ];

        // -jar must be the first option in the command line.
        if( executeJar )
        {
            result[ pos++ ] = "-jar";
        }
        // next follows the vm options
        System.arraycopy( vmArgs, 1, result, pos, vmArgs.length - 1 );
        pos += vmArgs.length - 1;
        // properties are part of the vm options...
        if( m_sysProperties.size() > 0 )
        {
            System.arraycopy( m_sysProperties.getJavaVariables(), 0,
                              result, pos, m_sysProperties.size() );
            pos += m_sysProperties.size();
        }
        // classpath is a vm option too..
        if( m_classpath != null && ! m_classpath.isEmpty() )
        {
            result[ pos++ ] = "-classpath";
            result[ pos++ ] = PathUtil.formatPath( m_classpath );
        }
        // this is the classname to run as well as its arguments.
        // in case of 'executeJar', the executable is a jar file.
        System.arraycopy( m_javaCommand.getCommandline(), 0,
                          result, pos, m_javaCommand.size() );
        return result;
    }

    /**
     * @return the pathname of the jar file to run via -jar option or <tt>null
     *      </tt> if there is no jar to run.
     * @see #getClassname()
     */
    public String getJar()
    {
        if( executeJar )
        {
            return m_javaCommand.getExecutable();
        }
        return null;
    }

    public Commandline getJavaCommand()
    {
        return m_javaCommand;
    }

    public SysProperties getSystemProperties()
    {
        return m_sysProperties;
    }

    public Commandline getVmCommand()
    {
        return getActualVMCommand();
    }

    public void addSysproperty( EnvironmentVariable sysp )
    {
        m_sysProperties.addVariable( sysp );
    }

    public void addArgument( final String argument )
    {
        m_javaCommand.addArgument( argument );
    }

    public void addArgument( final Argument argument )
    {
        m_javaCommand.addArgument( argument );
    }

    public Path createClasspath()
    {
        if( m_classpath == null )
        {
            m_classpath = new Path();
        }
        return m_classpath;
    }

    public void addVmArgument( final String argument )
    {
        m_vmCommand.addArgument( argument );
    }

    public void addVmArgument( final Argument argument )
    {
        m_vmCommand.addArgument( argument );
    }

    public void restoreSystemProperties()
        throws TaskException
    {
        m_sysProperties.restoreSystem();
    }

    /**
     * The size of the java command line.
     *
     * @return the total number of arguments in the java command line.
     * @see #getCommandline()
     */
    public int size()
        throws TaskException
    {
        int size = getActualVMCommand().size() + m_javaCommand.size() + m_sysProperties.size();
        // classpath is "-classpath <classpath>" -> 2 args
        if( m_classpath != null && ! m_classpath.isEmpty() )
        {
            size += 2;
        }
        // jar execution requires an additional -jar option
        if( executeJar )
        {
            size++;
        }
        return size;
    }

    public String toString()
    {
        try
        {
            final String[] line = getCommandline();
            return StringUtil.join( line, " " );
        }
        catch( TaskException e )
        {
            return e.toString();
        }
    }

    private Commandline getActualVMCommand()
    {
        Commandline actualVMCommand = new Commandline();
        //(Commandline)vmCommand.clone();
        if( m_maxMemory != null )
        {
            actualVMCommand.addArgument( "-Xmx" + m_maxMemory );
        }
        return actualVMCommand;
    }

    private String getJavaExecutableName()
    {
        // This is the most common extension case - exe for windows and OS/2,
        // nothing for *nix.
        String extension = Os.isFamily( Os.OS_FAMILY_DOS ) ? ".exe" : "";

        // Look for java in the java.home/../bin directory.  Unfortunately
        // on Windows java.home doesn't always refer to the correct location,
        // so we need to fall back to assuming java is somewhere on the
        // PATH.
        File jExecutable =
            new File( System.getProperty( "java.home" ) +
                      "/../bin/java" + extension );

        if( jExecutable.exists() && !Os.isFamily( Os.OS_FAMILY_NETWARE ) )
        {
            // NetWare may have a "java" in that directory, but 99% of
            // the time, you don't want to execute it -- Jeff Tulley
            // <JTULLEY@novell.com>
            return jExecutable.getAbsolutePath();
        }
        else
        {
            return "java";
        }
    }
}
