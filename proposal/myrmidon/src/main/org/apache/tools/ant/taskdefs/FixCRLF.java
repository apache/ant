/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.util.FileUtils;

/**
 * Task to convert text source files to local OS formatting conventions, as well
 * as repair text files damaged by misconfigured or misguided editors or file
 * transfer programs. <p>
 *
 * This task can take the following arguments:
 * <ul>
 *   <li> srcdir
 *   <li> destdir
 *   <li> include
 *   <li> exclude
 *   <li> cr
 *   <li> eol
 *   <li> tab
 *   <li> eof
 *   <li> encoding
 * </ul>
 * Of these arguments, only <b>sourcedir</b> is required. <p>
 *
 * When this task executes, it will scan the srcdir based on the include and
 * exclude properties. <p>
 *
 * This version generalises the handling of EOL characters, and allows for
 * CR-only line endings (which I suspect is the standard on Macs.) Tab handling
 * has also been generalised to accommodate any tabwidth from 2 to 80,
 * inclusive. Importantly, it will leave untouched any literal TAB characters
 * embedded within string or character constants. <p>
 *
 * <em>Warning:</em> do not run on binary files. <em>Caution:</em> run with care
 * on carefully formatted files. This may sound obvious, but if you don't
 * specify asis, presume that your files are going to be modified. If "tabs" is
 * "add" or "remove", whitespace characters may be added or removed as
 * necessary. Similarly, for CR's - in fact "eol"="crlf" or cr="add" can result
 * in cr characters being removed in one special case accommodated, i.e., CRCRLF
 * is regarded as a single EOL to handle cases where other programs have
 * converted CRLF into CRCRLF.
 *
 * @author Sam Ruby <a href="mailto:rubys@us.ibm.com">rubys@us.ibm.com</a>
 * @author <a href="mailto:pbwest@powerup.com.au">Peter B. West</a>
 * @version $Revision$ $Name$
 */

public class FixCRLF extends MatchingTask
{

    private final static int UNDEF = -1;
    private final static int NOTJAVA = 0;
    private final static int LOOKING = 1;
    private final static int IN_CHAR_CONST = 2;
    private final static int IN_STR_CONST = 3;
    private final static int IN_SINGLE_COMMENT = 4;
    private final static int IN_MULTI_COMMENT = 5;

    private final static int ASIS = 0;
    private final static int CR = 1;
    private final static int LF = 2;
    private final static int CRLF = 3;
    private final static int ADD = 1;
    private final static int REMOVE = -1;
    private final static int SPACES = -1;
    private final static int TABS = 1;

    private final static int INBUFLEN = 8192;
    private final static int LINEBUFLEN = 200;

    private final static char CTRLZ = '\u001A';

    private int tablength = 8;
    private String spaces = "        ";
    private StringBuffer linebuf = new StringBuffer( 1024 );
    private StringBuffer linebuf2 = new StringBuffer( 1024 );
    private boolean javafiles = false;
    private File destDir = null;

    /**
     * Encoding to assume for the files
     */
    private String encoding = null;
    private int ctrlz;
    private int eol;
    private String eolstr;

    private File srcDir;
    private int tabs;

    /**
     * Defaults the properties based on the system type.
     * <ul>
     *   <li> Unix: eol="LF" tab="asis" eof="remove"
     *   <li> Mac: eol="CR" tab="asis" eof="remove"
     *   <li> DOS: eol="CRLF" tab="asis" eof="asis"
     * </ul>
     *
     */
    public FixCRLF()
    {
        tabs = ASIS;
        if( System.getProperty( "path.separator" ).equals( ":" ) )
        {
            ctrlz = REMOVE;
            if( System.getProperty( "os.name" ).indexOf( "Mac" ) > -1 )
            {
                eol = CR;
                eolstr = "\r";
            }
            else
            {
                eol = LF;
                eolstr = "\n";
            }
        }
        else
        {
            ctrlz = ASIS;
            eol = CRLF;
            eolstr = "\r\n";
        }
    }

    /**
     * Set the destination where the fixed files should be placed. Default is to
     * replace the original file.
     *
     * @param destDir The new Destdir value
     */
    public void setDestdir( File destDir )
    {
        this.destDir = destDir;
    }

    /**
     * Specifies the encoding Ant expects the files to be in - defaults to the
     * platforms default encoding.
     *
     * @param encoding The new Encoding value
     */
    public void setEncoding( String encoding )
    {
        this.encoding = encoding;
    }

