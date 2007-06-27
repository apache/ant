/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.StringUtils;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.types.resources.Union;

import java.io.File;
import java.io.PrintStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.StringReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Types;

/**
 * Executes a series of SQL statements on a database using JDBC.
 *
 * <p>Statements can
 * either be read in from a text file using the <i>src</i> attribute or from
 * between the enclosing SQL tags.</p>
 *
 * <p>Multiple statements can be provided, separated by semicolons (or the
 * defined <i>delimiter</i>). Individual lines within the statements can be
 * commented using either --, // or REM at the start of the line.</p>
 *
 * <p>The <i>autocommit</i> attribute specifies whether auto-commit should be
 * turned on or off whilst executing the statements. If auto-commit is turned
 * on each statement will be executed and committed. If it is turned off the
 * statements will all be executed as one transaction.</p>
 *
 * <p>The <i>onerror</i> attribute specifies how to proceed when an error occurs
 * during the execution of one of the statements.
 * The possible values are: <b>continue</b> execution, only show the error;
 * <b>stop</b> execution and commit transaction;
 * and <b>abort</b> execution and transaction and fail task.</p>
 *
 * @since Ant 1.2
 *
 * @ant.task name="sql" category="database"
 */
public class SQLExec extends JDBCTask {

    /**
     * delimiters we support, "normal" and "row"
     */
    public static class DelimiterType extends EnumeratedAttribute {
        /** The enumerated strings */
        public static final String NORMAL = "normal", ROW = "row";
        /** @return the enumerated strings */
        public String[] getValues() {
            return new String[] {NORMAL, ROW};
        }
    }

    private int goodSql = 0;

    private int totalSql = 0;

    /**
     * Database connection
     */
    private Connection conn = null;

    /**
     * files to load
     */
    private Union resources = new Union();

    /**
     * SQL statement
     */
    private Statement statement = null;

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
     * The delimiter type indicating whether the delimiter will
     * only be recognized on a line by itself
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
     * Print SQL stats (rows affected)
     */
    private boolean showtrailers = true;

    /**
     * Results Output file.
     */
    private File output = null;


    /**
     * Action to perform if an error is found
     **/
    private String onError = "abort";

    /**
     * Encoding to use when reading SQL statements from a file
     */
    private String encoding = null;

    /**
     * Append to an existing file or overwrite it?
     */
    private boolean append = false;

    /**
     * Keep the format of a sql block?
     */
    private boolean keepformat = false;

    /**
     * Argument to Statement.setEscapeProcessing
     *
     * @since Ant 1.6
     */
    private boolean escapeProcessing = true;

    /**
     * should properties be expanded in text?
     * false for backwards compatibility
     *
     * @since Ant 1.7
     */
    private boolean expandProperties = true;

    /**
     * should we print raw BLOB data?
     * @since Ant 1.7.1
     */
    private boolean rawBlobs;

    /**
     * Set the name of the SQL file to be run.
     * Required unless statements are enclosed in the build file
     * @param srcFile the file containing the SQL command.
     */
    public void setSrc(File srcFile) {
        this.srcFile = srcFile;
    }

    /**
     * Enable property expansion inside nested text
     *
     * @param expandProperties if true expand properties.
     * @since Ant 1.7
     */
    public void setExpandProperties(boolean expandProperties) {
        this.expandProperties = expandProperties;
    }

    /**
     * is property expansion inside inline text enabled?
     *
     * @return true if properties are to be expanded.
     * @since Ant 1.7
     */
    public boolean getExpandProperties() {
        return expandProperties;
    }

    /**
     * Set an inline SQL command to execute.
     * NB: Properties are not expanded in this text unless {@link #expandProperties}
     * is set.
     * @param sql an inline string containing the SQL command.
     */
    public void addText(String sql) {
        //there is no need to expand properties here as that happens when Transaction.addText is
        //called; to do so here would be an error.
        this.sqlCommand += sql;
    }

    /**
     * Adds a set of files (nested fileset attribute).
     * @param set a set of files contains SQL commands, each File is run in
     *            a separate transaction.
     */
    public void addFileset(FileSet set) {
        add(set);
    }

    /**
     * Adds a collection of resources (nested element).
     * @param rc a collection of resources containing SQL commands,
     * each resource is run in a separate transaction.
     * @since Ant 1.7
     */
    public void add(ResourceCollection rc) {
        resources.add(rc);
    }

    /**
     * Add a SQL transaction to execute
     * @return a Transaction to be configured.
     */
    public Transaction createTransaction() {
        Transaction t = new Transaction();
        transactions.addElement(t);
        return t;
    }

