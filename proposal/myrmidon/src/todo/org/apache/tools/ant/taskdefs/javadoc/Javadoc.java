/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.javadoc;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.Os;
import org.apache.myrmidon.framework.exec.ExecOutputHandler;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.exec.Execute2;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;

/**
 * This task makes it easy to generate Javadoc documentation for a collection of
 * source code. <P>
 *
 * Current known limitations are: <P>
 *
 *
 * <UL>
 *   <LI> patterns must be of the form "xxx.*", every other pattern doesn't
 *   work.
 *   <LI> the java comment-stripper reader is horribly slow
 *   <LI> there is no control on arguments sanity since they are left to the
 *   javadoc implementation.
 *   <LI> argument J in javadoc1 is not supported (what is that for anyway?)
 *
 * </UL>
 * <P>
 *
 * If no <CODE>doclet</CODE> is set, then the <CODE>version</CODE> and <CODE>author</CODE>
 * are by default <CODE>"yes"</CODE>. <P>
 *
 * Note: This task is run on another VM because the Javadoc code calls <CODE>System.exit()</CODE>
 * which would break Ant functionality.
 *
 * @author Jon S. Stevens <a href="mailto:jon@clearink.com">jon@clearink.com</a>
 * @author Stefano Mazzocchi <a href="mailto:stefano@apache.org">
 *      stefano@apache.org</a>
 * @author Patrick Chanezon <a href="mailto:chanezon@netscape.com">
 *      chanezon@netscape.com</a>
 * @author Ernst de Haan <a href="mailto:ernst@jollem.com">ernst@jollem.com</a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */

