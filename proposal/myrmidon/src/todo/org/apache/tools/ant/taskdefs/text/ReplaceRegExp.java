/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.text;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.types.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.util.regexp.Regexp;

/**
 * <pre>
 * Task to do regular expression string replacements in a text
 * file.  The input file(s) must be able to be properly processed by
 * a Reader instance.  That is, they must be text only, no binary.
 *
 * The syntax of the regular expression depends on the implemtation that
 * you choose to use. The system property <code>ant.regexp.regexpimpl</code>
 * will be the classname of the implementation that will be used (the default is
 * <code>org.apache.tools.ant.util.regexp.JakartaOroRegexp</code> and requires
 * the Jakarta Oro Package). <pre>
 * For jdk  &lt;= 1.3, there are two available implementations:
 *   org.apache.tools.ant.util.regexp.JakartaOroRegexp (the default)
 *        Requires  the jakarta-oro package
 *
 *   org.apache.tools.ant.util.regexp.JakartaRegexpRegexp
 *        Requires the jakarta-regexp package
 *
 * For jdk &gt;= 1.4 an additional implementation is available:
 *   org.apache.tools.ant.util.regexp.Jdk14RegexpRegexp
 *        Requires the jdk 1.4 built in regular expression package.
 * </pre> Usage: Call Syntax: &lt;replaceregexp file="file" match="pattern"
 * replace="pattern" flags="options"? byline="true|false"? &gt;
 * regularexpression? substitution? fileset* &lt;/replaceregexp&gt; NOTE: You
 * must have either the file attribute specified, or at least one fileset
 * subelement to operation on. You may not have the file attribute specified if
 * you nest fileset elements inside this task. Also, you cannot specify both
 * match and a regular expression subelement at the same time, nor can you
 * specify the replace attribute and the substitution subelement at the same
 * time. Attributes: file --&gt; A single file to operation on (mutually
 * exclusive with the fileset subelements) match --&gt; The Regular expression
 * to match replace --&gt; The Expression replacement string flags --&gt; The
 * options to give to the replacement g = Substitute all occurrences. default is
 * to replace only the first one i = Case insensitive match byline --&gt; Should
 * this file be processed a single line at a time (default is false) "true"
 * indicates to perform replacement on a line by line basis "false" indicates to
 * perform replacement on the whole file at once. Example: The following call
 * could be used to replace an old property name in a ".properties" file with a
 * new name. In the replace attribute, you can refer to any part of the match
 * expression in parenthesis using backslash followed by a number like '\1'.
 * &lt;replaceregexp file="test.properties" match="MyProperty=(.*)"
 * replace="NewProperty=\1" byline="true" /&gt; </pre>
 *
 * @author <a href="mailto:mattinger@mindless.com">Matthew Inger</a>
 */
public class ReplaceRegExp extends Task
{
    private boolean byline;

    private File file;
    private ArrayList filesets;
    private String flags;// Keep jdk 1.1 compliant so others can use this
    private RegularExpression regex;
    private Substitution subs;

    /**
     * Default Constructor
     */
    public ReplaceRegExp()
    {
        super();
        this.file = null;
        this.filesets = new ArrayList();
        this.flags = "";
        this.byline = false;

        this.regex = null;
        this.subs = null;
    }

    public void setByLine( String byline )
    {
        Boolean res = Boolean.valueOf( byline );
        if( res == null ) {
            res = Boolean.FALSE;
        }
        this.byline = res.booleanValue();
    }

    public void setFile( File file )
    {
        this.file = file;
    }

    public void setFlags( String flags )
    {
        this.flags = flags;
    }

    public void setMatch( String match )
        throws TaskException
    {
        if( regex != null ) {
            throw new TaskException( "Only one regular expression is allowed" );
        }

        regex = new RegularExpression();
        regex.setPattern( match );
    }

    public void setReplace( String replace )
        throws TaskException
    {
        if( subs != null ) {
            throw new TaskException( "Only one substitution expression is allowed" );
        }

        subs = new Substitution();
        subs.setExpression( replace );
    }

    public void addFileset( FileSet set )
    {
        filesets.add( set );
    }

    public RegularExpression createRegularExpression()
        throws TaskException
    {
        if( regex != null ) {
            throw new TaskException( "Only one regular expression is allowed." );
        }

        regex = new RegularExpression();
        return regex;
    }

    public Substitution createSubstitution()
        throws TaskException
    {
        if( subs != null ) {
            throw new TaskException( "Only one substitution expression is allowed" );
        }

        subs = new Substitution();
        return subs;
    }

