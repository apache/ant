/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.dotnet;

import java.io.File;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.Execute;
import org.apache.tools.todo.taskdefs.MatchingTask;
import org.apache.tools.todo.types.Commandline;
import org.apache.tools.todo.types.DirectoryScanner;
import org.apache.tools.todo.types.Path;

/**
 * This task compiles CSharp source into executables or modules. The task will
 * only work on win2K until other platforms support csc.exe or an equivalent.
 * CSC.exe must be on the execute path too. <p>
 *
 * All parameters are optional: &lt;csc/&gt; should suffice to produce a debug
 * build of all *.cs files. References to external files do require explicit
 * enumeration, so are one of the first attributes to consider adding. <p>
 *
 * The task is a directory based task, so attributes like <b>includes="*.cs"</b>
 * and <b>excludes="broken.cs"</b> can be used to control the files pulled in.
 * By default, all *.cs files from the project folder down are included in the
 * command. When this happens the output file -if not specified- is taken as the
 * first file in the list, which may be somewhat hard to control. Specifying the
 * output file with <b>'outfile'</b> seems prudent. <p>
 *
 * <p>
 *
 * TODO
 * <ol>
 *   <li> is incremental build still broken in beta-1?
 *   <li> is Win32Icon broken?
 *   <li> all the missing options
 * </ol>
 * <p>
 *
 * History
 * <Table>
 *
 *   <tr>
 *
 *     <td>
 *       0.3
 *     </td>
 *
 *     <td>
 *       Beta 1 edition
 *     </td>
 *
 *     <td>
 *       To avoid having to remember which assemblies to include, the task
 *       automatically refers to the main dotnet libraries in Beta1.
 *     </tr>
 *
 *     <tr>
 *
 *       <td>
 *         0.2
 *       </td>
 *
 *       <td>
 *         Slightly different
 *       </td>
 *
 *       <td>
 *         Split command execution to a separate class;
 *       </tr>
 *
 *       <tr>
 *
 *         <td>
 *           0.1
 *         </td>
 *
 *         <td>
 *           "I can't believe it's so rudimentary"
 *         </td>
 *
 *         <td>
 *           First pass; minimal builds only support;
 *         </tr>
 *
 *       </table>
 *
 *
 * @author Steve Loughran steve_l@iseran.com
 * @version 0.3
 */