public class Javadoc
    extends Task
    implements ExecOutputHandler
{
    private Commandline m_command = new Commandline();

    private Path m_sourcePath;
    private File m_destDir;
    private ArrayList m_sourceFiles = new ArrayList();
    private ArrayList m_packageNames = new ArrayList( 5 );
    private ArrayList m_excludePackageNames = new ArrayList( 1 );
    private boolean m_author = true;
    private boolean m_version = true;
    private DocletInfo m_doclet;
    private Path m_classpath;
    private Path m_bootclasspath;
    private String m_group;
    private ArrayList m_compileList = new ArrayList( 10 );
    private String m_packageList;
    private ArrayList m_links = new ArrayList( 2 );
    private ArrayList m_groups = new ArrayList( 2 );
    private boolean m_useDefaultExcludes = true;
    private Html m_doctitle;
    private Html m_header;
    private Html m_footer;
    private Html m_bottom;
    private boolean m_useExternalFile;
    private File m_tmpList;

    public void setAccess( AccessType at )
    {
        m_command.createArgument().setValue( "-" + at.getValue() );
    }

    public void setAdditionalparam( String add )
        throws TaskException
    {
        m_command.createArgument().setLine( add );
    }

    public void setAuthor( boolean src )
    {
        m_author = src;
    }

    public void setBootclasspath( Path src )
        throws TaskException
    {
        if( m_bootclasspath == null )
        {
            m_bootclasspath = src;
        }
        else
        {
            m_bootclasspath.append( src );
        }
    }

    public void setBottom( String src )
    {
        Html h = new Html();
        h.addContent( src );
        addBottom( h );
    }

    public void setCharset( String src )
    {
        this.add12ArgIfNotEmpty( "-charset", src );
    }

    public void setClasspath( Path src )
        throws TaskException
    {
        if( m_classpath == null )
        {
            m_classpath = src;
        }
        else
        {
            m_classpath.append( src );
        }
    }

    /**
     * Sets whether default exclusions should be used or not.
     *
     * @param useDefaultExcludes "true"|"on"|"yes" when default exclusions
     *      should be used, "false"|"off"|"no" when they shouldn't be used.
     */
    public void setDefaultexcludes( boolean useDefaultExcludes )
    {
        this.m_useDefaultExcludes = useDefaultExcludes;
    }

    public void setDestdir( File dir )
    {
        m_destDir = dir;
        m_command.createArgument().setValue( "-d" );
        m_command.createArgument().setFile( m_destDir );
    }

    public void setDocencoding( String enc )
    {
        m_command.createArgument().setValue( "-docencoding" );
        m_command.createArgument().setValue( enc );
    }

    public void setDoclet( String src )
    {
        if( m_doclet == null )
        {
            m_doclet = new DocletInfo();
        }
        m_doclet.setName( src );
    }

    public void setDocletPath( Path src )
        throws TaskException
    {
        if( m_doclet == null )
        {
            m_doclet = new DocletInfo();
        }
        m_doclet.setPath( src );
    }

    public void setDoctitle( String src )
    {
        Html h = new Html();
        h.addContent( src );
        addDoctitle( h );
    }

    public void setEncoding( String enc )
    {
        m_command.createArgument().setValue( "-encoding" );
        m_command.createArgument().setValue( enc );
    }

    public void setExcludePackageNames( String src )
    {
        StringTokenizer tok = new StringTokenizer( src, "," );
        while( tok.hasMoreTokens() )
        {
            String p = tok.nextToken();
            PackageName pn = new PackageName();
            pn.setName( p );
            addExcludePackage( pn );
        }
    }

    public void setExtdirs( String src )
    {
        m_command.createArgument().setValue( "-extdirs" );
        m_command.createArgument().setValue( src );
    }

    public void setFooter( String src )
    {
        Html h = new Html();
        h.addContent( src );
        addFooter( h );
    }

    public void setGroup( String src )
    {
        m_group = src;
    }

    public void setHeader( String src )
    {
        Html h = new Html();
        h.addContent( src );
        addHeader( h );
    }

    public void setHelpfile( File f )
    {
        m_command.createArgument().setValue( "-helpfile" );
        m_command.createArgument().setFile( f );
    }

    public void setLink( String src )
    {
        createLink().setHref( src );
    }

    public void setLinkoffline( String src )
        throws TaskException
    {
        LinkArgument le = createLink();
        le.setOffline( true );
        String linkOfflineError = "The linkoffline attribute must include a URL and " +
            "a package-list file location separated by a space";
        if( src.trim().length() == 0 )
        {
            throw new TaskException( linkOfflineError );
        }
        StringTokenizer tok = new StringTokenizer( src, " ", false );
        le.setHref( tok.nextToken() );

        if( !tok.hasMoreTokens() )
        {
            throw new TaskException( linkOfflineError );
        }
        le.setPackagelistLoc( resolveFile( tok.nextToken() ) );
    }

    public void setLocale( String src )
    {
        m_command.createArgument().setValue( "-locale" );
        m_command.createArgument().setValue( src );
    }

    public void setMaxmemory( final String max )
    {
        m_command.createArgument().setValue( "-J-Xmx" + max );
    }

    public void setNodeprecated( boolean b )
    {
        addArgIf( b, "-nodeprecated" );
    }

    public void setNodeprecatedlist( boolean b )
    {
        addArgIf( b, "-nodeprecatedlist" );
    }

    public void setNohelp( boolean b )
    {
        addArgIf( b, "-nohelp" );
    }

    public void setNoindex( boolean b )
    {
        addArgIf( b, "-noindex" );
    }

    public void setNonavbar( boolean b )
    {
        addArgIf( b, "-nonavbar" );
    }

    public void setNotree( boolean b )
    {
        addArgIf( b, "-notree" );
    }

    public void setOld( boolean b )
    {
        addArgIf( b, "-1.1" );
    }

    public void setOverview( File f )
    {
        m_command.createArgument().setValue( "-overview" );
        m_command.createArgument().setFile( f );
    }

    public void setPackage( boolean b )
    {
        addArgIf( b, "-package" );
    }

    public void setPackageList( String src )
    {
        m_packageList = src;
    }

    public void setPackagenames( String src )
    {
        StringTokenizer tok = new StringTokenizer( src, "," );
        while( tok.hasMoreTokens() )
        {
            String p = tok.nextToken();
            PackageName pn = new PackageName();
            pn.setName( p );
            addPackage( pn );
        }
    }

    public void setPrivate( boolean b )
    {
        addArgIf( b, "-private" );
    }

    public void setProtected( boolean b )
    {
        addArgIf( b, "-protected" );
    }

    public void setPublic( boolean b )
    {
        addArgIf( b, "-public" );
    }

    public void setSerialwarn( boolean b )
    {
        addArgIf( b, "-serialwarn" );
    }

    public void setSourcefiles( String src )
        throws TaskException
    {
        StringTokenizer tok = new StringTokenizer( src, "," );
        while( tok.hasMoreTokens() )
        {
            String f = tok.nextToken();
            SourceFile sf = new SourceFile();
            sf.setFile( resolveFile( f ) );
            addSource( sf );
        }
    }

    public void setSourcepath( Path src )
        throws TaskException
    {
        if( m_sourcePath == null )
        {
            m_sourcePath = src;
        }
        else
        {
            m_sourcePath.append( src );
        }
    }

    public void setSplitindex( boolean b )
    {
        addArgIf( b, "-splitindex" );
    }

    public void setStylesheetfile( File f )
    {
        m_command.createArgument().setValue( "-stylesheetfile" );
        m_command.createArgument().setFile( f );
    }

    public void setUse( boolean b )
    {
        addArgIf( b, "-use" );
    }

    /**
     * Work around command line length limit by using an external file for the
     * sourcefiles.
     *
     * @param b The new UseExternalFile value
     */
    public void setUseExternalFile( boolean b )
    {
        m_useExternalFile = b;
    }

    public void setVerbose( boolean b )
    {
        addArgIf( b, "-verbose" );
    }

    public void setVersion( boolean src )
    {
        m_version = src;
    }

    public void setWindowtitle( String src )
    {
        add12ArgIfNotEmpty( "-windowtitle", src );
    }

    public void addBottom( Html text )
    {
        m_bottom = text;
    }

    public void addDoctitle( Html text )
    {
        m_doctitle = text;
    }

    public void addExcludePackage( PackageName pn )
    {
        m_excludePackageNames.add( pn );
    }

    public void addFooter( Html text )
    {
        m_footer = text;
    }

    public void addHeader( Html text )
    {
        m_header = text;
    }

    public void addPackage( PackageName pn )
    {
        m_packageNames.add( pn );
    }

    public void addSource( SourceFile sf )
        throws TaskException
    {
        m_sourceFiles.add( sf );
    }

    public Path createBootclasspath()
        throws TaskException
    {
        if( m_bootclasspath == null )
        {
            m_bootclasspath = new Path();
        }
        return m_bootclasspath.createPath();
    }

    public Path createClasspath()
        throws TaskException
    {
        if( m_classpath == null )
        {
            m_classpath = new Path();
        }
        return m_classpath.createPath();
    }

    public DocletInfo createDoclet()
    {
        m_doclet = new DocletInfo();
        return m_doclet;
    }

    public GroupArgument createGroup()
    {
        GroupArgument ga = new GroupArgument();
        m_groups.add( ga );
        return ga;
    }

    public LinkArgument createLink()
    {
        LinkArgument la = new LinkArgument();
        m_links.add( la );
        return la;
    }

    public Path createSourcepath()
        throws TaskException
    {
        if( m_sourcePath == null )
        {
            m_sourcePath = new Path();
        }
        return m_sourcePath.createPath();
    }

    public void execute()
        throws TaskException
    {
        if( m_sourcePath == null )
        {
            String msg = "sourcePath attribute must be set!";
            throw new TaskException( msg );
        }

        getLogger().info( "Generating Javadoc" );

        if( m_doctitle != null )
        {
            m_command.createArgument().setValue( "-doctitle" );
            m_command.createArgument().setValue( m_doctitle.getText() );
        }
        if( m_header != null )
        {
            m_command.createArgument().setValue( "-header" );
            m_command.createArgument().setValue( m_header.getText() );
        }
        if( m_footer != null )
        {
            m_command.createArgument().setValue( "-footer" );
            m_command.createArgument().setValue( m_footer.getText() );
        }
        if( m_bottom != null )
        {
            m_command.createArgument().setValue( "-bottom" );
            m_command.createArgument().setValue( m_bottom.getText() );
        }

        Commandline cmd = (Commandline)m_command.clone();
        cmd.setExecutable( getJavadocExecutableName() );

        // ------------------------------------------------ general javadoc arguments
        if( m_classpath == null )
            m_classpath = Path.systemClasspath;
        else
            m_classpath = m_classpath.concatSystemClasspath( "ignore" );

        cmd.createArgument().setValue( "-classpath" );
        cmd.createArgument().setValue( m_sourcePath.toString() +
                                       System.getProperty( "path.separator" ) + m_classpath.toString() );

        if( m_version && m_doclet == null )
            cmd.createArgument().setValue( "-version" );
        if( m_author && m_doclet == null )
            cmd.createArgument().setValue( "-author" );

        if( m_doclet == null )
        {
            if( m_destDir == null )
            {
                String msg = "destDir attribute must be set!";
                throw new TaskException( msg );
            }
        }

        // --------------------------------- javadoc2 arguments for default doclet

        // XXX: how do we handle a custom doclet?

        if( m_doclet != null )
        {
            if( m_doclet.getName() == null )
            {
                throw new TaskException( "The doclet name must be specified." );
            }
            else
            {
                cmd.createArgument().setValue( "-doclet" );
                cmd.createArgument().setValue( m_doclet.getName() );
                if( m_doclet.getPath() != null )
                {
                    cmd.createArgument().setValue( "-docletpath" );
                    cmd.createArgument().setPath( m_doclet.getPath() );
                }
                for( Iterator e = m_doclet.getParams(); e.hasNext(); )
                {
                    DocletParam param = (DocletParam)e.next();
                    if( param.getName() == null )
                    {
                        throw new TaskException( "Doclet parameters must have a name" );
                    }

                    cmd.createArgument().setValue( param.getName() );
                    if( param.getValue() != null )
                    {
                        cmd.createArgument().setValue( param.getValue() );
                    }
                }
            }

            if( m_bootclasspath != null )
            {
                cmd.createArgument().setValue( "-bootclasspath" );
                cmd.createArgument().setPath( m_bootclasspath );
            }

            // add the links arguments
            if( m_links.size() != 0 )
            {
                for( Iterator e = m_links.iterator(); e.hasNext(); )
                {
                    LinkArgument la = (LinkArgument)e.next();

                    if( la.getHref() == null )
                    {
                        throw new TaskException( "Links must provide the URL to the external class documentation." );
                    }

                    if( la.isLinkOffline() )
                    {
                        File packageListLocation = la.getPackagelistLoc();
                        if( packageListLocation == null )
                        {
                            throw new TaskException( "The package list location for link " + la.getHref() +
                                                     " must be provided because the link is offline" );
                        }
                        File packageList = new File( packageListLocation, "package-list" );
                        if( packageList.exists() )
                        {
                            cmd.createArgument().setValue( "-linkoffline" );
                            cmd.createArgument().setValue( la.getHref() );
                            cmd.createArgument().setValue( packageListLocation.getAbsolutePath() );
                        }
                        else
                        {
                            getLogger().debug( "Warning: No package list was found at " + packageListLocation );
                        }
                    }
                    else
                    {
                        cmd.createArgument().setValue( "-link" );
                        cmd.createArgument().setValue( la.getHref() );
                    }
                }
            }

            // add the single group arguments
            // Javadoc 1.2 rules:
            //   Multiple -group args allowed.
            //   Each arg includes 3 strings: -group [name] [packagelist].
            //   Elements in [packagelist] are colon-delimited.
            //   An element in [packagelist] may end with the * wildcard.

            // Ant javadoc task rules for group attribute:
            //   Args are comma-delimited.
            //   Each arg is 2 space-delimited strings.
            //   E.g., group="XSLT_Packages org.apache.xalan.xslt*,XPath_Packages org.apache.xalan.xpath*"
            if( m_group != null )
            {
                StringTokenizer tok = new StringTokenizer( m_group, ",", false );
                while( tok.hasMoreTokens() )
                {
                    String grp = tok.nextToken().trim();
                    int space = grp.indexOf( " " );
                    if( space > 0 )
                    {
                        String name = grp.substring( 0, space );
                        String pkgList = grp.substring( space + 1 );
                        cmd.createArgument().setValue( "-group" );
                        cmd.createArgument().setValue( name );
                        cmd.createArgument().setValue( pkgList );
                    }
                }
            }

            // add the group arguments
            if( m_groups.size() != 0 )
            {
                for( Iterator e = m_groups.iterator(); e.hasNext(); )
                {
                    GroupArgument ga = (GroupArgument)e.next();
                    String title = ga.getTitle();
                    String packages = ga.getPackages();
                    if( title == null || packages == null )
                    {
                        throw new TaskException( "The title and packages must be specified for group elements." );
                    }
                    cmd.createArgument().setValue( "-group" );
                    cmd.createArgument().setValue( title );
                    cmd.createArgument().setValue( packages );
                }
            }

        }

        m_tmpList = null;
        if( m_packageNames.size() > 0 )
        {
            ArrayList packages = new ArrayList();
            Iterator enum = m_packageNames.iterator();
            while( enum.hasNext() )
            {
                PackageName pn = (PackageName)enum.next();
                String name = pn.getName().trim();
                if( name.endsWith( ".*" ) )
                {
                    packages.add( name );
                }
                else
                {
                    cmd.createArgument().setValue( name );
                }
            }

            ArrayList excludePackages = new ArrayList();
            if( m_excludePackageNames.size() > 0 )
            {
                enum = m_excludePackageNames.iterator();
                while( enum.hasNext() )
                {
                    PackageName pn = (PackageName)enum.next();
                    excludePackages.add( pn.getName().trim() );
                }
            }
            if( packages.size() > 0 )
            {
                evaluatePackages( cmd, m_sourcePath, packages, excludePackages );
            }
        }

        if( m_sourceFiles.size() > 0 )
        {
            PrintWriter srcListWriter = null;
            try
            {

                /**
                 * Write sourcefiles to a temporary file if requested.
                 */
                if( m_useExternalFile )
                {
                    if( m_tmpList == null )
                    {
                        m_tmpList = File.createTempFile( "javadoc", "", getBaseDirectory() );
                        cmd.createArgument().setValue( "@" + m_tmpList.getAbsolutePath() );
                    }
                    srcListWriter = new PrintWriter( new FileWriter( m_tmpList.getAbsolutePath(),
                                                                     true ) );
                }

                Iterator enum = m_sourceFiles.iterator();
                while( enum.hasNext() )
                {
                    SourceFile sf = (SourceFile)enum.next();
                    String sourceFileName = sf.getFile().getAbsolutePath();
                    if( m_useExternalFile )
                    {
                        srcListWriter.println( sourceFileName );
                    }
                    else
                    {
                        cmd.createArgument().setValue( sourceFileName );
                    }
                }

            }
            catch( IOException e )
            {
                throw new TaskException( "Error creating temporary file", e );
            }
            finally
            {
                if( srcListWriter != null )
                {
                    srcListWriter.close();
                }
            }
        }

        if( m_packageList != null )
        {
            cmd.createArgument().setValue( "@" + m_packageList );
        }
        getLogger().debug( "Javadoc args: " + cmd );

        getLogger().info( "Javadoc execution" );

        final Execute2 exe = new Execute2();
        setupLogger( exe );
        exe.setExecOutputHandler( this );

        /*
         * No reason to change the working directory as all filenames and
         * path components have been resolved already.
         *
         * Avoid problems with command line length in some environments.
         */
        exe.setWorkingDirectory( null );
        try
        {
            exe.setCommandline( cmd.getCommandline() );
            final int ret = exe.execute();
            if( ret != 0 )
            {
                throw new TaskException( "Javadoc returned " + ret );
            }
        }
        catch( IOException e )
        {
            throw new TaskException( "Javadoc failed: " + e, e );
        }
        finally
        {

            if( m_tmpList != null )
            {
                m_tmpList.delete();
                m_tmpList = null;
            }
        }
    }

    private String getJavadocExecutableName()
    {
        // This is the most common extension case - exe for windows and OS/2,
        // nothing for *nix.
        String extension = Os.isFamily( "dos" ) ? ".exe" : "";

        // Look for javadoc in the java.home/../bin directory.  Unfortunately
        // on Windows java.home doesn't always refer to the correct location,
        // so we need to fall back to assuming javadoc is somewhere on the
        // PATH.
        File jdocExecutable = new File( System.getProperty( "java.home" ) +
                                        "/../bin/javadoc" + extension );

        if( jdocExecutable.exists() && !Os.isFamily( "netware" ) )
        {
            return jdocExecutable.getAbsolutePath();
        }
        else
        {
            if( !Os.isFamily( "netware" ) )
            {
                getLogger().debug( "Unable to locate " + jdocExecutable.getAbsolutePath() +
                                   ". Using \"javadoc\" instead." );
            }
            return "javadoc";
        }
    }

    private void add12ArgIfNotEmpty( String key, String value )
    {
        if( value != null && value.length() != 0 )
        {
            m_command.createArgument().setValue( key );
            m_command.createArgument().setValue( value );
        }
        else
        {
            getLogger().warn( "Warning: Leaving out empty argument '" + key + "'" );
        }
    }

    private void addArgIf( boolean b, String arg )
    {
        if( b )
        {
            m_command.createArgument().setValue( arg );
        }
    }

    /**
     * Given a source path, a list of package patterns, fill the given list with
     * the packages found in that path subdirs matching one of the given
     * patterns.
     *
     * @param toExecute Description of Parameter
     * @param sourcePath Description of Parameter
     * @param packages Description of Parameter
     * @param excludePackages Description of Parameter
     */
    private void evaluatePackages( Commandline toExecute, Path sourcePath,
                                   ArrayList packages, ArrayList excludePackages )
        throws TaskException
    {
        getLogger().debug( "Source path = " + sourcePath.toString() );
        StringBuffer msg = new StringBuffer( "Packages = " );
        for( int i = 0; i < packages.size(); i++ )
        {
            if( i > 0 )
            {
                msg.append( "," );
            }
            msg.append( packages.get( i ) );
        }
        getLogger().debug( msg.toString() );

        msg.setLength( 0 );
        msg.append( "Exclude Packages = " );
        for( int i = 0; i < excludePackages.size(); i++ )
        {
            if( i > 0 )
            {
                msg.append( "," );
            }
            msg.append( excludePackages.get( i ) );
        }
        getLogger().debug( msg.toString() );

        ArrayList addedPackages = new ArrayList();

        String[] list = sourcePath.list();
        if( list == null )
            list = new String[ 0 ];

        FileSet fs = new FileSet();
        fs.setDefaultexcludes( m_useDefaultExcludes );

        Iterator e = packages.iterator();
        while( e.hasNext() )
        {
            String pkg = (String)e.next();
            pkg = pkg.replace( '.', '/' );
            if( pkg.endsWith( "*" ) )
            {
                pkg += "*";
            }

            fs.createInclude().setName( pkg );
        }// while

        e = excludePackages.iterator();
        while( e.hasNext() )
        {
            String pkg = (String)e.next();
            pkg = pkg.replace( '.', '/' );
            if( pkg.endsWith( "*" ) )
            {
                pkg += "*";
            }

            fs.createExclude().setName( pkg );
        }

        PrintWriter packageListWriter = null;
        try
        {
            if( m_useExternalFile )
            {
                m_tmpList = File.createTempFile( "javadoc", "", getBaseDirectory() );
                toExecute.createArgument().setValue( "@" + m_tmpList.getAbsolutePath() );
                packageListWriter = new PrintWriter( new FileWriter( m_tmpList ) );
            }

            for( int j = 0; j < list.length; j++ )
            {
                final File source = resolveFile( list[ j ] );
                fs.setDir( source );

                final DirectoryScanner ds = fs.getDirectoryScanner();
                final String[] packageDirs = ds.getIncludedDirectories();

                for( int i = 0; i < packageDirs.length; i++ )
                {
                    File pd = new File( source, packageDirs[ i ] );
                    String[] files = pd.list(
                        new FilenameFilter()
                        {
                            public boolean accept( File dir1, String name )
                            {
                                if( name.endsWith( ".java" ) )
                                {
                                    return true;
                                }
                                return false;// ignore dirs
                            }
                        } );

                    if( files.length > 0 )
                    {
                        String pkgDir = packageDirs[ i ].replace( '/', '.' ).replace( '\\', '.' );
                        if( !addedPackages.contains( pkgDir ) )
                        {
                            if( m_useExternalFile )
                            {
                                packageListWriter.println( pkgDir );
                            }
                            else
                            {
                                toExecute.createArgument().setValue( pkgDir );
                            }
                            addedPackages.add( pkgDir );
                        }
                    }
                }
            }
        }
        catch( IOException ioex )
        {
            throw new TaskException( "Error creating temporary file", ioex );
        }
        finally
        {
            if( packageListWriter != null )
            {
                packageListWriter.close();
            }
        }
    }

    /**
     * Receive notification about the process writing
     * to standard output.
     */
    public void stdout( final String line )
    {
        if( line.startsWith( "Generating " ) || line.startsWith( "Building " ) )
        {
            getLogger().debug( line );
        }
        else
        {
            getLogger().info( line );
        }
    }

    /**
     * Receive notification about the process writing
     * to standard error.
     */
    public void stderr( final String line )
    {
        getLogger().warn( line );
    }
}