    /**
     * Set the file encoding to use on the SQL files read in
     *
     * @param encoding the encoding to use on the files
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * Set the delimiter that separates SQL statements. Defaults to &quot;;&quot;;
     * optional
     *
     * <p>For example, set this to "go" and delimitertype to "ROW" for
     * Sybase ASE or MS SQL Server.</p>
     * @param delimiter the separator.
     */
    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    /**
     * Set the delimiter type: "normal" or "row" (default "normal").
     *
     * <p>The delimiter type takes two values - normal and row. Normal
     * means that any occurrence of the delimiter terminate the SQL
     * command whereas with row, only a line containing just the
     * delimiter is recognized as the end of the command.</p>
     * @param delimiterType the type of delimiter - "normal" or "row".
     */
    public void setDelimiterType(DelimiterType delimiterType) {
        this.delimiterType = delimiterType.getValue();
    }

    /**
     * Print result sets from the statements;
     * optional, default false
     * @param print if true print result sets.
     */
    public void setPrint(boolean print) {
        this.print = print;
    }

    /**
     * Print headers for result sets from the
     * statements; optional, default true.
     * @param showheaders if true print headers of result sets.
     */
    public void setShowheaders(boolean showheaders) {
        this.showheaders = showheaders;
    }

    /**
     * Print trailing info (rows affected) for the SQL
     * Addresses Bug/Request #27446
     * @param showtrailers if true prints the SQL rows affected
     * @since Ant 1.7
     */
    public void setShowtrailers(boolean showtrailers) {
        this.showtrailers = showtrailers;
    }

    /**
     * Set the output file;
     * optional, defaults to the Ant log.
     * @param output the output file to use for logging messages.
     */
    public void setOutput(File output) {
        this.output = output;
    }

    /**
     * whether output should be appended to or overwrite
     * an existing file.  Defaults to false.
     *
     * @since Ant 1.5
     * @param append if true append to an existing file.
     */
    public void setAppend(boolean append) {
        this.append = append;
    }


    /**
     * Action to perform when statement fails: continue, stop, or abort
     * optional; default &quot;abort&quot;
     * @param action the action to perform on statement failure.
     */
    public void setOnerror(OnError action) {
        this.onError = action.getValue();
    }

    /**
     * whether or not format should be preserved.
     * Defaults to false.
     *
     * @param keepformat The keepformat to set
     */
    public void setKeepformat(boolean keepformat) {
        this.keepformat = keepformat;
    }

    /**
     * Set escape processing for statements.
     * @param enable if true enable escape processing, default is true.
     * @since Ant 1.6
     */
    public void setEscapeProcessing(boolean enable) {
        escapeProcessing = enable;
    }

    /**
     * Set whether to print raw BLOBs rather than their string (hex) representations.
     * @param rawBlobs whether to print raw BLOBs.
     * @since Ant 1.7.1
     */
    public void setRawBlobs(boolean rawBlobs) {
        this.rawBlobs = rawBlobs;
    }

    /**
     * Load the sql file and then execute it
     * @throws BuildException on error.
     */
    public void execute() throws BuildException {
        Vector savedTransaction = (Vector) transactions.clone();
        String savedSqlCommand = sqlCommand;

        sqlCommand = sqlCommand.trim();

        try {
            if (srcFile == null && sqlCommand.length() == 0
                && resources.size() == 0) {
                if (transactions.size() == 0) {
                    throw new BuildException("Source file or resource collection, "
                                             + "transactions or sql statement "
                                             + "must be set!", getLocation());
                }
            }

            if (srcFile != null && !srcFile.isFile()) {
                throw new BuildException("Source file " + srcFile
                        + " is not a file!", getLocation());
            }

            // deal with the resources
            Iterator iter = resources.iterator();
            while (iter.hasNext()) {
                Resource r = (Resource) iter.next();
                // Make a transaction for each resource
                Transaction t = createTransaction();
                t.setSrcResource(r);
            }

            // Make a transaction group for the outer command
            Transaction t = createTransaction();
            t.setSrc(srcFile);
            t.addText(sqlCommand);
            conn = getConnection();
            if (!isValidRdbms(conn)) {
                return;
            }
            try {
                statement = conn.createStatement();
                statement.setEscapeProcessing(escapeProcessing);

                PrintStream out = System.out;
                try {
                    if (output != null) {
                        log("Opening PrintStream to output file " + output, Project.MSG_VERBOSE);
                        out = new PrintStream(new BufferedOutputStream(
                                new FileOutputStream(output.getAbsolutePath(), append)));
                    }

                    // Process all transactions
                    for (Enumeration e = transactions.elements();
                         e.hasMoreElements();) {

                        ((Transaction) e.nextElement()).runTransaction(out);
                        if (!isAutocommit()) {
                            log("Committing transaction", Project.MSG_VERBOSE);
                            conn.commit();
                        }
                    }
                } finally {
                    FileUtils.close(out);
                }
            } catch (IOException e) {
                closeQuietly();
                throw new BuildException(e, getLocation());
            } catch (SQLException e) {
                closeQuietly();
                throw new BuildException(e, getLocation());
            } finally {
                try {
                    if (statement != null) {
                        statement.close();
                    }
                } catch (SQLException ex) {
                    // ignore
                }
                try {
                    if (conn != null) {
                        conn.close();
                    }
                } catch (SQLException ex) {
                    // ignore
                }
            }

            log(goodSql + " of " + totalSql + " SQL statements executed successfully");
        } finally {
            transactions = savedTransaction;
            sqlCommand = savedSqlCommand;
        }
    }