    /**
     * Specify how DOS EOF (control-z) charaters are to be handled
     *
     * @param attr The new Eof value
     */
    public void setEof( AddAsisRemove attr )
    {
        String option = attr.getValue();
        if( option.equals( "remove" ) )
        {
            ctrlz = REMOVE;
        }
        else if( option.equals( "asis" ) )
        {
            ctrlz = ASIS;
        }
        else
        {
            // must be "add"
            ctrlz = ADD;
        }
    }

    /**
     * Specify how EndOfLine characters are to be handled
     *
     * @param attr The new Eol value
     */
    public void setEol( CrLf attr )
    {
        String option = attr.getValue();
        if( option.equals( "asis" ) )
        {
            eol = ASIS;
        }
        else if( option.equals( "cr" ) )
        {
            eol = CR;
            eolstr = "\r";
        }
        else if( option.equals( "lf" ) )
        {
            eol = LF;
            eolstr = "\n";
        }
        else
        {
            // Must be "crlf"
            eol = CRLF;
            eolstr = "\r\n";
        }
    }

    /**
     * Fixing Java source files?
     *
     * @param javafiles The new Javafiles value
     */
    public void setJavafiles( boolean javafiles )
    {
        this.javafiles = javafiles;
    }

    /**
     * Set the source dir to find the source text files.
     *
     * @param srcDir The new Srcdir value
     */
    public void setSrcdir( File srcDir )
    {
        this.srcDir = srcDir;
    }

    /**
     * Specify how tab characters are to be handled
     *
     * @param attr The new Tab value
     */
    public void setTab( AddAsisRemove attr )
    {
        String option = attr.getValue();
        if( option.equals( "remove" ) )
        {
            tabs = SPACES;
        }
        else if( option.equals( "asis" ) )
        {
            tabs = ASIS;
        }
        else
        {
            // must be "add"
            tabs = TABS;
        }
    }

    /**
     * Specify tab length in characters
     *
     * @param tlength specify the length of tab in spaces,
     * @exception TaskException Description of Exception
     */
    public void setTablength( int tlength )
        throws TaskException
    {
        if( tlength < 2 || tlength > 80 )
        {
            throw new TaskException( "tablength must be between 2 and 80" );
        }
        tablength = tlength;
        StringBuffer sp = new StringBuffer();
        for( int i = 0; i < tablength; i++ )
        {
            sp.append( ' ' );
        }
        spaces = sp.toString();
    }

    /**
     * Executes the task.
     *
     * @exception TaskException Description of Exception
     */
    public void execute()
        throws TaskException
    {
        // first off, make sure that we've got a srcdir and destdir

        if( srcDir == null )
        {
            throw new TaskException( "srcdir attribute must be set!" );
        }
        if( !srcDir.exists() )
        {
            throw new TaskException( "srcdir does not exist!" );
        }
        if( !srcDir.isDirectory() )
        {
            throw new TaskException( "srcdir is not a directory!" );
        }
        if( destDir != null )
        {
            if( !destDir.exists() )
            {
                throw new TaskException( "destdir does not exist!" );
            }
            if( !destDir.isDirectory() )
            {
                throw new TaskException( "destdir is not a directory!" );
            }
        }

        // log options used
        getLogger().debug( "options:" +
                           " eol=" +
                           ( eol == ASIS ? "asis" : eol == CR ? "cr" : eol == LF ? "lf" : "crlf" ) +
                           " tab=" + ( tabs == TABS ? "add" : tabs == ASIS ? "asis" : "remove" ) +
                           " eof=" + ( ctrlz == ADD ? "add" : ctrlz == ASIS ? "asis" : "remove" ) +
                           " tablength=" + tablength +
                           " encoding=" + ( encoding == null ? "default" : encoding ) );

        DirectoryScanner ds = super.getDirectoryScanner( srcDir );
        String[] files = ds.getIncludedFiles();

        for( int i = 0; i < files.length; i++ )
        {
            processFile( files[ i ] );
        }
    }

    /**
     * Creates a Reader reading from a given file an taking the user defined
     * encoding into account.
     *
     * @param f Description of Parameter
     * @return The Reader value
     * @exception IOException Description of Exception
     */
    private Reader getReader( File f )
        throws IOException
    {
        return ( encoding == null ) ? new FileReader( f )
            : new InputStreamReader( new FileInputStream( f ), encoding );
    }

