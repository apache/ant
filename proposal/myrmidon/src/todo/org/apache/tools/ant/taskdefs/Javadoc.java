/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs;

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
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.exec.Execute;
import org.apache.tools.ant.taskdefs.exec.LogOutputStream;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;

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

public class Javadoc extends Task
{
    private static boolean javadoc1 =
        ( Project.getJavaVersion() == Project.JAVA_1_1 );

    private Commandline cmd = new Commandline();

    private boolean foundJavaFile = false;
    private boolean failOnError = false;
    private Path sourcePath = null;
    private File destDir = null;
    private ArrayList sourceFiles = new ArrayList();
    private ArrayList packageNames = new ArrayList( 5 );
    private ArrayList excludePackageNames = new ArrayList( 1 );
    private boolean author = true;
    private boolean version = true;
    private DocletInfo doclet = null;
    private Path classpath = null;
    private Path bootclasspath = null;
    private String group = null;
    private ArrayList compileList = new ArrayList( 10 );
    private String packageList = null;
    private ArrayList links = new ArrayList( 2 );
    private ArrayList groups = new ArrayList( 2 );
    private boolean useDefaultExcludes = true;
    private Html doctitle = null;
    private Html header = null;
    private Html footer = null;
    private Html bottom = null;
    private boolean useExternalFile = false;
    private File tmpList = null;

    public void setAccess( AccessType at )
    {
        cmd.createArgument().setValue( "-" + at.getValue() );
    }

    public void setAdditionalparam( String add )
        throws TaskException
    {
        cmd.createArgument().setLine( add );
    }

    public void setAuthor( boolean src )
    {
        author = src;
    }

    /**
     * Adds a reference to a CLASSPATH defined elsewhere.
     *
     * @param r The new BootClasspathRef value
     */
    public void setBootClasspathRef( Reference r )
        throws TaskException
    {
        createBootclasspath().setRefid( r );
    }

    public void setBootclasspath( Path src )
        throws TaskException
    {
        if( bootclasspath == null )
        {
            bootclasspath = src;
        }
        else
        {
            bootclasspath.append( src );
        }
    }

    public void setBottom( String src )
    {
        Html h = new Html();
        h.addText( src );
        addBottom( h );
    }

    public void setCharset( String src )
    {
        this.add12ArgIfNotEmpty( "-charset", src );
    }

    public void setClasspath( Path src )
        throws TaskException
    {
        if( classpath == null )
        {
            classpath = src;
        }
        else
        {
            classpath.append( src );
        }
    }

    /**
     * Adds a reference to a CLASSPATH defined elsewhere.
     *
     * @param r The new ClasspathRef value
     */
    public void setClasspathRef( Reference r )
        throws TaskException
    {
        createClasspath().setRefid( r );
    }

    /**
     * Sets whether default exclusions should be used or not.
     *
     * @param useDefaultExcludes "true"|"on"|"yes" when default exclusions
     *      should be used, "false"|"off"|"no" when they shouldn't be used.
     */
    public void setDefaultexcludes( boolean useDefaultExcludes )
    {
        this.useDefaultExcludes = useDefaultExcludes;
    }

    public void setDestdir( File dir )
    {
        destDir = dir;
        cmd.createArgument().setValue( "-d" );
        cmd.createArgument().setFile( destDir );
    }

    public void setDocencoding( String enc )
    {
        cmd.createArgument().setValue( "-docencoding" );
        cmd.createArgument().setValue( enc );
    }

    public void setDoclet( String src )
    {
        if( doclet == null )
        {
            doclet = new DocletInfo();
        }
        doclet.setName( src );
    }

    public void setDocletPath( Path src )
        throws TaskException
    {
        if( doclet == null )
        {
            doclet = new DocletInfo();
        }
        doclet.setPath( src );
    }

    public void setDocletPathRef( Reference r )
        throws TaskException
    {
        if( doclet == null )
        {
            doclet = new DocletInfo();
        }
        doclet.createPath().setRefid( r );
    }

    public void setDoctitle( String src )
    {
        Html h = new Html();
        h.addText( src );
        addDoctitle( h );
    }

