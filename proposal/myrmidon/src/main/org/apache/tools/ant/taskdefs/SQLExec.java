/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;

/**
 * Reads in a text file containing SQL statements seperated with semicolons and
 * executes it in a given db. Comments may be created with REM -- or //.
 *
 * @author <a href="mailto:jeff@custommonkey.org">Jeff Martin</a>
 * @author <A href="mailto:gholam@xtra.co.nz">Michael McCallum</A>
 * @author <A href="mailto:tim.stephenson@sybase.com">Tim Stephenson</A>
 */
public class SQLExec extends Task
{

    private int goodSql = 0, totalSql = 0;

    private Vector filesets = new Vector();

    /**
     * Database connection
     */
    private Connection conn = null;

    /**
     * Autocommit flag. Default value is false
     */
    private boolean autocommit = false;

    /**
     * SQL statement
     */
    private Statement statement = null;

    /**
     * DB driver.
     */
    private String driver = null;

    /**
     * DB url.
     */
    private String url = null;

    /**
     * User name.
     */
    private String userId = null;

    /**
     * Password
     */
    private String password = null;

    /**
     * SQL input file
     */
    private File srcFile = null;

    /**
     * SQL input command
     */
    private String sqlCommand = "";

    /**
     * SQL transactions to perform
     */
    private Vector transactions = new Vector();

    /**
     * SQL Statement delimiter
     */
    private String delimiter = ";";

    /**
     * The delimiter type indicating whether the delimiter will only be
     * recognized on a line by itself
     */
    private String delimiterType = DelimiterType.NORMAL;

    /**
     * Print SQL results.
     */
    private boolean print = false;

    /**
     * Print header columns.
     */
    private boolean showheaders = true;

    /**
     * Results Output file.
     */
    private File output = null;

    /**
     * RDBMS Product needed for this SQL.
     */
    private String rdbms = null;

    /**
     * RDBMS Version needed for this SQL.
     */
    private String version = null;

    /**
     * Action to perform if an error is found
     */
    private String onError = "abort";

    /**
     * Encoding to use when reading SQL statements from a file
     */
    private String encoding = null;

    private Path classpath;

    private AntClassLoader loader;

    /**
     * Set the autocommit flag for the DB connection.
     *
     * @param autocommit The new Autocommit value
     */
    public void setAutocommit( boolean autocommit )
    {
        this.autocommit = autocommit;
    }

    /**
     * Set the classpath for loading the driver.
     *
     * @param classpath The new Classpath value
     */
    public void setClasspath( Path classpath )
    {
        if( this.classpath == null )
        {
            this.classpath = classpath;
        }
        else
        {
            this.classpath.append( classpath );
        }
    }

    /**
     * Set the classpath for loading the driver using the classpath reference.
     *
     * @param r The new ClasspathRef value
     */
    public void setClasspathRef( Reference r )
    {
        createClasspath().setRefid( r );
    }

    /**
     * Set the statement delimiter. <p>
     *
     * For example, set this to "go" and delimitertype to "ROW" for Sybase ASE
     * or MS SQL Server.</p>
     *
     * @param delimiter The new Delimiter value
     */
    public void setDelimiter( String delimiter )
    {
        this.delimiter = delimiter;
    }

    /**
     * Set the Delimiter type for this sql task. The delimiter type takes two
     * values - normal and row. Normal means that any occurence of the delimiter
     * terminate the SQL command whereas with row, only a line containing just
     * the delimiter is recognized as the end of the command.
     *
     * @param delimiterType The new DelimiterType value
     */
    public void setDelimiterType( DelimiterType delimiterType )
    {
        this.delimiterType = delimiterType.getValue();
    }

    /**
     * Set the JDBC driver to be used.
     *
     * @param driver The new Driver value
     */
    public void setDriver( String driver )
    {
        this.driver = driver;
    }

    /**
     * Set the file encoding to use on the sql files read in
     *
     * @param encoding the encoding to use on the files
     */
    public void setEncoding( String encoding )
    {
        this.encoding = encoding;
    }

    /**
     * Set the action to perform onerror
     *
     * @param action The new Onerror value
     */
    public void setOnerror( OnError action )
    {
        this.onError = action.getValue();
    }

    /**
     * Set the output file.
     *
     * @param output The new Output value
     */
    public void setOutput( File output )
    {
        this.output = output;
    }


    /**
     * Set the password for the DB connection.
     *
     * @param password The new Password value
     */
    public void setPassword( String password )
    {
        this.password = password;
    }