    /**
     * read in lines and execute them
     * @param reader the reader contains sql lines.
     * @param out the place to output results.
     * @throws SQLException on sql problems
     * @throws IOException on io problems
     */
    protected void runStatements(Reader reader, PrintStream out)
        throws SQLException, IOException {
        StringBuffer sql = new StringBuffer();
        String line;

        BufferedReader in = new BufferedReader(reader);

        while ((line = in.readLine()) != null) {
            if (!keepformat) {
                line = line.trim();
            }
            if (expandProperties) {
                line = getProject().replaceProperties(line);
            }
            if (!keepformat) {
                if (line.startsWith("//")) {
                    continue;
                }
                if (line.startsWith("--")) {
                    continue;
                }
                StringTokenizer st = new StringTokenizer(line);
                if (st.hasMoreTokens()) {
                    String token = st.nextToken();
                    if ("REM".equalsIgnoreCase(token)) {
                        continue;
                    }
                }
            }

            sql.append(keepformat ? "\n" : " ").append(line);

            // SQL defines "--" as a comment to EOL
            // and in Oracle it may contain a hint
            // so we cannot just remove it, instead we must end it
            if (!keepformat && line.indexOf("--") >= 0) {
                sql.append("\n");
            }
            if ((delimiterType.equals(DelimiterType.NORMAL) && StringUtils.endsWith(sql, delimiter))
                    || (delimiterType.equals(DelimiterType.ROW) && line.equals(delimiter))) {
                execSQL(sql.substring(0, sql.length() - delimiter.length()), out);
                sql.replace(0, sql.length(), "");
            }
        }
        // Catch any statements not followed by ;
        if (sql.length() > 0) {
            execSQL(sql.toString(), out);
        }
    }