    public void setEncoding( String enc )
    {
        cmd.createArgument().setValue( "-encoding" );
        cmd.createArgument().setValue( enc );
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
        if( !javadoc1 )
        {
            cmd.createArgument().setValue( "-extdirs" );
            cmd.createArgument().setValue( src );
        }
    }

    /**
     * Should the build process fail if javadoc fails (as indicated by a non
     * zero return code)? <p>
     *
     * Default is false.</p>
     *
     * @param b The new Failonerror value
     */
    public void setFailonerror( boolean b )
    {
        failOnError = b;
    }

    public void setFooter( String src )
    {
        Html h = new Html();
        h.addText( src );
        addFooter( h );
    }

    public void setGroup( String src )
    {
        group = src;
    }

    public void setHeader( String src )
    {
        Html h = new Html();
        h.addText( src );
        addHeader( h );
    }

    public void setHelpfile( File f )
    {
        if( !javadoc1 )
        {
            cmd.createArgument().setValue( "-helpfile" );
            cmd.createArgument().setFile( f );
        }
    }

    public void setLink( String src )
    {
        if( !javadoc1 )
        {
            createLink().setHref( src );
        }
    }

    public void setLinkoffline( String src )
        throws TaskException
    {
        if( !javadoc1 )
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
    }

    public void setLocale( String src )
    {
        if( !javadoc1 )
        {
            cmd.createArgument().setValue( "-locale" );
            cmd.createArgument().setValue( src );
        }
    }

    public void setMaxmemory( String max )
    {
        if( javadoc1 )
        {
            cmd.createArgument().setValue( "-J-mx" + max );
        }
        else
        {
            cmd.createArgument().setValue( "-J-Xmx" + max );
        }
    }

    public void setNodeprecated( boolean b )
    {
        addArgIf( b, "-nodeprecated" );
    }

    public void setNodeprecatedlist( boolean b )
    {
        add12ArgIf( b, "-nodeprecatedlist" );
    }

    public void setNohelp( boolean b )
    {
        add12ArgIf( b, "-nohelp" );
    }

    public void setNoindex( boolean b )
    {
        addArgIf( b, "-noindex" );
    }

    public void setNonavbar( boolean b )
    {
        add12ArgIf( b, "-nonavbar" );
    }

    public void setNotree( boolean b )
    {
        addArgIf( b, "-notree" );
    }

    public void setOld( boolean b )
    {
        add12ArgIf( b, "-1.1" );
    }

    public void setOverview( File f )
    {
        if( !javadoc1 )
        {
            cmd.createArgument().setValue( "-overview" );
            cmd.createArgument().setFile( f );
        }
    }

    public void setPackage( boolean b )
    {
        addArgIf( b, "-package" );
    }

    public void setPackageList( String src )
    {
        packageList = src;
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
        add12ArgIf( b, "-serialwarn" );
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
        if( sourcePath == null )
        {
            sourcePath = src;
        }
        else
        {
            sourcePath.append( src );
        }
    }

    /**
     * Adds a reference to a CLASSPATH defined elsewhere.
     *
     * @param r The new SourcepathRef value
     */
    public void setSourcepathRef( Reference r )
        throws TaskException
    {
        createSourcepath().setRefid( r );
    }

    public void setSplitindex( boolean b )
    {
        add12ArgIf( b, "-splitindex" );
    }

    public void setStylesheetfile( File f )
    {
        if( !javadoc1 )
        {
            cmd.createArgument().setValue( "-stylesheetfile" );
            cmd.createArgument().setFile( f );
        }
    }

    public void setUse( boolean b )
    {
        add12ArgIf( b, "-use" );
    }

    /**
     * Work around command line length limit by using an external file for the
     * sourcefiles.
     *
     * @param b The new UseExternalFile value
     */
    public void setUseExternalFile( boolean b )
    {
        if( !javadoc1 )
        {
            useExternalFile = b;
        }
    }

    public void setVerbose( boolean b )
    {
        add12ArgIf( b, "-verbose" );
    }

    public void setVersion( boolean src )
    {
        version = src;
    }

    public void setWindowtitle( String src )
    {
        add12ArgIfNotEmpty( "-windowtitle", src );
    }

