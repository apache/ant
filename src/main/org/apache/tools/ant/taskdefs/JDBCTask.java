/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
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
 * 4. The names "Ant" and "Apache Software
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

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.Properties;

/**
 * Handles JDBC configuration needed by SQL type tasks.
 * <p>
 * The following example class prints the contents of the first column of each row in TableName.
 *</p>
 *<code><pre>
package examples;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.JDBCTask;

public class SQLExampleTask extends JDBCTask {	

    private String tableName;

    public void execute() throws BuildException {
        Connection conn = getConnection();
        Statement stmt=null;
        try {
            if (tableName == null ) {
                throw new BuildException("TableName must be specified",location);
            }             
            String sql = "SELECT * FROM "+tableName;
            stmt= conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                log(rs.getObject(1).toString());
            }
        } catch (SQLException e) {
        
        } finally {
            if (stmt != null) {
                try {stmt.close();}catch (SQLException ingore){}
            }
            if (conn != null) {
                try {conn.close();}catch (SQLException ingore){}
            }
        }
    }
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

}

 
</pre></code>


 * @author <a href="mailto:nick@chalko.com">Nick Chalko</a>
 * @author <a href="mailto:jeff@custommonkey.org">Jeff Martin</a>
 * @author <A href="mailto:gholam@xtra.co.nz">Michael McCallum</A>
 * @author <A href="mailto:tim.stephenson@sybase.com">Tim Stephenson</A>
 *
 * @since Ant 1.5
 *
 */

public abstract class JDBCTask extends Task {


    /**
     * Used for caching loaders / driver. This is to avoid
     * getting an OutOfMemoryError when calling this task
     * multiple times in a row.
     */
    private static Hashtable loaderMap = new Hashtable(3);

    private boolean caching = true;

    private Path classpath;

    private AntClassLoader loader;

    /**
     * Autocommit flag. Default value is false
     */
    private boolean autocommit = false;

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
     * RDBMS Product needed for this SQL.
     **/
    private String rdbms = null;

    /**
     * RDBMS Version needed for this SQL.
     **/
    private String version = null;

    /**
     * Sets the classpath for loading the driver.
     * @param classpath The classpath to set
     */
    public void setClasspath(Path classpath) {
        this.classpath = classpath;
    }

    /**
     * Caching loaders / driver. This is to avoid
     * getting an OutOfMemoryError when calling this task
     * multiple times in a row; default: true
     * @param enable
     */
    public void setCaching(boolean enable) {
        caching = enable;
    }

    /**
     * Add a path to the classpath for loading the driver.
     */
    public Path createClasspath() {
        if (this.classpath == null) {
            this.classpath = new Path(project);
        }
        return this.classpath.createPath();
    }

    /**
     * Set the classpath for loading the driver 
     * using the classpath reference.
     */
    public void setClasspathRef(Reference r) {
        createClasspath().setRefid(r);
    }

    /**
     * Class name of the JDBC driver; required.
     * @param driver The driver to set
     */
    public void setDriver(String driver) {
        this.driver = driver;
    }

    /**
     * Sets the database connection URL; required.
     * @param url The url to set
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Sets the password; required.
     * @param password The password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Auto commit flag for database connection;
     * optional, default false.
     * @param autocommit The autocommit to set
     */
    public void setAutocommit(boolean autocommit) {
        this.autocommit = autocommit;
    }

    /**
     * Execute task only if the lower case product name 
     * of the DB matches this
     * @param rdbms The rdbms to set
     */
    public void setRdbms(String rdbms) {
        this.rdbms = rdbms;
    }

    /**
     * Sets the version string, execute task only if 
     * rdbms version match; optional.
     * @param version The version to set
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Verify we are connected to the correct RDBMS
     */
    protected boolean isValidRdbms(Connection conn) {
        if (rdbms == null && version == null) {
            return true;
        }

        try {
            DatabaseMetaData dmd = conn.getMetaData();

            if (rdbms != null) {
                String theVendor = dmd.getDatabaseProductName().toLowerCase();

                log("RDBMS = " + theVendor, Project.MSG_VERBOSE);
                if (theVendor == null || theVendor.indexOf(rdbms) < 0) {
                    log("Not the required RDBMS: " + rdbms, Project.MSG_VERBOSE);
                    return false;
                }
            }

            if (version != null) {
                // XXX maybe better toLowerCase(Locale.US)
                String theVersion = dmd.getDatabaseProductVersion().toLowerCase();

                log("Version = " + theVersion, Project.MSG_VERBOSE);
                if (theVersion == null
                        || !(theVersion.startsWith(version) || theVersion.indexOf(" " + version) >= 0)) {
                    log("Not the required version: \"" + version + "\"", Project.MSG_VERBOSE);
                    return false;
                }
            }
        } catch (SQLException e) {
            // Could not get the required information
            log("Failed to obtain required RDBMS information", Project.MSG_ERR);
            return false;
        }

        return true;
    }