    /**
     * Scan a BufferLine forward from the 'next' pointer for the end of a
     * character constant. Set 'lookahead' pointer to the character following
     * the terminating quote.
     *
     * @param bufline Description of Parameter
     * @param terminator Description of Parameter
     * @exception TaskException Description of Exception
     */
    private void endOfCharConst( OneLiner.BufferLine bufline, char terminator )
        throws TaskException
    {
        int ptr = bufline.getNext();
        int eol = bufline.length();
        char c;
        ptr++;// skip past initial quote
        while( ptr < eol )
        {
            if( ( c = bufline.getChar( ptr++ ) ) == '\\' )
            {
                ptr++;
            }
            else
            {
                if( c == terminator )
                {
                    bufline.setLookahead( ptr );
                    return;
                }
            }
        }// end of while (ptr < eol)
        // Must have fallen through to the end of the line
        throw new TaskException( "endOfCharConst: unterminated char constant" );
    }

    /**
     * Scan a BufferLine for the next state changing token: the beginning of a
     * single or multi-line comment, a character or a string constant. As a
     * side-effect, sets the buffer state to the next state, and sets field
     * lookahead to the first character of the state-changing token, or to the
     * next eol character.
     *
     * @param bufline Description of Parameter
     * @exception TaskException Description of Exception
     */
    private void nextStateChange( OneLiner.BufferLine bufline )
        throws TaskException
    {
        int eol = bufline.length();
        int ptr = bufline.getNext();

        //  Look for next single or double quote, double slash or slash star
        while( ptr < eol )
        {
            switch( bufline.getChar( ptr++ ) )
            {
                case '\'':
                    bufline.setState( IN_CHAR_CONST );
                    bufline.setLookahead( --ptr );
                    return;
                case '\"':
                    bufline.setState( IN_STR_CONST );
                    bufline.setLookahead( --ptr );
                    return;
                case '/':
                    if( ptr < eol )
                    {
                        if( bufline.getChar( ptr ) == '*' )
                        {
                            bufline.setState( IN_MULTI_COMMENT );
                            bufline.setLookahead( --ptr );
                            return;
                        }
                        else if( bufline.getChar( ptr ) == '/' )
                        {
                            bufline.setState( IN_SINGLE_COMMENT );
                            bufline.setLookahead( --ptr );
                            return;
                        }
                    }
                    break;
            }// end of switch (bufline.getChar(ptr++))

        }// end of while (ptr < eol)
        // Eol is the next token
        bufline.setLookahead( ptr );
    }

    /**
     * Process a BufferLine string which is not part of of a string constant.
     * The start position of the string is given by the 'next' field. Sets the
     * 'next' and 'column' fields in the BufferLine.
     *
     * @param bufline Description of Parameter
     * @param end Description of Parameter
     * @param outWriter Description of Parameter
     */
    private void notInConstant( OneLiner.BufferLine bufline, int end,
                                BufferedWriter outWriter )
        throws TaskException
    {
        // N.B. both column and string index are zero-based
        // Process a string not part of a constant;
        // i.e. convert tabs<->spaces as required
        // This is NOT called for ASIS tab handling
        int nextTab;
        int nextStop;
        int tabspaces;
        String line = bufline.substring( bufline.getNext(), end );
        int place = 0;// Zero-based
        int col = bufline.getColumn();// Zero-based

        // process sequences of white space
        // first convert all tabs to spaces
        linebuf.setLength( 0 );
        while( ( nextTab = line.indexOf( (int)'\t', place ) ) >= 0 )
        {
            linebuf.append( line.substring( place, nextTab ) );// copy to the TAB
            col += nextTab - place;
            tabspaces = tablength - ( col % tablength );
            linebuf.append( spaces.substring( 0, tabspaces ) );
            col += tabspaces;
            place = nextTab + 1;
        }// end of while
        linebuf.append( line.substring( place, line.length() ) );
        // if converting to spaces, all finished
        String linestring = new String( linebuf.toString() );
        if( tabs == REMOVE )
        {
            try
            {
                outWriter.write( linestring );
            }
            catch( IOException e )
            {
                throw new TaskException( "Error", e );
            }// end of try-catch
        }
        else
        {// tabs == ADD
            int tabCol;
            linebuf2.setLength( 0 );
            place = 0;
            col = bufline.getColumn();
            int placediff = col - 0;
            // for the length of the string, cycle through the tab stop
            // positions, checking for a space preceded by at least one
            // other space at the tab stop.  if so replace the longest possible
            // preceding sequence of spaces with a tab.
            nextStop = col + ( tablength - col % tablength );
            if( nextStop - col < 2 )
            {
                linebuf2.append( linestring.substring(
                    place, nextStop - placediff ) );
                place = nextStop - placediff;
                nextStop += tablength;
            }

            for( ; nextStop - placediff <= linestring.length()
                ; nextStop += tablength )
            {
                for( tabCol = nextStop;
                     --tabCol - placediff >= place
                    && linestring.charAt( tabCol - placediff ) == ' '
                    ; )
                {
                    ;// Loop for the side-effects
                }
                // tabCol is column index of the last non-space character
                // before the next tab stop
                if( nextStop - tabCol > 2 )
                {
                    linebuf2.append( linestring.substring(
                        place, ++tabCol - placediff ) );
                    linebuf2.append( '\t' );
                }
                else
                {
                    linebuf2.append( linestring.substring(
                        place, nextStop - placediff ) );
                }// end of else

                place = nextStop - placediff;
            }// end of for (nextStop ... )

            // pick up that last bit, if any
            linebuf2.append( linestring.substring( place, linestring.length() ) );

            try
            {
                outWriter.write( linebuf2.toString() );
            }
            catch( IOException e )
            {
                throw new TaskException( "Error", e );
            }// end of try-catch

        }// end of else tabs == ADD

        // Set column position as modified by this method
        bufline.setColumn( bufline.getColumn() + linestring.length() );
        bufline.setNext( end );

    }