    /**
     * Set the print flag.
     *
     * @param print The new Print value
     */
    public void setPrint( boolean print )
    {
        this.print = print;
    }

    /**
     * Set the rdbms required
     *
     * @param vendor The new Rdbms value
     */
    public void setRdbms( String vendor )
    {
        this.rdbms = vendor.toLowerCase();
    }

    /**
     * Set the showheaders flag.
     *
     * @param showheaders The new Showheaders value
     */
    public void setShowheaders( boolean showheaders )
    {
        this.showheaders = showheaders;
    }

    /**
     * Set the name of the sql file to be run.
     *
     * @param srcFile The new Src value
     */
    public void setSrc( File srcFile )
    {
        this.srcFile = srcFile;
    }

    /**
     * Set the DB connection url.
     *
     * @param url The new Url value
     */
    public void setUrl( String url )
    {
        this.url = url;
    }

    /**
     * Set the user name for the DB connection.
     *
     * @param userId The new Userid value
     */
    public void setUserid( String userId )
    {
        this.userId = userId;
    }

    /**
     * Set the version required
     *
     * @param version The new Version value
     */
    public void setVersion( String version )
    {
        this.version = version.toLowerCase();
    }

    /**
     * Adds a set of files (nested fileset attribute).
     *
     * @param set The feature to be added to the Fileset attribute
     */
    public void addFileset( FileSet set )
    {
        filesets.addElement( set );
    }

    /**
     * Set the sql command to execute
     *
     * @param sql The feature to be added to the Text attribute
     */
    public void addText( String sql )
    {
        this.sqlCommand += sql;
    }

    /**
     * Create the classpath for loading the driver.
     *
     * @return Description of the Returned Value
     */
    public Path createClasspath()
    {
        if( this.classpath == null )
        {
            this.classpath = new Path( project );
        }
        return this.classpath.createPath();
    }


    /**
     * Set the sql command to execute
     *
     * @return Description of the Returned Value
     */
    public Transaction createTransaction()
    {
        Transaction t = new Transaction();
        transactions.addElement( t );
        return t;
    }

    /**
     * Load the sql file and then execute it
     *
     * @exception BuildException Description of Exception
     */
    public void execute()
        throws BuildException
    {
        sqlCommand = sqlCommand.trim();

        if( srcFile == null && sqlCommand.length() == 0 && filesets.isEmpty() )
        {
            if( transactions.size() == 0 )
            {
                throw new BuildException( "Source file or fileset, transactions or sql statement must be set!", location );
            }
        }
        else
        {
            // deal with the filesets
            for( int i = 0; i < filesets.size(); i++ )
            {
                FileSet fs = ( FileSet )filesets.elementAt( i );
                DirectoryScanner ds = fs.getDirectoryScanner( project );
                File srcDir = fs.getDir( project );

                String[] srcFiles = ds.getIncludedFiles();

                // Make a transaction for each file
                for( int j = 0; j < srcFiles.length; j++ )
                {
                    Transaction t = createTransaction();
                    t.setSrc( new File( srcDir, srcFiles[j] ) );
                }
            }

            // Make a transaction group for the outer command
            Transaction t = createTransaction();
            t.setSrc( srcFile );
            t.addText( sqlCommand );
        }

        if( driver == null )
        {
            throw new BuildException( "Driver attribute must be set!", location );
        }
        if( userId == null )
        {
            throw new BuildException( "User Id attribute must be set!", location );
        }
        if( password == null )
        {
            throw new BuildException( "Password attribute must be set!", location );
        }
        if( url == null )
        {
            throw new BuildException( "Url attribute must be set!", location );
        }
        if( srcFile != null && !srcFile.exists() )
        {
            throw new BuildException( "Source file does not exist!", location );
        }
        Driver driverInstance = null;
        // Load the driver using the
        try
        {
            Class dc;
            if( classpath != null )
            {
                log( "Loading " + driver + " using AntClassLoader with classpath " + classpath,
                    Project.MSG_VERBOSE );

                loader = new AntClassLoader( project, classpath );
                dc = loader.loadClass( driver );
            }
            else
            {
                log( "Loading " + driver + " using system loader.", Project.MSG_VERBOSE );
                dc = Class.forName( driver );
            }
            driverInstance = ( Driver )dc.newInstance();
        }
        catch( ClassNotFoundException e )
        {
            throw new BuildException( "Class Not Found: JDBC driver " + driver + " could not be loaded", location );
        }
        catch( IllegalAccessException e )
        {
            throw new BuildException( "Illegal Access: JDBC driver " + driver + " could not be loaded", location );
        }
        catch( InstantiationException e )
        {
            throw new BuildException( "Instantiation Exception: JDBC driver " + driver + " could not be loaded", location );
        }

        try
        {
            log( "connecting to " + url, Project.MSG_VERBOSE );
            Properties info = new Properties();
            info.put( "user", userId );
            info.put( "password", password );
            conn = driverInstance.connect( url, info );

            if( conn == null )
            {
                // Driver doesn't understand the URL
                throw new SQLException( "No suitable Driver for " + url );
            }

            if( !isValidRdbms( conn ) )
                return;

            conn.setAutoCommit( autocommit );

            statement = conn.createStatement();

            PrintStream out = System.out;
            try
            {
                if( output != null )
                {
                    log( "Opening PrintStream to output file " + output, Project.MSG_VERBOSE );
                    out = new PrintStream( new BufferedOutputStream( new FileOutputStream( output ) ) );
                }

                // Process all transactions
                for( Enumeration e = transactions.elements();
                    e.hasMoreElements();  )
                {

                    ( ( Transaction )e.nextElement() ).runTransaction( out );
                    if( !autocommit )
                    {
                        log( "Commiting transaction", Project.MSG_VERBOSE );
                        conn.commit();
                    }
                }
            }
            finally
            {
                if( out != null && out != System.out )
                {
                    out.close();
                }
            }
        }
        catch( IOException e )
        {
            if( !autocommit && conn != null && onError.equals( "abort" ) )
            {
                try
                {
                    conn.rollback();
                }
                catch( SQLException ex )
                {}
            }
            throw new BuildException( e );
        }
        catch( SQLException e )
        {
            if( !autocommit && conn != null && onError.equals( "abort" ) )
            {
                try
                {
                    conn.rollback();
                }
                catch( SQLException ex )
                {}
            }
            throw new BuildException( e );
        }
        finally
        {
            try
            {
                if( statement != null )
                {
                    statement.close();
                }
                if( conn != null )
                {
                    conn.close();
                }
            }
            catch( SQLException e )
            {}
        }

        log( goodSql + " of " + totalSql +
            " SQL statements executed successfully" );
    }

