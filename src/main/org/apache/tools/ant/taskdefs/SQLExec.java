/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights 
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer. 
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:  
 *       "This product includes software developed by the 
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written 
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.*;

import java.io.*;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.zip.*;
import java.sql.*;

/**
 * Reads in a text file containing SQL statements seperated with semicolons
 * and executes it in a given db.
 * Both -- and // maybe used as comments.
 * 
 * @author <a href="mailto:jeff@custommonkey.org">Jeff Martin</a>
 */
public class SQLExec extends Task {

    /**
     * Database connection
     */
    private Connection conn = null;
    
    /**
     * Autocommit flag. Default value is false
     */
    private boolean autocommit=false;
    
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
     * Set the name of the sql file to be run.
     */
    public void setSrc(File srcFile) {
        this.srcFile = srcFile;
    }
    
    /**
     * Set the name of the sql file to be run.
     */
    public void addText(String sql) {
        this.sqlCommand += sql;
    }
    
    /**
     * Set the JDBC driver to be used.
     */
    public void setDriver(String driver) {
        this.driver = driver;
    }
    
    /**
     * Set the DB connection url.
     */
    public void setUrl(String url) {
        this.url = url;
    }
    
    /**
     * Set the user name for the DB connection.
     */
    public void setUserid(String userId) {
        this.userId = userId;
    }
    
    /**
     * Set the password for the DB connection.
     */
    public void setPassword(String password) {
        this.password = password;
    }
    
    /**
     * Set the autocommit flag for the DB connection.
     */
    public void setAutocommit(boolean autocommit) {
        this.autocommit = autocommit;
    }
     
    /**
     * Load the sql file and then execute it
     */
    public void execute() throws BuildException {

        sqlCommand = sqlCommand.trim();

        if (srcFile == null && sqlCommand.length() == 0) {
            throw new BuildException("Source file or sql statement must be set!", location);
        }
        if (driver == null) {
            throw new BuildException("Driver attribute must be set!", location);
        }
        if (userId == null) {
            throw new BuildException("User Id attribute must be set!", location);
        }
        if (password == null) {
            throw new BuildException("Password attribute must be set!", location);
        }
        if (url == null) {
            throw new BuildException("Url attribute must be set!", location);
        }
        if (srcFile != null && !srcFile.exists()) {
            throw new BuildException("Source file does not exist!", location);
        }

        try{
            Class.forName(driver);
        }catch(ClassNotFoundException e){
            throw new BuildException("JDBC driver " + driver + " could not be loaded", location);
        }

        try{
            log("connecting to " + url, Project.MSG_VERBOSE );
            conn = DriverManager.getConnection(url, userId, password);

            conn.setAutoCommit(autocommit);

            statement = conn.createStatement();

            if (sqlCommand.length() != 0) {
                runStatements(new StringReader(sqlCommand));
            }
            
            if (srcFile != null) {
                runStatements(new FileReader(srcFile));
            }

            if (!autocommit) {
                conn.commit();
            }
            
        } catch(IOException e){
            if (!autocommit) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {}
            }
            throw new BuildException(e, location);
        } catch(SQLException e){
            if (!autocommit) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {}
            }
            throw new BuildException(e, location);
        }
        finally {
            try {
                if (statement != null) {
                    statement.close();
                }
                if (conn != null) {
                    conn.close();
                }
            }
            catch (SQLException e) {}
        }
          
        log("SQL statements executed successfully", Project.MSG_VERBOSE);
    }

    protected void runStatements(Reader reader) throws SQLException, IOException {
        String sql = "";
        String line = "";
 
 	BufferedReader in = new BufferedReader(reader);
 
 	try{
            while ((line=in.readLine()) != null){
                if (line.trim().startsWith("//")) continue;
                if (line.trim().startsWith("--")) continue;
      
                sql += " " + line;
                sql = sql.trim();
                if (sql.endsWith(";")){
                    log("SQL: " + sql, Project.MSG_VERBOSE);
                    execSQL(sql.substring(0, sql.length()-1));
                    sql = "";
                }
            }
 
 	    // Catch any statements not followed by ;
 	    if(!sql.equals("")){
 	    	execSQL(sql);
 	    }
 	}catch(SQLException e){
            log("Failed to execute: " + sql, Project.MSG_ERR);
 	    throw e;
 	}
     }
 

    /**
     * Exec the sql statement.
     */
    protected void execSQL(String sql) throws SQLException{
        if (!statement.execute(sql)) {
            log(statement.getUpdateCount()+" rows affected", 
                Project.MSG_VERBOSE);
        }

        SQLWarning warning = conn.getWarnings();
        while(warning!=null){
            log(warning + " sql warning", Project.MSG_VERBOSE);
            warning=warning.getNextWarning();
        }
        conn.clearWarnings();
    }

}