    private void processFile( String file )
        throws TaskException
    {
        File srcFile = new File( srcDir, file );
        File destD = destDir == null ? srcDir : destDir;
        File tmpFile = null;
        BufferedWriter outWriter;
        OneLiner.BufferLine line;

        // read the contents of the file
        OneLiner lines = new OneLiner( srcFile );

        try
        {
            // Set up the output Writer
            try
            {
                tmpFile = File.createTempFile( "fixcrlf", "", destD );
                Writer writer = ( encoding == null ) ? new FileWriter( tmpFile )
                    : new OutputStreamWriter( new FileOutputStream( tmpFile ), encoding );
                outWriter = new BufferedWriter( writer );
            }
            catch( IOException e )
            {
                throw new TaskException( "Error", e );
            }

            while( lines.hasNext() )
            {
                // In-line states
                int endComment;

                try
                {
                    line = (OneLiner.BufferLine)lines.next();
                }
                catch( NoSuchElementException e )
                {
                    throw new TaskException( "Error", e );
                }

                String lineString = line.getLineString();
                int linelen = line.length();

                // Note - all of the following processing NOT done for
                // tabs ASIS

                if( tabs == ASIS )
                {
                    // Just copy the body of the line across
                    try
                    {
                        outWriter.write( lineString );
                    }
                    catch( IOException e )
                    {
                        throw new TaskException( "Error", e );
                    }// end of try-catch

                }
                else
                {// (tabs != ASIS)
                    int ptr;

                    while( ( ptr = line.getNext() ) < linelen )
                    {

                        switch( lines.getState() )
                        {

                            case NOTJAVA:
                                notInConstant( line, line.length(), outWriter );
                                break;
                            case IN_MULTI_COMMENT:
                                if( ( endComment =
                                    lineString.indexOf( "*/", line.getNext() )
                                    ) >= 0 )
                                {
                                    // End of multiLineComment on this line
                                    endComment += 2;// Include the end token
                                    lines.setState( LOOKING );
                                }
                                else
                                {
                                    endComment = linelen;
                                }

                                notInConstant( line, endComment, outWriter );
                                break;
                            case IN_SINGLE_COMMENT:
                                notInConstant( line, line.length(), outWriter );
                                lines.setState( LOOKING );
                                break;
                            case IN_CHAR_CONST:
                            case IN_STR_CONST:
                                // Got here from LOOKING by finding an opening "\'"
                                // next points to that quote character.
                                // Find the end of the constant.  Watch out for
                                // backslashes.  Literal tabs are left unchanged, and
                                // the column is adjusted accordingly.

                                int begin = line.getNext();
                                char terminator = ( lines.getState() == IN_STR_CONST
                                    ? '\"'
                                    : '\'' );
                                endOfCharConst( line, terminator );
                                while( line.getNext() < line.getLookahead() )
                                {
                                    if( line.getNextCharInc() == '\t' )
                                    {
                                        line.setColumn(
                                            line.getColumn() +
                                            tablength -
                                            line.getColumn() % tablength );
                                    }
                                    else
                                    {
                                        line.incColumn();
                                    }
                                }

                                // Now output the substring
                                try
                                {
                                    outWriter.write( line.substring( begin, line.getNext() ) );
                                }
                                catch( IOException e )
                                {
                                    throw new TaskException( "Error", e );
                                }

                                lines.setState( LOOKING );

                                break;

                            case LOOKING:
                                nextStateChange( line );
                                notInConstant( line, line.getLookahead(), outWriter );
                                break;
                        }// end of switch (state)

                    }// end of while (line.getNext() < linelen)

                }// end of else (tabs != ASIS)

                try
                {
                    outWriter.write( eolstr );
                }
                catch( IOException e )
                {
                    throw new TaskException( "Error", e );
                }// end of try-catch

            }// end of while (lines.hasNext())

            try
            {
                // Handle CTRLZ
                if( ctrlz == ASIS )
                {
                    outWriter.write( lines.getEofStr() );
                }
                else if( ctrlz == ADD )
                {
                    outWriter.write( CTRLZ );
                }
            }
            catch( IOException e )
            {
                throw new TaskException( "Error", e );
            }
            finally
            {
                try
                {
                    outWriter.close();
                }
                catch( IOException e )
                {
                    throw new TaskException( "Error", e );
                }
            }

            File destFile = new File( destD, file );

            try
            {
                lines.close();
                lines = null;
            }
            catch( IOException e )
            {
                throw new TaskException( "Unable to close source file " + srcFile );
            }

            if( destFile.exists() )
            {
                // Compare the destination with the temp file
                getLogger().debug( "destFile exists" );
                if( !FileUtils.contentEquals( destFile, tmpFile ) )
                {
                    getLogger().debug( destFile + " is being written" );
                    if( !destFile.delete() )
                    {
                        throw new TaskException( "Unable to delete "
                                                 + destFile );
                    }
                    if( !tmpFile.renameTo( destFile ) )
                    {
                        throw new TaskException(
                            "Failed to transform " + srcFile
                            + " to " + destFile
                            + ". Couldn't rename temporary file: "
                            + tmpFile );
                    }

                }
                else
                {// destination is equal to temp file
                    getLogger().debug( destFile + " is not written, as the contents are identical" );
                    if( !tmpFile.delete() )
                    {
                        throw new TaskException( "Unable to delete "
                                                 + tmpFile );
                    }
                }
            }
            else
            {// destFile does not exist - write the temp file
                ///XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
                getLogger().debug( "destFile does not exist" );
                if( !tmpFile.renameTo( destFile ) )
                {
                    throw new TaskException(
                        "Failed to transform " + srcFile
                        + " to " + destFile
                        + ". Couldn't rename temporary file: "
                        + tmpFile );
                }
            }

            tmpFile = null;

        }
        catch( IOException e )
        {
            throw new TaskException( "Error", e );
        }
        finally
        {
            try
            {
                if( lines != null )
                {
                    lines.close();
                }
            }
            catch( IOException io )
            {
                getLogger().error( "Error closing " + srcFile );
            }// end of catch

            if( tmpFile != null )
            {
                tmpFile.delete();
            }
        }// end of finally
    }