    /**
     * Verify if connected to the correct RDBMS
     *
     * @param conn Description of Parameter
     * @return The ValidRdbms value
     */
    protected boolean isValidRdbms( Connection conn )
    {
        if( rdbms == null && version == null )
            return true;

        try
        {
            DatabaseMetaData dmd = conn.getMetaData();

            if( rdbms != null )
            {
                String theVendor = dmd.getDatabaseProductName().toLowerCase();

                log( "RDBMS = " + theVendor, Project.MSG_VERBOSE );
                if( theVendor == null || theVendor.indexOf( rdbms ) < 0 )
                {
                    log( "Not the required RDBMS: " + rdbms, Project.MSG_VERBOSE );
                    return false;
                }
            }

            if( version != null )
            {
                String theVersion = dmd.getDatabaseProductVersion().toLowerCase();

                log( "Version = " + theVersion, Project.MSG_VERBOSE );
                if( theVersion == null ||
                    !( theVersion.startsWith( version ) ||
                    theVersion.indexOf( " " + version ) >= 0 ) )
                {
                    log( "Not the required version: \"" + version + "\"", Project.MSG_VERBOSE );
                    return false;
                }
            }
        }
        catch( SQLException e )
        {
            // Could not get the required information
            log( "Failed to obtain required RDBMS information", Project.MSG_ERR );
            return false;
        }

        return true;
    }

    /**
     * Exec the sql statement.
     *
     * @param sql Description of Parameter
     * @param out Description of Parameter
     * @exception SQLException Description of Exception
     */
    protected void execSQL( String sql, PrintStream out )
        throws SQLException
    {
        // Check and ignore empty statements
        if( "".equals( sql.trim() ) )
            return;

        try
        {
            totalSql++;
            if( !statement.execute( sql ) )
            {
                log( statement.getUpdateCount() + " rows affected",
                    Project.MSG_VERBOSE );
            }
            else
            {
                if( print )
                {
                    printResults( out );
                }
            }

            SQLWarning warning = conn.getWarnings();
            while( warning != null )
            {
                log( warning + " sql warning", Project.MSG_VERBOSE );
                warning = warning.getNextWarning();
            }
            conn.clearWarnings();
            goodSql++;
        }
        catch( SQLException e )
        {
            log( "Failed to execute: " + sql, Project.MSG_ERR );
            if( !onError.equals( "continue" ) )
                throw e;
            log( e.toString(), Project.MSG_ERR );
        }
    }

