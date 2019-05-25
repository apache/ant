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

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.io.File;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;

/**
 * Simple testcase to test for driver caching.
 * To test for your own database, you may need to tweak getProperties(int)
 * and add a couple of keys. see testOracle and testMySQL for an example.
 *
 * It would be much better to extend this testcase by using HSQL
 * as the test db, so that a db is really used.
 *
 */
public class SQLExecTest {

    // some database keys, see #getProperties(int)
    public static final int NULL = 0;
    public static final int ORACLE = 1;
    public static final int MYSQL = 2;

    // keys used in properties.
    public static final String DRIVER = "driver";
    public static final String USER = "user";
    public static final String PASSWORD = "password";
    public static final String URL = "url";
    public static final String PATH = "path";
    public static final String SQL = "sql";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        // make sure the cache is cleared.
        JDBCTask.getLoaderMap().clear();
    }

   // simple test to ensure that the caching does work...
    @Test
    public void testDriverCaching() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("No suitable Driver");
        boolean canary = false;
        SQLExec sql = createTask(getProperties(NULL));
        assertThat(SQLExec.getLoaderMap(), not(hasKey(NULL_DRIVER)));
        try {
            sql.execute();
            canary = true;
        } finally {
            assertFalse("Found some Driver", canary);
            assertThat(SQLExec.getLoaderMap(), hasKey(NULL_DRIVER));
            assertSame(sql.getLoader(), JDBCTask.getLoaderMap().get(NULL_DRIVER));
            ClassLoader loader1 = sql.getLoader();

            // 2nd run..
            sql = createTask(getProperties(NULL));
            // the driver must still be cached.
            assertThat(JDBCTask.getLoaderMap(), hasKey(NULL_DRIVER));
            try {
                sql.execute();
                canary = true;
            } finally {
                assertFalse("Found some Driver", canary);
                assertThat(JDBCTask.getLoaderMap(), hasKey(NULL_DRIVER));
                assertSame(sql.getLoader(), JDBCTask.getLoaderMap().get(NULL_DRIVER));
                assertSame(loader1, sql.getLoader());
            }
        }
    }

    @Test
    public void testNull() {
        doMultipleCalls(1000, NULL, true, true);
    }

    @Ignore
    @Test
    public void testOracle() {
        doMultipleCalls(1000, ORACLE, true, false);
    }

    @Ignore
    @Test
    public void testMySQL() {
        doMultipleCalls(1000, MYSQL, true, false);
    }


    /**
     * run a sql tasks multiple times.
     * @param calls number of times to execute the task
     * @param database the database to execute on.
     * @param caching should caching be enabled ?
     * @param catchexception true to catch exception for each call, false if not.
     */
    protected void doMultipleCalls(int calls, int database, boolean caching, boolean catchexception) {
        Properties props = getProperties(database);
        for (int i = 0; i < calls; i++) {
            SQLExec sql = createTask(props);
            sql.setCaching(caching);
            try  {
                sql.execute();
            } catch (BuildException e) {
                if (!catchexception) {
                    throw e;
                }
            }
        }
    }

    /**
     * Create a task from a set of properties
     * @see #getProperties(int)
     */
    protected SQLExec createTask(Properties props) {
        SQLExec sql = new SQLExec();
        sql.setProject(new Project());
        sql.setDriver(props.getProperty(DRIVER));
        sql.setUserid(props.getProperty(USER));
        sql.setPassword(props.getProperty(PASSWORD));
        sql.setUrl(props.getProperty(URL));
        sql.createClasspath().setLocation(new File(props.getProperty(PATH)));
        sql.addText(props.getProperty(SQL));
        return sql;
    }

    /**
     * try to find the path from a resource (jar file or directory name)
     * so that it can be used as a classpath to load the resource.
     */
    protected String findResourcePath(String resource) {
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
     *
     * @param database int
     */
    protected Properties getProperties(int database) {
        Properties props = null;
        switch (database) {
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

    /**
     * helper method to build properties
     *
     * @param driver String
     * @param user String
     * @param pwd String
     * @param url String
     */
    protected Properties getProperties(String driver, String user, String pwd, String url) {
        Properties props = new Properties();
        props.put(DRIVER, driver);
        props.put(USER, user);
        props.put(PASSWORD, pwd);
        props.put(URL, url);
        return props;
    }


//--- NULL JDBC driver just for simple test since there are no db driver
// available as a default in Ant :)

    public static final String NULL_DRIVER = NullDriver.class.getName();

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

        public Logger getParentLogger() /*throws SQLFeatureNotSupportedException*/ {
            return Logger.getAnonymousLogger();
        }
    }

    @Test
    public void testLastDelimiterPositionNormalModeStrict() {
        SQLExec s = new SQLExec();
        assertEquals(-1,
                     s.lastDelimiterPosition(new StringBuffer(), null));
        assertEquals(-1,
                     s.lastDelimiterPosition(new StringBuffer("GO"), null));
        assertEquals(-1,
                     s.lastDelimiterPosition(new StringBuffer("; "), null));
        assertEquals(2,
                     s.lastDelimiterPosition(new StringBuffer("ab;"), null));
        s.setDelimiter("GO");
        assertEquals(-1,
                     s.lastDelimiterPosition(new StringBuffer("GO "), null));
        assertEquals(-1,
                     s.lastDelimiterPosition(new StringBuffer("go"), null));
        assertEquals(0,
                     s.lastDelimiterPosition(new StringBuffer("GO"), null));
    }

    @Test
    public void testLastDelimiterPositionNormalModeNonStrict() {
        SQLExec s = new SQLExec();
        s.setStrictDelimiterMatching(false);
        assertEquals(-1,
                     s.lastDelimiterPosition(new StringBuffer(), null));
        assertEquals(-1,
                     s.lastDelimiterPosition(new StringBuffer("GO"), null));
        assertEquals(0,
                     s.lastDelimiterPosition(new StringBuffer("; "), null));
        assertEquals(2,
                     s.lastDelimiterPosition(new StringBuffer("ab;"), null));
        s.setDelimiter("GO");
        assertEquals(0,
                     s.lastDelimiterPosition(new StringBuffer("GO "), null));
        assertEquals(0,
                     s.lastDelimiterPosition(new StringBuffer("go"), null));
        assertEquals(0,
                     s.lastDelimiterPosition(new StringBuffer("GO"), null));
    }

    @Test
    public void testLastDelimiterPositionRowModeStrict() {
        SQLExec s = new SQLExec();
        SQLExec.DelimiterType t = new SQLExec.DelimiterType();
        t.setValue("row");
        s.setDelimiterType(t);
        assertEquals(-1, s.lastDelimiterPosition(null, ""));
        assertEquals(-1, s.lastDelimiterPosition(null, "GO"));
        assertEquals(-1, s.lastDelimiterPosition(null, "; "));
        assertEquals(1, s.lastDelimiterPosition(new StringBuffer("ab"), ";"));
        s.setDelimiter("GO");
        assertEquals(-1, s.lastDelimiterPosition(null, "GO "));
        assertEquals(-1, s.lastDelimiterPosition(null, "go"));
        assertEquals(0, s.lastDelimiterPosition(new StringBuffer("ab"), "GO"));
    }

    @Test
    public void testLastDelimiterPositionRowModeNonStrict() {
        SQLExec s = new SQLExec();
        SQLExec.DelimiterType t = new SQLExec.DelimiterType();
        t.setValue("row");
        s.setDelimiterType(t);
        s.setStrictDelimiterMatching(false);
        assertEquals(-1, s.lastDelimiterPosition(null, ""));
        assertEquals(-1, s.lastDelimiterPosition(null, "GO"));
        assertEquals(0, s.lastDelimiterPosition(new StringBuffer("; "), "; "));
        assertEquals(1, s.lastDelimiterPosition(new StringBuffer("ab"), ";"));
        s.setDelimiter("GO");
        assertEquals(1, s.lastDelimiterPosition(new StringBuffer("abcd"), "GO "));
        assertEquals(0, s.lastDelimiterPosition(new StringBuffer("go"), "go"));
        assertEquals(0, s.lastDelimiterPosition(new StringBuffer("ab"), "GO"));
    }

}