    /**
     * Enumerated attribute with the values "asis", "add" and "remove".
     *
     * @author RT
     */
    public static class AddAsisRemove extends EnumeratedAttribute
    {
        public String[] getValues()
        {
            return new String[]{"add", "asis", "remove"};
        }
    }

    /**
     * Enumerated attribute with the values "asis", "cr", "lf" and "crlf".
     *
     * @author RT
     */
    public static class CrLf extends EnumeratedAttribute
    {
        public String[] getValues()
        {
            return new String[]{"asis", "cr", "lf", "crlf"};
        }
    }

    class OneLiner
        implements Iterator
    {

        private int state = javafiles ? LOOKING : NOTJAVA;

        private StringBuffer eolStr = new StringBuffer( LINEBUFLEN );
        private StringBuffer eofStr = new StringBuffer();
        private StringBuffer line = new StringBuffer();
        private boolean reachedEof = false;

        private BufferedReader reader;

        public OneLiner( File srcFile )
            throws TaskException
        {
            try
            {
                reader = new BufferedReader
                    ( getReader( srcFile ), INBUFLEN );
                nextLine();
            }
            catch( IOException e )
            {
                throw new TaskException( "Error", e );
            }
        }

        public void remove()
        {
            throw new UnsupportedOperationException();
        }

        public void setState( int state )
        {
            this.state = state;
        }

        public String getEofStr()
        {
            return eofStr.toString();
        }

        public int getState()
        {
            return state;
        }

        public void close()
            throws IOException
        {
            if( reader != null )
            {
                reader.close();
            }
        }

        public boolean hasNext()
        {
            return !reachedEof;
        }

        public Object next()
            throws NoSuchElementException
        {
            if( !hasNext() )
            {
                throw new NoSuchElementException( "OneLiner" );
            }

            try
            {
                BufferLine tmpLine =
                    new BufferLine( line.toString(), eolStr.toString() );
                nextLine();
                return tmpLine;
            }
            catch( TaskException e )
            {
                throw new NoSuchElementException();
            }
        }

        protected void nextLine()
            throws TaskException
        {
            int ch = -1;
            int eolcount = 0;

            eolStr.setLength( 0 );
            line.setLength( 0 );

            try
            {
                ch = reader.read();
                while( ch != -1 && ch != '\r' && ch != '\n' )
                {
                    line.append( (char)ch );
                    ch = reader.read();
                }

                if( ch == -1 && line.length() == 0 )
                {
                    // Eof has been reached
                    reachedEof = true;
                    return;
                }

                switch( (char)ch )
                {
                    case '\r':
                        // Check for \r, \r\n and \r\r\n
                        // Regard \r\r not followed by \n as two lines
                        ++eolcount;
                        eolStr.append( '\r' );
                        switch( (char)( ch = reader.read() ) )
                        {
                            case '\r':
                                if( (char)( ch = reader.read() ) == '\n' )
                                {
                                    eolcount += 2;
                                    eolStr.append( "\r\n" );
                                }
                                break;
                            case '\n':
                                ++eolcount;
                                eolStr.append( '\n' );
                                break;
                        }// end of switch ((char)(ch = reader.read()))
                        break;
                    case '\n':
                        ++eolcount;
                        eolStr.append( '\n' );
                        break;
                }// end of switch ((char) ch)

                // if at eolcount == 0 and trailing characters of string
                // are CTRL-Zs, set eofStr
                if( eolcount == 0 )
                {
                    int i = line.length();
                    while( --i >= 0 && line.charAt( i ) == CTRLZ )
                    {
                        // keep searching for the first ^Z
                    }
                    if( i < line.length() - 1 )
                    {
                        // Trailing characters are ^Zs
                        // Construct new line and eofStr
                        eofStr.append( line.toString().substring( i + 1 ) );
                        if( i < 0 )
                        {
                            line.setLength( 0 );
                            reachedEof = true;
                        }
                        else
                        {
                            line.setLength( i + 1 );
                        }
                    }

                }// end of if (eolcount == 0)

            }
            catch( IOException e )
            {
                throw new TaskException( "Error", e );
            }
        }

        class BufferLine
        {
            private int next = 0;
            private int column = 0;
            private int lookahead = UNDEF;
            private String eolStr;
            private String line;

            public BufferLine( String line, String eolStr )
                throws TaskException
            {
                next = 0;
                column = 0;
                this.line = line;
                this.eolStr = eolStr;
            }

            public void setColumn( int col )
            {
                column = col;
            }

            public void setLookahead( int lookahead )
            {
                this.lookahead = lookahead;
            }

            public void setNext( int next )
            {
                this.next = next;
            }

            public void setState( int state )
            {
                OneLiner.this.setState( state );
            }

            public char getChar( int i )
            {
                return line.charAt( i );
            }

            public int getColumn()
            {
                return column;
            }

            public String getEol()
            {
                return eolStr;
            }

            public int getEolLength()
            {
                return eolStr.length();
            }

            public String getLineString()
            {
                return line;
            }

            public int getLookahead()
            {
                return lookahead;
            }

            public int getNext()
            {
                return next;
            }

            public char getNextChar()
            {
                return getChar( next );
            }

            public char getNextCharInc()
            {
                return getChar( next++ );
            }

            public int getState()
            {
                return OneLiner.this.getState();
            }

            public int incColumn()
            {
                return column++;
            }

            public int length()
            {
                return line.length();
            }

            public String substring( int begin )
            {
                return line.substring( begin );
            }

            public String substring( int begin, int end )
            {
                return line.substring( begin, end );
            }
        }
    }

}