    /**
     * print any results in the statement.
     *
     * @param out Description of Parameter
     * @exception java.sql.SQLException Description of Exception
     */
    protected void printResults( PrintStream out )
        throws java.sql.SQLException
    {
        ResultSet rs = null;
        do
        {
            rs = statement.getResultSet();
            if( rs != null )
            {
                log( "Processing new result set.", Project.MSG_VERBOSE );
                ResultSetMetaData md = rs.getMetaData();
                int columnCount = md.getColumnCount();
                StringBuffer line = new StringBuffer();
                if( showheaders )
                {
                    for( int col = 1; col < columnCount; col++ )
                    {
                        line.append( md.getColumnName( col ) );
                        line.append( "," );
                    }
                    line.append( md.getColumnName( columnCount ) );
                    out.println( line );
                    line.setLength( 0 );
                }
                while( rs.next() )
                {
                    boolean first = true;
                    for( int col = 1; col <= columnCount; col++ )
                    {
                        String columnValue = rs.getString( col );
                        if( columnValue != null )
                        {
                            columnValue = columnValue.trim();
                        }

                        if( first )
                        {
                            first = false;
                        }
                        else
                        {
                            line.append( "," );
                        }
                        line.append( columnValue );
                    }
                    out.println( line );
                    line.setLength( 0 );
                }
            }
        }while ( statement.getMoreResults() );
        out.println();
    }

    protected void runStatements( Reader reader, PrintStream out )
        throws SQLException, IOException
    {
        String sql = "";
        String line = "";

        BufferedReader in = new BufferedReader( reader );

        try
        {
            while( ( line = in.readLine() ) != null )
            {
                line = line.trim();
                line = project.replaceProperties( line );
                if( line.startsWith( "//" ) )
                    continue;
                if( line.startsWith( "--" ) )
                    continue;
                StringTokenizer st = new StringTokenizer( line );
                if( st.hasMoreTokens() )
                {
                    String token = st.nextToken();
                    if( "REM".equalsIgnoreCase( token ) )
                    {
                        continue;
                    }
                }

                sql += " " + line;
                sql = sql.trim();

                // SQL defines "--" as a comment to EOL
                // and in Oracle it may contain a hint
                // so we cannot just remove it, instead we must end it
                if( line.indexOf( "--" ) >= 0 )
                    sql += "\n";

                if( delimiterType.equals( DelimiterType.NORMAL ) && sql.endsWith( delimiter ) ||
                    delimiterType.equals( DelimiterType.ROW ) && line.equals( delimiter ) )
                {
                    log( "SQL: " + sql, Project.MSG_VERBOSE );
                    execSQL( sql.substring( 0, sql.length() - delimiter.length() ), out );
                    sql = "";
                }
            }

            // Catch any statements not followed by ;
            if( !sql.equals( "" ) )
            {
                execSQL( sql, out );
            }
        }
        catch( SQLException e )
        {
            throw e;
        }

    }

    public static class DelimiterType extends EnumeratedAttribute
    {
        public final static String NORMAL = "normal";
        public final static String ROW = "row";

        public String[] getValues()
        {
            return new String[]{NORMAL, ROW};
        }
    }

    /**
     * Enumerated attribute with the values "continue", "stop" and "abort" for
     * the onerror attribute.
     *
     * @author RT
     */
    public static class OnError extends EnumeratedAttribute
    {
        public String[] getValues()
        {
            return new String[]{"continue", "stop", "abort"};
        }
    }

    /**
     * Contains the definition of a new transaction element. Transactions allow
     * several files or blocks of statements to be executed using the same JDBC
     * connection and commit operation in between.
     *
     * @author RT
     */
    public class Transaction
    {
        private File tSrcFile = null;
        private String tSqlCommand = "";

        public void setSrc( File src )
        {
            this.tSrcFile = src;
        }

        public void addText( String sql )
        {
            this.tSqlCommand += sql;
        }

        private void runTransaction( PrintStream out )
            throws IOException, SQLException
        {
            if( tSqlCommand.length() != 0 )
            {
                log( "Executing commands", Project.MSG_INFO );
                runStatements( new StringReader( tSqlCommand ), out );
            }

            if( tSrcFile != null )
            {
                log( "Executing file: " + tSrcFile.getAbsolutePath(),
                    Project.MSG_INFO );
                Reader reader = ( encoding == null ) ? new FileReader( tSrcFile )
                     : new InputStreamReader( new FileInputStream( tSrcFile ), encoding );
                runStatements( reader, out );
                reader.close();
            }
        }
    }

}