    /**
     * Exec the sql statement.
     * @param sql the SQL statement to execute
     * @param out the place to put output
     * @throws SQLException on SQL problems
     */
    protected void execSQL(String sql, PrintStream out) throws SQLException {
        // Check and ignore empty statements
        if ("".equals(sql.trim())) {
            return;
        }

        ResultSet resultSet = null;
        try {
            totalSql++;
            log("SQL: " + sql, Project.MSG_VERBOSE);

            boolean ret;
            int updateCount = 0, updateCountTotal = 0;

            ret = statement.execute(sql);
            updateCount = statement.getUpdateCount();
            resultSet = statement.getResultSet();
            do {
                if (!ret) {
                    if (updateCount != -1) {
                        updateCountTotal += updateCount;
                    }
                } else if (print) {
                    printResults(resultSet, out);
                }
                ret = statement.getMoreResults();
                if (ret) {
                    updateCount = statement.getUpdateCount();
                    resultSet = statement.getResultSet();
                }
            } while (ret);

            log(updateCountTotal + " rows affected", Project.MSG_VERBOSE);

            if (print && showtrailers) {
                out.println(updateCountTotal + " rows affected");
            }
            SQLWarning warning = conn.getWarnings();
            while (warning != null) {
                log(warning + " sql warning", Project.MSG_VERBOSE);
                warning = warning.getNextWarning();
            }
            conn.clearWarnings();
            goodSql++;
        } catch (SQLException e) {
            log("Failed to execute: " + sql, Project.MSG_ERR);
            if (!onError.equals("continue")) {
                throw e;
            }
            log(e.toString(), Project.MSG_ERR);
        } finally {
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (SQLException e) {
                    //ignore
                }
            }
        }
    }

    /**
     * print any results in the statement
     * @deprecated since 1.6.x.
     *             Use {@link #printResults(java.sql.ResultSet, java.io.PrintStream)
     *             the two arg version} instead.
     * @param out the place to print results
     * @throws SQLException on SQL problems.
     */
    protected void printResults(PrintStream out) throws SQLException {
        ResultSet rs = statement.getResultSet();
        try {
            printResults(rs, out);
        } finally {
            if (rs != null) {
                rs.close();
            }
        }
    }

    /**
     * print any results in the result set.
     * @param rs the resultset to print information about
     * @param out the place to print results
     * @throws SQLException on SQL problems.
     * @since Ant 1.6.3
     */
    protected void printResults(ResultSet rs, PrintStream out) throws SQLException {
        if (rs != null) {
            log("Processing new result set.", Project.MSG_VERBOSE);
            ResultSetMetaData md = rs.getMetaData();
            int columnCount = md.getColumnCount();
            if (columnCount > 0) {
                if (showheaders) {
                    out.print(md.getColumnName(1));
                    for (int col = 2; col <= columnCount; col++) {
                         out.write(',');
                         out.print(md.getColumnName(col));
                    }
                    out.println();
                }
                while (rs.next()) {
                    printValue(rs, 1, out);
                    for (int col = 2; col <= columnCount; col++) {
                        out.write(',');
                        printValue(rs, col, out);
                    }
                    out.println();
                }
            }
        }
        out.println();
    }

    private void printValue(ResultSet rs, int col, PrintStream out)
            throws SQLException {
        if (rawBlobs && rs.getMetaData().getColumnType(col) == Types.BLOB) {
            new StreamPumper(rs.getBlob(col).getBinaryStream(), out).run();
        } else {
            out.print(rs.getString(col));
        }
    }

    /*
     * Closes an unused connection after an error and doesn't rethrow
     * a possible SQLException
     * @since Ant 1.7
     */
    private void closeQuietly() {
        if (!isAutocommit() && conn != null && onError.equals("abort")) {
            try {
                conn.rollback();
            } catch (SQLException ex) {
                // ignore
            }
        }
    }

    /**
     * The action a task should perform on an error,
     * one of "continue", "stop" and "abort"
     */
    public static class OnError extends EnumeratedAttribute {
        /** @return the enumerated values */
        public String[] getValues() {
            return new String[] {"continue", "stop", "abort"};
        }
    }

    /**
     * Contains the definition of a new transaction element.
     * Transactions allow several files or blocks of statements
     * to be executed using the same JDBC connection and commit
     * operation in between.
     */
    public class Transaction {
        private Resource tSrcResource = null;
        private String tSqlCommand = "";

        /**
         * Set the source file attribute.
         * @param src the source file
         */
        public void setSrc(File src) {
            //there are places (in this file, and perhaps elsewhere, where it is assumed
            //that null is an acceptable parameter.
            if (src != null) {
                setSrcResource(new FileResource(src));
            }
        }

        /**
         * Set the source resource attribute.
         * @param src the source file
         * @since Ant 1.7
         */
        public void setSrcResource(Resource src) {
            if (tSrcResource != null) {
                throw new BuildException("only one resource per transaction");
            }
            tSrcResource = src;
        }

        /**
         * Set inline text
         * @param sql the inline text
         */
        public void addText(String sql) {
            if (sql != null) {
                this.tSqlCommand += sql;
            }
        }

        /**
         * Set the source resource.
         * @param a the source resource collection.
         * @since Ant 1.7
         */
        public void addConfigured(ResourceCollection a) {
            if (a.size() != 1) {
                throw new BuildException("only single argument resource "
                                         + "collections are supported.");
            }
            setSrcResource((Resource) a.iterator().next());
        }

        /**
         *
         */
        private void runTransaction(PrintStream out)
            throws IOException, SQLException {
            if (tSqlCommand.length() != 0) {
                log("Executing commands", Project.MSG_INFO);
                runStatements(new StringReader(tSqlCommand), out);
            }

            if (tSrcResource != null) {
                log("Executing resource: " + tSrcResource.toString(),
                    Project.MSG_INFO);
                InputStream is = null;
                Reader reader = null;
                try {
                    is = tSrcResource.getInputStream();
                    reader = (encoding == null) ? new InputStreamReader(is)
                        : new InputStreamReader(is, encoding);
                    runStatements(reader, out);
                } finally {
                    FileUtils.close(is);
                    FileUtils.close(reader);
                }
            }
        }
    }
}