    public void execute()
        throws TaskException
    {
        if( regex == null ) {
            throw new TaskException( "No expression to match." );
        }
        if( subs == null ) {
            throw new TaskException( "Nothing to replace expression with." );
        }

        if( file != null && filesets.size() > 0 ) {
            throw new TaskException( "You cannot supply the 'file' attribute and filesets at the same time." );
        }

        int options = 0;

        if( flags.indexOf( 'g' ) != -1 ) {
            options |= Regexp.REPLACE_ALL;
        }

        if( flags.indexOf( 'i' ) != -1 ) {
            options |= Regexp.MATCH_CASE_INSENSITIVE;
        }

        if( flags.indexOf( 'm' ) != -1 ) {
            options |= Regexp.MATCH_MULTILINE;
        }

        if( flags.indexOf( 's' ) != -1 ) {
            options |= Regexp.MATCH_SINGLELINE;
        }

        if( file != null && file.exists() )
        {
            try
            {
                doReplace( file, options );
            }
            catch( IOException e )
            {
                final String message = "An error occurred processing file: '" +
                    file.getAbsolutePath() + "': " + e.toString();
                getLogger().error( message, e );
            }
        }
        else if( file != null )
        {
            final String message =
                "The following file is missing: '" + file.getAbsolutePath() + "'";
            getLogger().error( message );
        }

        int sz = filesets.size();
        for( int i = 0; i < sz; i++ )
        {
            FileSet fs = (FileSet)( filesets.get( i ) );
            DirectoryScanner ds = fs.getDirectoryScanner();

            String files[] = ds.getIncludedFiles();
            for( int j = 0; j < files.length; j++ )
            {
                File f = new File( files[ j ] );
                if( f.exists() )
                {
                    try
                    {
                        doReplace( f, options );
                    }
                    catch( Exception e )
                    {
                        final String message = "An error occurred processing file: '" + f.getAbsolutePath() +
                            "': " + e.toString();
                        getLogger().error( message );
                    }
                }
                else
                {
                    final String message = "The following file is missing: '" + file.getAbsolutePath() + "'";
                    getLogger().error( message );
                }
            }
        }
    }

    protected String doReplace( RegularExpression r,
                                Substitution s,
                                String input,
                                int options )
        throws TaskException
    {
        String res = input;
        Regexp regexp = r.getRegexp();

        if( regexp.matches( input, options ) )
        {
            res = regexp.substitute( input, s.getExpression(), options );
        }

        return res;
    }

    /**
     * Perform the replace on the entire file
     *
     * @param f Description of Parameter
     * @param options Description of Parameter
     * @exception IOException Description of Exception
     */
    protected void doReplace( File f, int options )
        throws IOException, TaskException
    {
        File parentDir = new File( new File( f.getAbsolutePath() ).getParent() );
        File temp = File.createTempFile( "replace", ".txt", parentDir );

        FileReader r = null;
        FileWriter w = null;

        try
        {
            r = new FileReader( f );
            w = new FileWriter( temp );

            BufferedReader br = new BufferedReader( r );
            BufferedWriter bw = new BufferedWriter( w );
            PrintWriter pw = new PrintWriter( bw );

            boolean changes = false;

            final String message = "Replacing pattern '" + regex.getPattern() +
                "' with '" + subs.getExpression() +
                "' in '" + f.getPath() + "'" +
                ( byline ? " by line" : "" ) +
                ( flags.length() > 0 ? " with flags: '" + flags + "'" : "" ) +
                ".";
            getLogger().warn( message );

            if( byline )
            {
                LineNumberReader lnr = new LineNumberReader( br );
                String line = null;

                while( ( line = lnr.readLine() ) != null )
                {
                    String res = doReplace( regex, subs, line, options );
                    if( !res.equals( line ) ) {
                        changes = true;
                    }

                    pw.println( res );
                }
                pw.flush();
            }
            else
            {
                int flen = (int)( f.length() );
                char tmpBuf[] = new char[ flen ];
                int numread = 0;
                int totread = 0;
                while( numread != -1 && totread < flen )
                {
                    numread = br.read( tmpBuf, totread, flen );
                    totread += numread;
                }

                String buf = new String( tmpBuf );

                String res = doReplace( regex, subs, buf, options );
                if( !res.equals( buf ) ) {
                    changes = true;
                }

                pw.println( res );
                pw.flush();
            }

            r.close();
            r = null;
            w.close();
            w = null;

            if( changes )
            {
                f.delete();
                temp.renameTo( f );
            }
            else
            {
                temp.delete();
            }
        }
        finally
        {
            try
            {
                if( r != null ) {
                    r.close();
                }
            }
            catch( Exception e )
            {
            }
            ;

            try
            {
                if( w != null ) {
                    r.close();
                }
            }
            catch( Exception e )
            {
            }
            ;
        }
    }

}