    protected static Hashtable getLoaderMap() {
        return loaderMap;
    }

    protected AntClassLoader getLoader() {
        return loader;
    }

    /**
     * Creates a new Connection as using the driver, url, userid and password specified.
     * The calling method is responsible for closing the connection.
     * @return Connection the newly created connection.
     * @throws BuildException if the UserId/Password/Url is not set or there is no suitable driver or the driver fails to load.
     */
    protected Connection getConnection() throws BuildException {
        if (userId == null) {
            throw new BuildException("User Id attribute must be set!", location);
        }
        if (password == null) {
            throw new BuildException("Password attribute must be set!", location);
        }
        if (url == null) {
            throw new BuildException("Url attribute must be set!", location);
        }
        try {

            log("connecting to " + getUrl(), Project.MSG_VERBOSE);
            Properties info = new Properties();
            info.put("user", getUserId());
            info.put("password", getPassword());
            Connection conn = getDriver().connect(getUrl(), info);

            if (conn == null) {
                // Driver doesn't understand the URL
                throw new SQLException("No suitable Driver for " + url);
            }

            conn.setAutoCommit(autocommit);
            return conn;
        } catch (SQLException e) {
            throw new BuildException(e, location);
        }

    }

    /**
     * Gets an instance of the required driver.
     * Uses the ant class loader and the optionally the provided classpath.
     * @return Driver
     * @throws BuildException
     */
    private Driver getDriver() throws BuildException {
        if (driver == null) {
            throw new BuildException("Driver attribute must be set!", location);
        }

        Driver driverInstance = null;
        try {
            Class dc;
            if (classpath != null) {
                // check first that it is not already loaded otherwise
                // consecutive runs seems to end into an OutOfMemoryError
                // or it fails when there is a native library to load
                // several times.
                // this is far from being perfect but should work
                // in most cases.
                synchronized (loaderMap) {
                    if (caching) {
                        loader = (AntClassLoader) loaderMap.get(driver);
                    }
                    if (loader == null) {
                        log(
                                "Loading " + driver + " using AntClassLoader with classpath " + classpath,
                                Project.MSG_VERBOSE);
                        loader = new AntClassLoader(project, classpath);
                        if (caching) {
                            loaderMap.put(driver, loader);
                        }
                    } else {
                        log(
                                "Loading " + driver + " using a cached AntClassLoader.",
                                Project.MSG_VERBOSE);
                    }
                }
                dc = loader.loadClass(driver);
            } else {
                log("Loading " + driver + " using system loader.", Project.MSG_VERBOSE);
                dc = Class.forName(driver);
            }
            driverInstance = (Driver) dc.newInstance();
        } catch (ClassNotFoundException e) {
            throw new BuildException(
                    "Class Not Found: JDBC driver " + driver + " could not be loaded",
                    location);
        } catch (IllegalAccessException e) {
            throw new BuildException(
                    "Illegal Access: JDBC driver " + driver + " could not be loaded",
                    location);
        } catch (InstantiationException e) {
            throw new BuildException(
                    "Instantiation Exception: JDBC driver " + driver + " could not be loaded",
                    location);
        }
        return driverInstance;
    }


    public void isCaching(boolean value) {
        caching = value;
    }

    /**
     * Gets the classpath.
     * @return Returns a Path
     */
    public Path getClasspath() {
        return classpath;
    }

    /**
     * Gets the autocommit.
     * @return Returns a boolean
     */
    public boolean isAutocommit() {
        return autocommit;
    }

    /**
     * Gets the url.
     * @return Returns a String
     */
    public String getUrl() {
        return url;
    }

    /**
     * Gets the userId.
     * @return Returns a String
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Set the user name for the connection; required.
     * @param userId The userId to set
     */
    public void setUserid(String userId) {
        this.userId = userId;
    }

    /**
     * Gets the password.
     * @return Returns a String
     */
    public String getPassword() {
        return password;
    }

    /**
     * Gets the rdbms.
     * @return Returns a String
     */
    public String getRdbms() {
        return rdbms;
    }

    /**
     * Gets the version.
     * @return Returns a String
     */
    public String getVersion() {
        return version;
    }

}
