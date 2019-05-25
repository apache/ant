/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.tools.ant.taskdefs;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.Appendable;
import org.apache.tools.ant.types.resources.FileProvider;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.types.resources.Union;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.KeepAliveOutputStream;
import org.apache.tools.ant.util.StringUtils;

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
        @Override
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
    private Union resources;

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
    private List<Transaction> transactions = new Vector<>();

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
     * Results Output Resource.
     */
    private Resource output = null;

    /**
     * Output encoding.
     */
    private String outputEncoding = null;

    /**
     * Action to perform if an error is found
     */
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
     * delimiters must match in case and whitespace is significant.
     * @since Ant 1.8.0
     */
    private boolean strictDelimiterMatching = true;

    /**
     * whether to show SQLWarnings as WARN messages.
     * @since Ant 1.8.0
     */
    private boolean showWarnings = false;

    /**
     * The column separator used when printing the results.
     *
     * <p>Defaults to ","</p>
     *
     * @since Ant 1.8.0
     */
    private String csvColumnSep = ",";

    /**
     * The character used to quote column values.
     *
     * <p>If set, columns that contain either the column separator or
     * the quote character itself will be surrounded by the quote
     * character.  The quote character itself will be doubled if it
     * appears inside of the column's value.</p>
     *
     * <p>If this value is not set (the default), no column values
     * will be quoted, not even if they contain the column
     * separator.</p>
     *
     * <p><b>Note:<b> BLOB values will never be quoted.</p>
     *
     * <p>Defaults to "not set"</p>
     *
     * @since Ant 1.8.0
     */
    private String csvQuoteChar = null;

    /**
     * Whether a warning is an error - in which case onError applies.
     * @since Ant 1.8.0
     */
    private boolean treatWarningsAsErrors = false;

    /**
     * The name of the property to set in the event of an error
     * @since Ant 1.8.0
     */
    private String errorProperty = null;

    /**
     * The name of the property to set in the event of a warning
     * @since Ant 1.8.0
     */
    private String warningProperty = null;

    /**
     * The name of the property that receives the number of rows
     * returned
     * @since Ant 1.8.0
     */
    private String rowCountProperty = null;

    /**
     * The name of the property to force the csv quote character
    */
    private boolean forceCsvQuoteChar = false;

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
        if (rc == null) {
            throw new BuildException("Cannot add null ResourceCollection");
        }
        synchronized (this) {
            if (resources == null) {
                resources = new Union();
            }
        }
        resources.add(rc);
    }

    /**
     * Add a SQL transaction to execute
     * @return a Transaction to be configured.
     */
    public Transaction createTransaction() {
        Transaction t = new Transaction();
        transactions.add(t);
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
        setOutput(new FileResource(getProject(), output));
    }

    /**
     * Set the output Resource;
     * optional, defaults to the Ant log.
     * @param output the output Resource to store results.
     * @since Ant 1.8
     */
    public void setOutput(Resource output) {
        this.output = output;
    }

    /**
     * The encoding to use when writing the result to a resource.
     * <p>Default's to the platform's default encoding</p>
     * @param outputEncoding the name of the encoding or null for the
     * platform's default encoding
     * @since Ant 1.9.4
     */
    public void setOutputEncoding(String outputEncoding) {
        this.outputEncoding = outputEncoding;
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
     * If false, delimiters will be searched for in a case-insensitive
     * manner (i.e. delimiter="go" matches "GO") and surrounding
     * whitespace will be ignored (delimiter="go" matches "GO ").
     *
     * @param b boolean
     * @since Ant 1.8.0
     */
    public void setStrictDelimiterMatching(boolean b) {
        strictDelimiterMatching = b;
    }

    /**
     * whether to show SQLWarnings as WARN messages.
     *
     * @param b boolean
     * @since Ant 1.8.0
     */
    public void setShowWarnings(boolean b) {
        showWarnings = b;
    }

    /**
     * Whether a warning is an error - in which case onError applies.
     *
     * @param b boolean
     * @since Ant 1.8.0
     */
    public void setTreatWarningsAsErrors(boolean b) {
        treatWarningsAsErrors =  b;
    }

    /**
     * The column separator used when printing the results.
     *
     * <p>Defaults to ","</p>
     *
     * @param s String
     * @since Ant 1.8.0
     */
    public void setCsvColumnSeparator(String s) {
        csvColumnSep = s;
    }

    /**
     * The character used to quote column values.
     *
     * <p>If set, columns that contain either the column separator or
     * the quote character itself will be surrounded by the quote
     * character.  The quote character itself will be doubled if it
     * appears inside of the column's value.</p>
     *
     * <p>If this value is not set (the default), no column values
     * will be quoted, not even if they contain the column
     * separator.</p>
     *
     * <p><b>Note:</b> BLOB values will never be quoted.</p>
     *
     * <p>Defaults to "not set"</p>
     *
     * @param s String
     * @since Ant 1.8.0
     */
    public void setCsvQuoteCharacter(String s) {
        if (s != null && s.length() > 1) {
            throw new BuildException(
                "The quote character must be a single character.");
        }
        csvQuoteChar = s;
    }

    /**
     * Property to set to "true" if a statement throws an error.
     *
     * @param errorProperty the name of the property to set in the
     * event of an error.
     * @since Ant 1.8.0
     */
    public void setErrorProperty(String errorProperty) {
        this.errorProperty = errorProperty;
    }

    /**
     * Property to set to "true" if a statement produces a warning.
     *
     * @param warningProperty the name of the property to set in the
     * event of a warning.
     * @since Ant 1.8.0
     */
    public void setWarningProperty(String warningProperty) {
        this.warningProperty = warningProperty;
    }

    /**
     * Sets a given property to the number of rows in the first
     * statement that returned a row count.
     * @param rowCountProperty String
     * @since Ant 1.8.0
     */
    public void setRowCountProperty(String rowCountProperty) {
        this.rowCountProperty = rowCountProperty;
    }

    /**
     * Force the csv quote character
     * @param forceCsvQuoteChar boolean
     */
    public void setForceCsvQuoteChar(boolean forceCsvQuoteChar) {
        this.forceCsvQuoteChar = forceCsvQuoteChar;
    }

    /**
     * Load the sql file and then execute it
     * @throws BuildException on error.
     */
    @Override
    public void execute() throws BuildException {
        List<Transaction> savedTransaction = new Vector<>(transactions);
        String savedSqlCommand = sqlCommand;

        sqlCommand = sqlCommand.trim();

        try {
            if (srcFile == null && sqlCommand.isEmpty() && resources == null) {
                if (transactions.isEmpty()) {
                    throw new BuildException(
                        "Source file or resource collection, transactions or sql statement must be set!",
                        getLocation());
                }
            }

            if (srcFile != null && !srcFile.isFile()) {
                throw new BuildException("Source file " + srcFile
                        + " is not a file!", getLocation());
            }

            if (resources != null) {
                // deal with the resources
                for (Resource r : resources) {
                    // Make a transaction for each resource
                    Transaction t = createTransaction();
                    t.setSrcResource(r);
                }
            }

            // Make a transaction group for the outer command
            Transaction t = createTransaction();
            t.setSrc(srcFile);
            t.addText(sqlCommand);

            if (getConnection() == null) {
                // not a valid rdbms
                return;
            }

            try {
                PrintStream out = KeepAliveOutputStream.wrapSystemOut();
                try {
                    if (output != null) {
                        log("Opening PrintStream to output Resource " + output, Project.MSG_VERBOSE);
                        OutputStream os = null;
                        FileProvider fp =
                            output.as(FileProvider.class);
                        if (fp != null) {
                            os = FileUtils.newOutputStream(fp.getFile().toPath(), append);
                        } else {
                            if (append) {
                                Appendable a =
                                    output.as(Appendable.class);
                                if (a != null) {
                                    os = a.getAppendOutputStream();
                                }
                            }
                            if (os == null) {
                                os = output.getOutputStream();
                                if (append) {
                                    log("Ignoring append=true for non-appendable"
                                        + " resource " + output,
                                        Project.MSG_WARN);
                                }
                            }
                        }
                        if (outputEncoding != null) {
                            out = new PrintStream(new BufferedOutputStream(os),
                                                  false, outputEncoding);
                        } else {
                            out = new PrintStream(new BufferedOutputStream(os));
                        }
                    }

                    // Process all transactions
                    for (Transaction txn : transactions) {
                        txn.runTransaction(out);
                        if (!isAutocommit()) {
                            log("Committing transaction", Project.MSG_VERBOSE);
                            getConnection().commit();
                        }
                    }
                } finally {
                    FileUtils.close(out);
                }
            } catch (IOException | SQLException e) {
                closeQuietly();
                setErrorProperty();
                if ("abort".equals(onError)) {
                    throw new BuildException(e, getLocation());
                }
            } finally {
                try {
                    FileUtils.close(getStatement());
                } catch (SQLException ex) {
                    // ignore
                }
                FileUtils.close(getConnection());
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

        BufferedReader in = new BufferedReader(reader);

        String line;
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
            if (!keepformat && line.contains("--")) {
                sql.append("\n");
            }
            int lastDelimPos = lastDelimiterPosition(sql, line);
            if (lastDelimPos > -1) {
                execSQL(sql.substring(0, lastDelimPos), out);
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
        if (sql.trim().isEmpty()) {
            return;
        }

        ResultSet resultSet = null;
        try {
            totalSql++;
            log("SQL: " + sql, Project.MSG_VERBOSE);

            boolean ret;
            int updateCount = 0, updateCountTotal = 0;

            ret = getStatement().execute(sql);
            updateCount = getStatement().getUpdateCount();
            do {
                if (updateCount != -1) {
                    updateCountTotal += updateCount;
                }
                if (ret) {
                    resultSet = getStatement().getResultSet();
                    printWarnings(resultSet.getWarnings(), false);
                    resultSet.clearWarnings();
                    if (print) {
                        printResults(resultSet, out);
                    }
                }
                ret = getStatement().getMoreResults();
                updateCount = getStatement().getUpdateCount();
            } while (ret || updateCount != -1);

            printWarnings(getStatement().getWarnings(), false);
            getStatement().clearWarnings();

            log(updateCountTotal + " rows affected", Project.MSG_VERBOSE);
            if (updateCountTotal != -1) {
                setRowCountProperty(updateCountTotal);
            }

            if (print && showtrailers) {
                out.println(updateCountTotal + " rows affected");
            }
            SQLWarning warning = getConnection().getWarnings();
            printWarnings(warning, true);
            getConnection().clearWarnings();
            goodSql++;
        } catch (SQLException e) {
            log("Failed to execute: " + sql, Project.MSG_ERR);
            setErrorProperty();
            if (!"abort".equals(onError)) {
                log(e.toString(), Project.MSG_ERR);
            }
            if (!"continue".equals(onError)) {
                throw e;
            }
        } finally {
            FileUtils.close(resultSet);
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
    @Deprecated
    protected void printResults(PrintStream out) throws SQLException {
        try (ResultSet rs = getStatement().getResultSet()) {
            printResults(rs, out);
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
                    out.print(maybeQuote(md.getColumnName(1)));
                    for (int col = 2; col <= columnCount; col++) {
                         out.print(csvColumnSep);
                         out.print(maybeQuote(md.getColumnName(col)));
                    }
                    out.println();
                }
                while (rs.next()) {
                    printValue(rs, 1, out);
                    for (int col = 2; col <= columnCount; col++) {
                        out.print(csvColumnSep);
                        printValue(rs, col, out);
                    }
                    out.println();
                    printWarnings(rs.getWarnings(), false);
                }
            }
        }
        out.println();
    }

    private void printValue(ResultSet rs, int col, PrintStream out)
            throws SQLException {
        if (rawBlobs && rs.getMetaData().getColumnType(col) == Types.BLOB) {
            Blob blob = rs.getBlob(col);
            if (blob != null) {
                new StreamPumper(rs.getBlob(col).getBinaryStream(), out).run();
            }
        } else {
            out.print(maybeQuote(rs.getString(col)));
        }
    }

    private String maybeQuote(String s) {
        if (csvQuoteChar == null || s == null
                || (!forceCsvQuoteChar && !s.contains(csvColumnSep) && !s.contains(csvQuoteChar))) {
            return s;
        }
        StringBuilder sb = new StringBuilder(csvQuoteChar);
        char q = csvQuoteChar.charAt(0);
        for (final char c : s.toCharArray()) {
            if (c == q) {
                sb.append(q);
            }
            sb.append(c);
        }
        return sb.append(csvQuoteChar).toString();
    }

    /*
     * Closes an unused connection after an error and doesn't rethrow
     * a possible SQLException
     * @since Ant 1.7
     */
    private void closeQuietly() {
        if (!isAutocommit() && getConnection() != null && "abort".equals(onError)) {
            try {
                getConnection().rollback();
            } catch (SQLException ex) {
                // ignore
            }
        }
    }

    /**
     * Caches the connection returned by the base class's getConnection method.
     *
     * <p>Subclasses that need to provide a different connection than
     * the base class would, should override this method but keep in
     * mind that this class expects to get the same connection
     * instance on consecutive calls.</p>
     *
     * <p>returns null if the connection does not connect to the
     * expected RDBMS.</p>
     */
    @Override
    protected Connection getConnection() {
        if (conn == null) {
            conn = super.getConnection();
            if (!isValidRdbms(conn)) {
                conn = null;
            }
        }
        return conn;
    }

    /**
     * Creates and configures a Statement instance which is then
     * cached for subsequent calls.
     *
     * <p>Subclasses that want to provide different Statement
     * instances, should override this method but keep in mind that
     * this class expects to get the same connection instance on
     * consecutive calls.</p>
     *
     * @return Statement
     * @throws SQLException if statement creation or processing fails
     */
    protected Statement getStatement() throws SQLException {
        if (statement == null) {
            statement = getConnection().createStatement();
            statement.setEscapeProcessing(escapeProcessing);
        }
        return statement;
    }

    /**
     * The action a task should perform on an error,
     * one of "continue", "stop" and "abort"
     */
    public static class OnError extends EnumeratedAttribute {
        /** @return the enumerated values */
        @Override
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
                throw new BuildException(
                    "only single argument resource collections are supported.");
            }
            setSrcResource(a.iterator().next());
        }

        private void runTransaction(PrintStream out)
            throws IOException, SQLException {
            if (!tSqlCommand.isEmpty()) {
                log("Executing commands", Project.MSG_INFO);
                runStatements(new StringReader(tSqlCommand), out);
            }

            if (tSrcResource != null) {
                log("Executing resource: " + tSrcResource.toString(),
                    Project.MSG_INFO);
                Charset charset = encoding == null ? Charset.defaultCharset()
                    : Charset.forName(encoding);
                try (Reader reader = new InputStreamReader(
                    tSrcResource.getInputStream(), charset)) {
                    runStatements(reader, out);
                }
            }
        }
    }

    public int lastDelimiterPosition(StringBuffer buf, String currentLine) {
        if (strictDelimiterMatching) {
            if ((delimiterType.equals(DelimiterType.NORMAL)
                    && StringUtils.endsWith(buf, delimiter))
                    || (delimiterType.equals(DelimiterType.ROW)
                    && currentLine.equals(delimiter))) {
                return buf.length() - delimiter.length();
            }
            // no match
            return -1;
        }
        String d = delimiter.trim().toLowerCase(Locale.ENGLISH);
        if (DelimiterType.NORMAL.equals(delimiterType)) {
            // still trying to avoid wasteful copying, see
            // StringUtils.endsWith
            int endIndex = delimiter.length() - 1;
            int bufferIndex = buf.length() - 1;
            while (bufferIndex >= 0 && Character.isWhitespace(buf.charAt(bufferIndex))) {
                --bufferIndex;
            }
            if (bufferIndex < endIndex) {
                return -1;
            }
            while (endIndex >= 0) {
                if (buf.substring(bufferIndex, bufferIndex + 1).toLowerCase(Locale.ENGLISH)
                        .charAt(0) != d.charAt(endIndex)) {
                    return -1;
                }
                bufferIndex--;
                endIndex--;
            }
            return bufferIndex + 1;
        }
        return currentLine.trim().toLowerCase(Locale.ENGLISH).equals(d)
            ? buf.length() - currentLine.length() : -1;
    }

    private void printWarnings(SQLWarning warning, boolean force)
        throws SQLException {
        SQLWarning initialWarning = warning;
        if (showWarnings || force) {
            while (warning != null) {
                log(warning + " sql warning",
                    showWarnings ? Project.MSG_WARN : Project.MSG_VERBOSE);
                warning = warning.getNextWarning();
            }
        }
        if (initialWarning != null) {
            setWarningProperty();
        }
        if (treatWarningsAsErrors && initialWarning != null) {
            throw initialWarning;
        }
    }

    protected final void setErrorProperty() {
        setProperty(errorProperty, "true");
    }

    protected final void setWarningProperty() {
        setProperty(warningProperty, "true");
    }

    protected final void setRowCountProperty(int rowCount) {
        setProperty(rowCountProperty, Integer.toString(rowCount));
    }

    private void setProperty(String name, String value) {
        if (name != null) {
            getProject().setNewProperty(name, value);
        }
    }
}