    public void addBottom( Html text )
    {
        if( !javadoc1 )
        {
            bottom = text;
        }
    }

    public void addDoctitle( Html text )
    {
        if( !javadoc1 )
        {
            doctitle = text;
        }
    }

    public void addExcludePackage( PackageName pn )
    {
        excludePackageNames.add( pn );
    }

    public void addFooter( Html text )
    {
        if( !javadoc1 )
        {
            footer = text;
        }
    }

    public void addHeader( Html text )
    {
        if( !javadoc1 )
        {
            header = text;
        }
    }

    public void addPackage( PackageName pn )
    {
        packageNames.add( pn );
    }

    public void addSource( SourceFile sf )
        throws TaskException
    {
        sourceFiles.add( sf );
    }

    public Path createBootclasspath()
        throws TaskException
    {
        if( bootclasspath == null )
        {
            bootclasspath = new Path( getProject() );
        }
        return bootclasspath.createPath();
    }

    public Path createClasspath()
        throws TaskException
    {
        if( classpath == null )
        {
            classpath = new Path( getProject() );
        }
        return classpath.createPath();
    }

    public DocletInfo createDoclet()
    {
        doclet = new DocletInfo();
        return doclet;
    }

    public GroupArgument createGroup()
    {
        GroupArgument ga = new GroupArgument();
        groups.add( ga );
        return ga;
    }

    public LinkArgument createLink()
    {
        LinkArgument la = new LinkArgument();
        links.add( la );
        return la;
    }

    public Path createSourcepath()
        throws TaskException
    {
        if( sourcePath == null )
        {
            sourcePath = new Path( getProject() );
        }
        return sourcePath.createPath();
    }