public class CSharp
    extends MatchingTask
{
    /**
     * name of the executable. the .exe suffix is deliberately not included in
     * anticipation of the unix version
     */
    private final static String EXE_NAME = "csc";

    /**
     * what is the file extension we search on?
     */
    private final static String FILE_EXT = "cs";

    /**
     * derive the search pattern from the extension
     */
    private final static String FILE_PATTERN = "**/*." + FILE_EXT;

    /**
     * Fix C# reference inclusion. C# is really dumb in how it handles
     * inclusion. You have to list every 'assembly' -read DLL that is imported.
     * So already you are making a platform assumption -shared libraries have a
     * .dll;"+ extension and the poor developer has to know every library which
     * is included why the compiler cant find classes on the path or in a
     * directory, is a mystery. To reduce the need to be explicit, here is a
     * long list of the core libraries used in Beta-1 of .NET ommitting the
     * blatantly non portable (MS.win32.interop) and the .designer libraries.
     * (ripping out Com was tempting) Casing is chosen to match that of the file
     * system <i>exactly</i> so may work on a unix box too.
     */

    private final static String DEFAULT_REFERENCE_LIST =
        "Accessibility.dll;" +
        "cscompmgd.dll;" +
        "CustomMarshalers.dll;" +
        "IEExecRemote.dll;" +
        "IEHost.dll;" +
        "IIEHost.dll;" +
        "ISymWrapper.dll;" +
        "Microsoft.JScript.dll;" +
        "Microsoft.VisualBasic.dll;" +
        "Microsoft.VisualC.dll;" +
        "Microsoft.Vsa.dll;" +
        "Mscorcfg.dll;" +
        "RegCode.dll;" +
        "System.Configuration.Install.dll;" +
        "System.Data.dll;" +
        "System.Design.dll;" +
        "System.DirectoryServices.dll;" +
        "System.EnterpriseServices.dll;" +
        "System.dll;" +
        "System.Drawing.Design.dll;" +
        "System.Drawing.dll;" +
        "System.Management.dll;" +
        "System.Messaging.dll;" +
        "System.Runtime.Remoting.dll;" +
        "System.Runtime.Serialization.Formatters.Soap.dll;" +
        "System.Security.dll;" +
        "System.ServiceProcess.dll;" +
        "System.Web.dll;" +
        "System.Web.RegularExpressions.dll;" +
        "System.Web.Services.dll;" +
        "System.Windows.Forms.dll;" +
        "System.XML.dll;";

    /**
     * utf out flag
     */
    private boolean m_utf8output;

    private boolean m_fullpaths = true;

    /**
     * debug flag. Controls generation of debug information.
     */
    private boolean m_debug = true;

    /**
     * output XML documentation flag
     */
    private File m_docFile;

    /**
     * any extra command options?
     */
    private String m_extraOptions;

    /**
     * flag to enable automatic reference inclusion
     */
    private boolean m_includeDefaultReferences = true;

    /**
     * incremental build flag
     */
    private boolean m_incremental;

    /**
     * main class (or null for automatic choice)
     */
    private String m_mainClass;

    /**
     * optimise flag
     */
    private boolean m_optimize;

    /**
     * output file. If not supplied this is derived from the source file
     */
    private File m_outputFile;

    /**
     * using the path approach didnt work as it could not handle the implicit
     * execution path. Perhaps that could be extracted from the runtime and then
     * the path approach would be viable
     */
    private Path m_referenceFiles;

    /**
     * list of reference classes. (pretty much a classpath equivalent)
     */
    private String m_references;

    /**
     * type of target. Should be one of exe|library|module|winexe|(null) default
     * is exe; the actual value (if not null) is fed to the command line. <br>
     * See /target
     */
    private String m_targetType;

    /**
     * enable unsafe code flag. Clearly set to false by default
     */
    private boolean m_unsafe;

    /**
     * icon for incorporation into apps
     */
    private File m_win32icon;
    /**
     * icon for incorporation into apps
     */
    private File m_win32res;

    /**
     * list of extra modules to refer to
     */
    private String m_additionalModules;

    /**
     * defines list something like 'RELEASE;WIN32;NO_SANITY_CHECKS;;SOMETHING_ELSE'
     */
    private String m_definitions;

    /**
     * destination directory (null means use the source directory) NB: this is
     * currently not used
     */
    private File m_destDir;

    /**
     * source directory upon which the search pattern is applied
     */
    private File m_srcDir;

    /**
     * warning level: 0-4, with 4 being most verbose
     */
    private int m_warnLevel = 3;

    /**
     * constructor inits everything and set up the search pattern
     */

    public CSharp()
        throws TaskException
    {
        setIncludes( FILE_PATTERN );
    }

    /**
     * Set the definitions
     */
    public void setAdditionalModules( final String additionalModules )
    {
        m_additionalModules = additionalModules;
    }

    /**
     * set the debug flag on or off
     *
     * @param debug on/off flag
     */
    public void setDebug( final boolean debug )
    {
        m_debug = debug;
    }

    /**
     * Set the definitions
     */
    public void setDefinitions( final String definitions )
    {
        m_definitions = definitions;
    }

    /**
     * Set the destination dir to find the files to be compiled
     *
     * @param destDir The new DestDir value
     */
    public void setDestDir( final File destDir )
    {
        m_destDir = destDir;
    }

    /**
     * file for generated XML documentation
     *
     * @param docFile output file
     */
    public void setDocFile( final File docFile )
    {
        m_docFile = docFile;
    }

    /**
     * Sets the ExtraOptions attribute
     */
    public void setExtraOptions( final String extraOptions )
    {
        m_extraOptions = extraOptions;
    }

    public void setFullPaths( final boolean fullpaths )
    {
        m_fullpaths = fullpaths;
    }

    /**
     * set the automatic reference inclusion flag on or off this flag controls
     * the string of references and the /nostdlib option in CSC
     *
     * @param includeDefaultReferences on/off flag
     */
    public void setIncludeDefaultReferences( final boolean includeDefaultReferences )
    {
        m_includeDefaultReferences = includeDefaultReferences;
    }

    /**
     * set the incremental compilation flag on or off
     *
     * @param incremental on/off flag
     */
    public void setIncremental( final boolean incremental )
    {
        m_incremental = incremental;
    }

    /**
     * Sets the MainClass attribute
     *
     * @param mainClass The new MainClass value
     */
    public void setMainClass( final String mainClass )
    {
        m_mainClass = mainClass;
    }

    /**
     * set the optimise flag on or off
     *
     * @param optimize on/off flag
     */
    public void setOptimize( final boolean optimize )
    {
        m_optimize = optimize;
    }

    /**
     * Set the definitions
     */
    public void setOutputFile( final File outputFile )
    {
        m_outputFile = outputFile;
    }

    /**
     * add another path to the reference file path list
     *
     * @param path another path to append
     */
    public void setReferenceFiles( final Path path )
        throws TaskException
    {
        //demand create pathlist
        if( null == m_referenceFiles )
        {
            m_referenceFiles = new Path();
        }
        m_referenceFiles.addPath( path );
    }

    /**
     * Set the reference list to be used for this compilation.
     *
     * @param references The new References value
     */
    public void setReferences( final String references )
    {
        m_references = references;
    }

    /**
     * Set the source dir to find the files to be compiled
     *
     * @param srcDir The new SrcDir value
     */
    public void setSrcDir( final File srcDir )
    {
        m_srcDir = srcDir;
    }

    /**
     * define the target
     *
     * @param targetType The new TargetType value
     * @exception TaskException if target is not one of
     *      exe|library|module|winexe
     */
    public void setTargetType( final String targetType )
        throws TaskException
    {
        final String type = targetType.toLowerCase();
        if( type.equals( "exe" ) || type.equals( "library" ) ||
            type.equals( "module" ) || type.equals( "winexe" ) )
        {
            m_targetType = type;
        }
        else
        {
            final String message = "targetType " + type + " is not a valid type";
            throw new TaskException( message );
        }
    }

    /**
     * Sets the Unsafe attribute
     *
     * @param unsafe The new Unsafe value
     */
    public void setUnsafe( final boolean unsafe )
    {
        m_unsafe = unsafe;
    }

    /**
     * enable generation of utf8 output from the compiler.
     *
     * @param enabled The new Utf8Output value
     */
    public void setUtf8Output( final boolean enabled )
    {
        m_utf8output = enabled;
    }

    /**
     * set warn level (no range checking)
     *
     * @param warnLevel warn level -see .net docs for valid range (probably 0-4)
     */
    public void setWarnLevel( final int warnLevel )
    {
        m_warnLevel = warnLevel;
    }

    /**
     * Set the win32 icon
     *
     * @param fileName path to the file. Can be relative, absolute, whatever.
     */
    public void setWin32Icon( final File fileName )
    {
        m_win32icon = fileName;
    }

    /**
     * Set the win32 icon
     *
     * @param win32res path to the file. Can be relative, absolute, whatever.
     */
    public void setWin32Res( final File win32res )
    {
        m_win32res = win32res;
    }

    /**
     * do the work by building the command line and then calling it
     */
    public void execute()
        throws TaskException
    {
        if( null == m_srcDir )
        {
            m_srcDir = getBaseDirectory();
        }

        final Execute exe = new Execute();
        final Commandline cmd = exe.getCommandline();
        cmd.setExecutable( EXE_NAME );

        addArgument( cmd, "/nologo" );
        addArgument( cmd, getAdditionalModulesParameter() );
        addArgument( cmd, getDefinitionsParameter() );
        addArgument( cmd, getDebugParameter() );
        addArgument( cmd, getDocFileParameter() );
        addArgument( cmd, getIncrementalParameter() );
        addArgument( cmd, getMainClassParameter() );
        addArgument( cmd, getOptimizeParameter() );
        addArgument( cmd, getReferencesParameter() );
        addArgument( cmd, getTargetTypeParameter() );
        addArgument( cmd, getUnsafeParameter() );
        addArgument( cmd, getWarnLevelParameter() );
        addArgument( cmd, getWin32IconParameter() );
        addArgument( cmd, getOutputFileParameter() );
        addArgument( cmd, getIncludeDefaultReferencesParameter() );
        addArgument( cmd, getDefaultReferenceParameter() );
        addArgument( cmd, getWin32ResParameter() );
        addArgument( cmd, getUtf8OutpuParameter() );
        addArgument( cmd, getFullPathsParameter() );
        addArgument( cmd, getExtraOptionsParameter() );

        //get dependencies list.
        final DirectoryScanner scanner = super.getDirectoryScanner( m_srcDir );
        final String[] dependencies = scanner.getIncludedFiles();
        final String message = "compiling " + dependencies.length + " file" +
            ( ( dependencies.length == 1 ) ? "" : "s" );
        getContext().info( message );
        final String baseDir = scanner.getBasedir().toString();
        //add to the command
        for( int i = 0; i < dependencies.length; i++ )
        {
            final String targetFile = baseDir + File.separator + dependencies[ i ];
            addArgument( cmd, targetFile );
        }

        //now run the command of exe + settings + files
        exe.execute( getContext() );
    }

    private void addArgument( final Commandline cmd, final String argument )
    {
        if( null != argument && 0 != argument.length() )
        {
            cmd.addArgument( argument );
        }
    }

    /**
     * get the argument or null for no argument needed
     *
     * @return The AdditionalModules Parameter to CSC
     */
    private String getAdditionalModulesParameter()
    {
        if( notEmpty( m_additionalModules ) )
        {
            return "/addmodule:" + m_additionalModules;
        }
        else
        {
            return null;
        }
    }

    /**
     * get the debug switch argument
     *
     * @return The Debug Parameter to CSC
     */
    private String getDebugParameter()
    {
        return "/debug" + ( m_debug ? "+" : "-" );
    }

    /**
     * get default reference list
     *
     * @return null or a string of references.
     */
    private String getDefaultReferenceParameter()
    {
        if( m_includeDefaultReferences )
        {
            StringBuffer s = new StringBuffer( "/reference:" );
            s.append( DEFAULT_REFERENCE_LIST );
            return new String( s );
        }
        else
        {
            return null;
        }
    }

    /**
     * get the argument or null for no argument needed
     *
     * @return The Definitions Parameter to CSC
     */
    private String getDefinitionsParameter()
    {
        if( notEmpty( m_definitions ) )
        {
            return "/define:" + m_definitions;
        }
        else
        {
            return null;
        }
    }

    /**
     * get the argument or null for no argument needed
     *
     * @return The DocFile Parameter to CSC
     */
    private String getDocFileParameter()
    {
        if( m_docFile != null )
        {
            return "/doc:" + m_docFile.toString();
        }
        else
        {
            return null;
        }
    }

    /**
     * get any extra options or null for no argument needed
     *
     * @return The ExtraOptions Parameter to CSC
     */
    private String getExtraOptionsParameter()
    {
        if( m_extraOptions != null && m_extraOptions.length() != 0 )
        {
            return m_extraOptions;
        }
        else
        {
            return null;
        }
    }

    private String getFullPathsParameter()
    {
        return m_fullpaths ? "/fullpaths" : null;
    }

    /**
     * get the include default references flag or null for no argument needed
     *
     * @return The Parameter to CSC
     */
    private String getIncludeDefaultReferencesParameter()
    {
        return "/nostdlib" + ( m_includeDefaultReferences ? "-" : "+" );
    }

    /**
     * get the incremental build argument
     *
     * @return The Incremental Parameter to CSC
     */
    private String getIncrementalParameter()
    {
        return "/incremental" + ( m_incremental ? "+" : "-" );
    }

    /**
     * get the /main argument or null for no argument needed
     *
     * @return The MainClass Parameter to CSC
     */
    private String getMainClassParameter()
    {
        if( m_mainClass != null && m_mainClass.length() != 0 )
        {
            return "/main:" + m_mainClass;
        }
        else
        {
            return null;
        }
    }

    /**
     * get the optimise flag or null for no argument needed
     *
     * @return The Optimize Parameter to CSC
     */
    private String getOptimizeParameter()
    {
        return "/optimize" + ( m_optimize ? "+" : "-" );
    }

    /**
     * get the argument or null for no argument needed
     *
     * @return The OutputFile Parameter to CSC
     */
    private String getOutputFileParameter()
    {
        if( m_outputFile != null )
        {
            File f = m_outputFile;
            return "/out:" + f.toString();
        }
        else
        {
            return null;
        }
    }

    /**
     * get the reference string or null for no argument needed
     *
     * @return The References Parameter to CSC
     */
    private String getReferencesParameter()
    {
        //bail on no references
        if( notEmpty( m_references ) )
        {
            return "/reference:" + m_references;
        }
        else
        {
            return null;
        }
    }

    /**
     * get the argument or null for no argument needed
     *
     * @return The TargetType Parameter to CSC
     */
    private String getTargetTypeParameter()
    {
        if( notEmpty( m_targetType ) )
        {
            return "/target:" + m_targetType;
        }
        else
        {
            return null;
        }
    }

    /**
     * get the argument or null for no argument needed
     *
     * @return The Unsafe Parameter to CSC
     */
    private String getUnsafeParameter()
    {
        return m_unsafe ? "/unsafe" : null;
    }

    private String getUtf8OutpuParameter()
    {
        return m_utf8output ? "/utf8output" : null;
    }

    /**
     * get the warn level switch
     *
     * @return The WarnLevel Parameter to CSC
     */
    private String getWarnLevelParameter()
    {
        return "/warn:" + m_warnLevel;
    }

    /**
     * get the argument or null for no argument needed
     *
     * @return The Win32Icon Parameter to CSC
     */
    private String getWin32IconParameter()
    {
        if( m_win32icon != null )
        {
            return "/win32icon:" + m_win32icon.toString();
        }
        else
        {
            return null;
        }
    }

    /**
     * get the argument or null for no argument needed
     *
     * @return The Win32Icon Parameter to CSC
     */
    private String getWin32ResParameter()
    {
        if( m_win32res != null )
        {
            return "/win32res:" + m_win32res.toString();
        }
        else
        {
            return null;
        }
    }

    /**
     * test for a string containing something useful
     *
     * @param string string in
     * @return true if the argument is not null or empty
     */
    private boolean notEmpty( final String string )
    {
        return string != null && string.length() != 0;
    }
}
