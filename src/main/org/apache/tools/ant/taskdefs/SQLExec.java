/*
 * Copyright  2000-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.FileSet;

import java.io.File;
import java.io.PrintStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.StringReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

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
        public static final String NORMAL = "normal";
        public static final String ROW = "row";
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
    private Vector filesets = new Vector();

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
     * Set the name of the SQL file to be run.
     * Required unless statements are enclosed in the build file
     */
    public void setSrc(File srcFile) {
        this.srcFile = srcFile;
    }

    /**
     * Set an inline SQL command to execute.
     * NB: Properties are not expanded in this text.
     */
    public void addText(String sql) {
        this.sqlCommand += sql;
    }

    /**
     * Adds a set of files (nested fileset attribute).
     */
    public void addFileset(FileSet set) {
        filesets.addElement(set);
    }


    /**
     * Add a SQL transaction to execute
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
     */
    public void setDelimiterType(DelimiterType delimiterType) {
        this.delimiterType = delimiterType.getValue();
    }

    /**
     * Print result sets from the statements;
     * optional, default false
     */
    public void setPrint(boolean print) {
        this.print = print;
    }

    /**
     * Print headers for result sets from the
     * statements; optional, default true.
     */
    public void setShowheaders(boolean showheaders) {
        this.showheaders = showheaders;
    }

    /**
     * Set the output file;
     * optional, defaults to the Ant log.
     */
    public void setOutput(File output) {
        this.output = output;
    }

    /**
     * whether output should be appended to or overwrite
     * an existing file.  Defaults to false.
     *
     * @since Ant 1.5
     */
    public void setAppend(boolean append) {
        this.append = append;
    }


    /**
     * Action to perform when statement fails: continue, stop, or abort
     * optional; default &quot;abort&quot;
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
     *
     * @since Ant 1.6
     */
    public void setEscapeProcessing(boolean enable) {
        escapeProcessing = enable;
    }

    /**
     * Load the sql file and then execute it
     */
    public void execute() throws BuildException {
        Vector savedTransaction = (Vector) transactions.clone();
        String savedSqlCommand = sqlCommand;

        sqlCommand = sqlCommand.trim();

        try {
            if (srcFile == null && sqlCommand.length() == 0
                && filesets.isEmpty()) {
                if (transactions.size() == 0) {
                    throw new BuildException("Source file or fileset, "
                                             + "transactions or sql statement "
                                             + "must be set!", getLocation());
                }
            }

            if (srcFile != null && !srcFile.exists()) {
                throw new BuildException("Source file does not exist!", getLocation());
            }

            // deal with the filesets
            for (int i = 0; i < filesets.size(); i++) {
                FileSet fs = (FileSet) filesets.elementAt(i);
                DirectoryScanner ds = fs.getDirectoryScanner(getProject());
                File srcDir = fs.getDir(getProject());

                String[] srcFiles = ds.getIncludedFiles();

                // Make a transaction for each file
                for (int j = 0; j < srcFiles.length; j++) {
                    Transaction t = createTransaction();
                    t.setSrc(new File(srcDir, srcFiles[j]));
                }
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
                        log("Opening PrintStream to output file " + output,
                            Project.MSG_VERBOSE);
                        out = new PrintStream(
                                  new BufferedOutputStream(
                                      new FileOutputStream(output
                                                           .getAbsolutePath(),
                                                           append)));
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
                    if (out != null && out != System.out) {
                        out.close();
                    }
                }
            } catch (IOException e) {
                if (!isAutocommit() && conn != null && onError.equals("abort")) {
                    try {
                        conn.rollback();
                    } catch (SQLException ex) {
                        // ignore
                    }
                }
                throw new BuildException(e, getLocation());
            } catch (SQLException e) {
                if (!isAutocommit() && conn != null && onError.equals("abort")) {
                    try {
                        conn.rollback();
                    } catch (SQLException ex) {
                        // ignore
                    }
                }
                throw new BuildException(e, getLocation());
            } finally {
                try {
                    if (statement != null) {
                        statement.close();
                    }
                    if (conn != null) {
                        conn.close();
                    }
                } catch (SQLException ex) {
                    // ignore
                }
            }

            log(goodSql + " of " + totalSql
                + " SQL statements executed successfully");
        } finally {
            transactions = savedTransaction;
            sqlCommand = savedSqlCommand;
        }
    }

    /**
     * read in lines and execute them
     */
    protected void runStatements(Reader reader, PrintStream out)
        throws SQLException, IOException {
        StringBuffer sql = new StringBuffer();
        String line = "";

        BufferedReader in = new BufferedReader(reader);

        while ((line = in.readLine()) != null) {
            if (!keepformat) {
                line = line.trim();
            }
            line = getProject().replaceProperties(line);
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

            if (!keepformat) {
                sql.append(" " + line);
            } else {
                sql.append("\n" + line);
            }

            // SQL defines "--" as a comment to EOL
            // and in Oracle it may contain a hint
            // so we cannot just remove it, instead we must end it
            if (!keepformat) {
                if (line.indexOf("--") >= 0) {
                    sql.append("\n");
                }
            }
            if ((delimiterType.equals(DelimiterType.NORMAL)
                 && sql.toString().endsWith(delimiter))
                ||
                (delimiterType.equals(DelimiterType.ROW)
                 && line.equals(delimiter))) {
                execSQL(sql.substring(0, sql.length() - delimiter.length()),
                        out);
                sql.replace(0, sql.length(), "");
            }
        }
        // Catch any statements not followed by ;
        if (!sql.equals("")) {
            execSQL(sql.toString(), out);
        }
    }


    /**
     * Exec the sql statement.
     */
    protected void execSQL(String sql, PrintStream out) throws SQLException {
        // Check and ignore empty statements
        if ("".equals(sql.trim())) {
            return;
        }

        try {
            totalSql++;
            log("SQL: " + sql, Project.MSG_VERBOSE);

            boolean ret;
            int updateCount = 0, updateCountTotal = 0;
            ResultSet resultSet = null;

            ret = statement.execute(sql);
            updateCount = statement.getUpdateCount();
            resultSet = statement.getResultSet();
            do {
                if (!ret) {
                    if (updateCount != -1) {
                        updateCountTotal += updateCount;
                    }
                } else {
                    if (print) {
                        printResults(out);
                    }
                }
                ret = statement.getMoreResults();
                updateCount = statement.getUpdateCount();
                resultSet = statement.getResultSet();
            } while (ret);

            log(updateCountTotal + " rows affected",
                Project.MSG_VERBOSE);

            if (print) {
                StringBuffer line = new StringBuffer();
                line.append(updateCountTotal + " rows affected");
                out.println(line);
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
        }
    }

    /**
     * print any results in the statement.
     */
    protected void printResults(PrintStream out) throws java.sql.SQLException {
        ResultSet rs = null;
        rs = statement.getResultSet();
        if (rs != null) {
            log("Processing new result set.", Project.MSG_VERBOSE);
            ResultSetMetaData md = rs.getMetaData();
            int columnCount = md.getColumnCount();
            StringBuffer line = new StringBuffer();
            if (showheaders) {
                for (int col = 1; col < columnCount; col++) {
                     line.append(md.getColumnName(col));
                     line.append(",");
                }
                line.append(md.getColumnName(columnCount));
                out.println(line);
                line = new StringBuffer();
            }
            while (rs.next()) {
                boolean first = true;
                for (int col = 1; col <= columnCount; col++) {
                    String columnValue = rs.getString(col);
                    if (columnValue != null) {
                        columnValue = columnValue.trim();
                    }

                    if (first) {
                        first = false;
                    } else {
                        line.append(",");
                    }
                    line.append(columnValue);
                }
                out.println(line);
                line = new StringBuffer();
            }
        }
        out.println();
    }

    /**
     * The action a task should perform on an error,
     * one of "continue", "stop" and "abort"
     */
    public static class OnError extends EnumeratedAttribute {
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
        private File tSrcFile = null;
        private String tSqlCommand = "";

        /**
         *
         */
        public void setSrc(File src) {
            this.tSrcFile = src;
        }

        /**
         *
         */
        public void addText(String sql) {
            this.tSqlCommand += sql;
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

            if (tSrcFile != null) {
                log("Executing file: " + tSrcFile.getAbsolutePath(),
                    Project.MSG_INFO);
                Reader reader =
                    (encoding == null) ? new FileReader(tSrcFile)
                                       : new InputStreamReader(
                                             new FileInputStream(tSrcFile),
                                             encoding);
                try {
                    runStatements(reader, out);
                } finally {
                    reader.close();
                }
            }
        }
    }

}