    public void execute()
        throws TaskException
    {
        if( sourcePath == null )
        {
            String msg = "sourcePath attribute must be set!";
            throw new TaskException( msg );
        }

        log( "Generating Javadoc", Project.MSG_INFO );

        if( doctitle != null )
        {
            cmd.createArgument().setValue( "-doctitle" );
            cmd.createArgument().setValue( expand( doctitle.getText() ) );
        }
        if( header != null )
        {
            cmd.createArgument().setValue( "-header" );
            cmd.createArgument().setValue( expand( header.getText() ) );
        }
        if( footer != null )
        {
            cmd.createArgument().setValue( "-footer" );
            cmd.createArgument().setValue( expand( footer.getText() ) );
        }
        if( bottom != null )
        {
            cmd.createArgument().setValue( "-bottom" );
            cmd.createArgument().setValue( expand( bottom.getText() ) );
        }

        Commandline toExecute = (Commandline)cmd.clone();
        toExecute.setExecutable( getJavadocExecutableName() );

        // ------------------------------------------------ general javadoc arguments
        if( classpath == null )
            classpath = Path.systemClasspath;
        else
            classpath = classpath.concatSystemClasspath( "ignore" );

        if( !javadoc1 )
        {
            toExecute.createArgument().setValue( "-classpath" );
            toExecute.createArgument().setPath( classpath );
            toExecute.createArgument().setValue( "-sourcepath" );
            toExecute.createArgument().setPath( sourcePath );
        }
        else
        {
            toExecute.createArgument().setValue( "-classpath" );
            toExecute.createArgument().setValue( sourcePath.toString() +
                                                 System.getProperty( "path.separator" ) + classpath.toString() );
        }

        if( version && doclet == null )
            toExecute.createArgument().setValue( "-version" );
        if( author && doclet == null )
            toExecute.createArgument().setValue( "-author" );

        if( javadoc1 || doclet == null )
        {
            if( destDir == null )
            {
                String msg = "destDir attribute must be set!";
                throw new TaskException( msg );
            }
        }

        // --------------------------------- javadoc2 arguments for default doclet

        // XXX: how do we handle a custom doclet?

        if( !javadoc1 )
        {
            if( doclet != null )
            {
                if( doclet.getName() == null )
                {
                    throw new TaskException( "The doclet name must be specified." );
                }
                else
                {
                    toExecute.createArgument().setValue( "-doclet" );
                    toExecute.createArgument().setValue( doclet.getName() );
                    if( doclet.getPath() != null )
                    {
                        toExecute.createArgument().setValue( "-docletpath" );
                        toExecute.createArgument().setPath( doclet.getPath() );
                    }
                    for( Iterator e = doclet.getParams(); e.hasNext(); )
                    {
                        DocletParam param = (DocletParam)e.next();
                        if( param.getName() == null )
                        {
                            throw new TaskException( "Doclet parameters must have a name" );
                        }

                        toExecute.createArgument().setValue( param.getName() );
                        if( param.getValue() != null )
                        {
                            toExecute.createArgument().setValue( param.getValue() );
                        }
                    }
                }
            }
            if( bootclasspath != null )
            {
                toExecute.createArgument().setValue( "-bootclasspath" );
                toExecute.createArgument().setPath( bootclasspath );
            }

            // add the links arguments
            if( links.size() != 0 )
            {
                for( Iterator e = links.iterator(); e.hasNext(); )
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
                            toExecute.createArgument().setValue( "-linkoffline" );
                            toExecute.createArgument().setValue( la.getHref() );
                            toExecute.createArgument().setValue( packageListLocation.getAbsolutePath() );
                        }
                        else
                        {
                            log( "Warning: No package list was found at " + packageListLocation,
                                 Project.MSG_VERBOSE );
                        }
                    }
                    else
                    {
                        toExecute.createArgument().setValue( "-link" );
                        toExecute.createArgument().setValue( la.getHref() );
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
            if( group != null )
            {
                StringTokenizer tok = new StringTokenizer( group, ",", false );
                while( tok.hasMoreTokens() )
                {
                    String grp = tok.nextToken().trim();
                    int space = grp.indexOf( " " );
                    if( space > 0 )
                    {
                        String name = grp.substring( 0, space );
                        String pkgList = grp.substring( space + 1 );
                        toExecute.createArgument().setValue( "-group" );
                        toExecute.createArgument().setValue( name );
                        toExecute.createArgument().setValue( pkgList );
                    }
                }
            }

            // add the group arguments
            if( groups.size() != 0 )
            {
                for( Iterator e = groups.iterator(); e.hasNext(); )
                {
                    GroupArgument ga = (GroupArgument)e.next();
                    String title = ga.getTitle();
                    String packages = ga.getPackages();
                    if( title == null || packages == null )
                    {
                        throw new TaskException( "The title and packages must be specified for group elements." );
                    }
                    toExecute.createArgument().setValue( "-group" );
                    toExecute.createArgument().setValue( expand( title ) );
                    toExecute.createArgument().setValue( packages );
                }
            }

        }

        tmpList = null;
        if( packageNames.size() > 0 )
        {
            ArrayList packages = new ArrayList();
            Iterator enum = packageNames.iterator();
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
                    toExecute.createArgument().setValue( name );
                }
            }

