/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
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

import java.sql.Driver;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverPropertyInfo;
import java.util.Properties;
import java.io.File;
import java.net.URL;

import junit.framework.TestCase;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;

/**
 * Simple testcase to test for driver caching.
 * To test for your own database, you may need to tweak getProperties(int)
 * and add a couple of keys. see testOracle and testMySQL for an example.
 *
 * It would be much better to extend this testcase by using HSQL
 * as the test db, so that a db is really used.
 *
 * @author <a href="mailto:sbailliez@apache.org">Stephane Bailliez</a>
 */
public class SQLExecTest extends TestCase {

    // some database keys, see #getProperties(int)
    public final static int NULL = 0;
    public final static int ORACLE = 1;
    public final static int MYSQL = 2;

    // keys used in properties.
    public final static String DRIVER = "driver";
    public final static String USER = "user";
    public final static String PASSWORD = "password";
    public final static String URL = "url";
    public final static String PATH = "path";
    public final static String SQL = "sql";

    public SQLExecTest(String s) {
        super(s);
    }

    protected void setUp() throws Exception {
        // make sure the cache is cleared.
        SQLExec.getLoaderMap().clear();
    }

   // simple test to ensure that the caching does work...
    public void testDriverCaching(){
       SQLExec sql = createTask(getProperties(NULL));
        assertTrue(!SQLExec.getLoaderMap().containsKey(NULL_DRIVER));
        try {
            sql.execute();
        } catch (BuildException e){
            assertTrue(e.getException().getMessage().indexOf("No suitable Driver") != -1);
        }
        assertTrue(SQLExec.getLoaderMap().containsKey(NULL_DRIVER));
        assertSame(sql.getLoader(), SQLExec.getLoaderMap().get(NULL_DRIVER));
        ClassLoader loader1 = sql.getLoader();

        // 2nd run..
        sql = createTask(getProperties(NULL));
        // the driver must still be cached.
        assertTrue(sql.getLoaderMap().containsKey(NULL_DRIVER));
        try {
            sql.execute();
        } catch (BuildException e){
            assertTrue(e.getException().getMessage().indexOf("No suitable Driver") != -1);
        }
        assertTrue(sql.getLoaderMap().containsKey(NULL_DRIVER));
        assertSame(sql.getLoader(), sql.getLoaderMap().get(NULL_DRIVER));
        assertSame(loader1, sql.getLoader());
    }

    public void testNull() throws Exception {
        doMultipleCalls(1000, NULL, true, true);
    }

    /*
    public void testOracle(){
        doMultipleCalls(1000, ORACLE, true, false);
    }*/

    /*
    public void testMySQL(){
        doMultipleCalls(1000, MYSQL, true, false);
    }*/


    /**
     * run a sql tasks multiple times.
     * @param calls number of times to execute the task
     * @param database the database to execute on.
     * @param caching should caching be enabled ?
     * @param catchexception true to catch exception for each call, false if not.
     */
    protected void doMultipleCalls(int calls, int database, boolean caching, boolean catchexception){
        Properties props = getProperties(database);
        for (int i = 0; i < calls; i++){
            SQLExec sql = createTask(props);
            sql.setCaching(caching);
            try  {
                sql.execute();
            } catch (BuildException e){
                if (!catchexception){
                    throw e;
                }
            }
        }
    }

    /**
     * Create a task from a set of properties
     * @see #getProperties(int)
     */
    protected SQLExec createTask(Properties props){
        SQLExec sql = new SQLExec();
        sql.setProject( new Project() );
        sql.setDriver( props.getProperty(DRIVER) );
        sql.setUserid( props.getProperty(USER) );
        sql.setPassword( props.getProperty(PASSWORD) );
        sql.setUrl( props.getProperty(URL) );
        sql.createClasspath().setLocation( new File(props.getProperty(PATH)) );
        sql.addText( props.getProperty(SQL) );
        return sql;
    }

    /**
     * try to find the path from a resource (jar file or directory name)
     * so that it can be used as a classpath to load the resource.
     */
    protected String findResourcePath(String resource){
        resource = resource.replace('.', '/') + ".class";
        URL url = getClass().getClassLoader().getResource(resource);
        if (url == null) {
            return null;
        }
        String u = url.toString();
        if (u.startsWith("jar:file:")) {
            int pling = u.indexOf("!");
            return u.substring("jar:file:".length(), pling);
        } else if (u.startsWith("file:")) {
            int tail = u.indexOf(resource);
            return u.substring("file:".length(), tail);
        }
        return null;
    }

    /**
     * returns a configuration associated to a specific database.
     * If you want to test on your specific base, you'd better
     * tweak this to make it run or add your own database.
     * The driver lib should be dropped into the system classloader.
     */
    protected Properties getProperties(int database){
        Properties props = null;
        switch (database){
            case ORACLE:
                props = getProperties("oracle.jdbc.driver.OracleDriver", "test", "test", "jdbc:oracle:thin:@127.0.0.1:1521:orcl");
                break;
            case MYSQL:
                props = getProperties("org.gjt.mm.mysql.Driver", "test", "test", "jdbc:mysql://127.0.0.1:3306/test");
                break;
            case NULL:
            default:
                props = getProperties(NULL_DRIVER, "test", "test", "jdbc:database://hostname:port/name");
        }
        // look for the driver path...
        String path = findResourcePath(props.getProperty(DRIVER));
        props.put(PATH, path);
        props.put(SQL, "create table OOME_TEST(X INTEGER NOT NULL);\ndrop table if exists OOME_TEST;");
        return props;
    }

    /** helper method to build properties */
    protected Properties getProperties(String driver, String user, String pwd, String url){
        Properties props = new Properties();
        props.put(DRIVER, driver);
        props.put(USER, user);
        props.put(PASSWORD, pwd);
        props.put(URL, url);
        return props;
    }


//--- NULL JDBC driver just for simple test since there are no db driver
// available as a default in Ant :)

    public final static String NULL_DRIVER = NullDriver.class.getName();

    public static class NullDriver implements Driver {
        public Connection connect(String url, Properties info)
                throws SQLException {
            return null;
        }

        public boolean acceptsURL(String url) throws SQLException {
            return false;
        }

        public DriverPropertyInfo[] getPropertyInfo(String url, Properties info)
                throws SQLException {
            return new DriverPropertyInfo[0];
        }

        public int getMajorVersion() {
            return 0;
        }

        public int getMinorVersion() {
            return 0;
        }

        public boolean jdbcCompliant() {
            return false;
        }
    }

}