            ArrayList excludePackages = new ArrayList();
            if( excludePackageNames.size() > 0 )
            {
                enum = excludePackageNames.iterator();
                while( enum.hasNext() )
                {
                    PackageName pn = (PackageName)enum.next();
                    excludePackages.add( pn.getName().trim() );
                }
            }
            if( packages.size() > 0 )
            {
                evaluatePackages( toExecute, sourcePath, packages, excludePackages );
            }
        }

        if( sourceFiles.size() > 0 )
        {
            PrintWriter srcListWriter = null;
            try
            {

                /**
                 * Write sourcefiles to a temporary file if requested.
                 */
                if( useExternalFile )
                {
                    if( tmpList == null )
                    {
                        tmpList = File.createTempFile( "javadoc", "", getBaseDirectory() );
                        toExecute.createArgument().setValue( "@" + tmpList.getAbsolutePath() );
                    }
                    srcListWriter = new PrintWriter( new FileWriter( tmpList.getAbsolutePath(),
                                                                     true ) );
                }

                Iterator enum = sourceFiles.iterator();
                while( enum.hasNext() )
                {
                    SourceFile sf = (SourceFile)enum.next();
                    String sourceFileName = sf.getFile().getAbsolutePath();
                    if( useExternalFile )
                    {
                        srcListWriter.println( sourceFileName );
                    }
                    else
                    {
                        toExecute.createArgument().setValue( sourceFileName );
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

        if( packageList != null )
        {
            toExecute.createArgument().setValue( "@" + packageList );
        }
        log( "Javadoc args: " + toExecute, Project.MSG_VERBOSE );

        log( "Javadoc execution", Project.MSG_INFO );

        JavadocOutputStream out = new JavadocOutputStream( Project.MSG_INFO );
        JavadocOutputStream err = new JavadocOutputStream( Project.MSG_WARN );
        Execute exe = new Execute();
        exe.setOutput( out );
        exe.setError( err );

        /*
         * No reason to change the working directory as all filenames and
         * path components have been resolved already.
         *
         * Avoid problems with command line length in some environments.
         */
        exe.setWorkingDirectory( null );
        try
        {
            exe.setCommandline( toExecute.getCommandline() );
            int ret = exe.execute();
            if( ret != 0 && failOnError )
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

            if( tmpList != null )
            {
                tmpList.delete();
                tmpList = null;
            }

            out.logFlush();
            err.logFlush();
            try
            {
                out.close();
                err.close();
            }
            catch( IOException e )
            {
            }
        }
    }

    /**
     * Convenience method to expand properties.
     *
     * @param content Description of Parameter
     * @return Description of the Returned Value
     */
    protected String expand( String content )
        throws TaskException
    {
        return getProject().replaceProperties( content );
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
                log( "Unable to locate " + jdocExecutable.getAbsolutePath() +
                     ". Using \"javadoc\" instead.", Project.MSG_VERBOSE );
            }
            return "javadoc";
        }
    }

    private void add11ArgIf( boolean b, String arg )
    {
        if( javadoc1 && b )
        {
            cmd.createArgument().setValue( arg );
        }
    }

    private void add12ArgIf( boolean b, String arg )
    {
        if( !javadoc1 && b )
        {
            cmd.createArgument().setValue( arg );
        }
    }

    private void add12ArgIfNotEmpty( String key, String value )
    {
        if( !javadoc1 )
        {
            if( value != null && value.length() != 0 )
            {
                cmd.createArgument().setValue( key );
                cmd.createArgument().setValue( value );
            }
            else
            {
                log( "Warning: Leaving out empty argument '" + key + "'", Project.MSG_WARN );
            }
        }
    }

    private void addArgIf( boolean b, String arg )
    {
        if( b )
        {
            cmd.createArgument().setValue( arg );
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
        log( "Source path = " + sourcePath.toString(), Project.MSG_VERBOSE );
        StringBuffer msg = new StringBuffer( "Packages = " );
        for( int i = 0; i < packages.size(); i++ )
        {
            if( i > 0 )
            {
                msg.append( "," );
            }
            msg.append( packages.get( i ) );
        }
        log( msg.toString(), Project.MSG_VERBOSE );

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
        log( msg.toString(), Project.MSG_VERBOSE );

        ArrayList addedPackages = new ArrayList();

        String[] list = sourcePath.list();
        if( list == null )
            list = new String[ 0 ];

        FileSet fs = new FileSet();
        fs.setDefaultexcludes( useDefaultExcludes );

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
            if( useExternalFile )
            {
                tmpList = File.createTempFile( "javadoc", "", getBaseDirectory() );
                toExecute.createArgument().setValue( "@" + tmpList.getAbsolutePath() );
                packageListWriter = new PrintWriter( new FileWriter( tmpList ) );
            }

            for( int j = 0; j < list.length; j++ )
            {
                File source = resolveFile( list[ j ] );
                fs.setDir( source );

                DirectoryScanner ds = fs.getDirectoryScanner( getProject() );
                String[] packageDirs = ds.getIncludedDirectories();

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
                            if( useExternalFile )
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

    public static class AccessType extends EnumeratedAttribute
    {
        public String[] getValues()
        {
            // Protected first so if any GUI tool offers a default
            // based on enum #0, it will be right.
            return new String[]{"protected", "public", "package", "private"};
        }
    }

    public static class Html
    {
        private StringBuffer text = new StringBuffer();

        public String getText()
        {
            return text.toString();
        }

        public void addText( String t )
        {
            text.append( t );
        }
    }

    public static class PackageName
    {
        private String name;

        public void setName( String name )
        {
            this.name = name;
        }

        public String getName()
        {
            return name;
        }

        public String toString()
        {
            return getName();
        }
    }

    public static class SourceFile
    {
        private File file;

        public void setFile( File file )
        {
            this.file = file;
        }

        public File getFile()
        {
            return file;
        }
    }

    public class DocletInfo
    {

        private ArrayList params = new ArrayList();
        private String name;
        private Path path;

        public void setName( String name )
        {
            this.name = name;
        }

        public void setPath( Path path )
            throws TaskException
        {
            if( this.path == null )
            {
                this.path = path;
            }
            else
            {
                this.path.append( path );
            }
        }

        /**
         * Adds a reference to a CLASSPATH defined elsewhere.
         *
         * @param r The new PathRef value
         */
        public void setPathRef( Reference r )
            throws TaskException
        {
            createPath().setRefid( r );
        }

        public String getName()
        {
            return name;
        }

        public Iterator getParams()
        {
            return params.iterator();
        }

        public Path getPath()
        {
            return path;
        }

        public DocletParam createParam()
        {
            DocletParam param = new DocletParam();
            params.add( param );

            return param;
        }

        public Path createPath()
            throws TaskException
        {
            if( path == null )
            {
                path = new Path( getProject() );
            }
            return path.createPath();
        }
    }

    public class DocletParam
    {
        private String name;
        private String value;

        public void setName( String name )
        {
            this.name = name;
        }

        public void setValue( String value )
        {
            this.value = value;
        }

        public String getName()
        {
            return name;
        }

        public String getValue()
        {
            return value;
        }
    }

    public class GroupArgument
    {
        private ArrayList packages = new ArrayList( 3 );
        private Html title;

        public GroupArgument()
        {
        }

        public void setPackages( String src )
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

        public void setTitle( String src )
        {
            Html h = new Html();
            h.addText( src );
            addTitle( h );
        }

        public String getPackages()
        {
            StringBuffer p = new StringBuffer();
            for( int i = 0; i < packages.size(); i++ )
            {
                if( i > 0 )
                {
                    p.append( ":" );
                }
                p.append( packages.get( i ).toString() );
            }
            return p.toString();
        }

        public String getTitle()
        {
            return title != null ? title.getText() : null;
        }

        public void addPackage( PackageName pn )
        {
            packages.add( pn );
        }

        public void addTitle( Html text )
        {
            title = text;
        }
    }

    public class LinkArgument
    {
        private boolean offline = false;
        private String href;
        private File packagelistLoc;

        public LinkArgument()
        {
        }

        public void setHref( String hr )
        {
            href = hr;
        }

        public void setOffline( boolean offline )
        {
            this.offline = offline;
        }

        public void setPackagelistLoc( File src )
        {
            packagelistLoc = src;
        }

        public String getHref()
        {
            return href;
        }

        public File getPackagelistLoc()
        {
            return packagelistLoc;
        }

        public boolean isLinkOffline()
        {
            return offline;
        }
    }

    private class JavadocOutputStream extends LogOutputStream
    {

        //
        // Override the logging of output in order to filter out Generating
        // messages.  Generating messages are set to a priority of VERBOSE
        // unless they appear after what could be an informational message.
        //
        private String queuedLine = null;

        JavadocOutputStream( int level )
        {
            super( Javadoc.this, level );
        }

        protected void logFlush()
        {
            if( queuedLine != null )
            {
                super.processLine( queuedLine, Project.MSG_VERBOSE );
                queuedLine = null;
            }
        }

        protected void processLine( String line, int messageLevel )
        {
            if( messageLevel == Project.MSG_INFO && line.startsWith( "Generating " ) )
            {
                if( queuedLine != null )
                {
                    super.processLine( queuedLine, Project.MSG_VERBOSE );
                }
                queuedLine = line;
            }
            else
            {
                if( queuedLine != null )
                {
                    if( line.startsWith( "Building " ) )
                        super.processLine( queuedLine, Project.MSG_VERBOSE );
                    else
                        super.processLine( queuedLine, Project.MSG_INFO );
                    queuedLine = null;
                }
                super.processLine( line, messageLevel );
            }
        }
    }

}
