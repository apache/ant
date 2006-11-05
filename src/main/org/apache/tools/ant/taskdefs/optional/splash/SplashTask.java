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

package org.apache.tools.ant.taskdefs.optional.splash;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import javax.swing.ImageIcon;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.util.Base64Converter;

/**
 * Creates a splash screen. The splash screen is displayed
 * for the duration of the build and includes a handy progress bar as
 * well. Use in conjunction with the sound task to provide interest
 * whilst waiting for your builds to complete...
 * @since Ant1.5
 */
public class SplashTask extends Task {

    private String imgurl = null;
    private String proxy = null;
    private String user = null;
    private String password = null;
    private String port = "80";
    private int showDuration = 5000;
    private boolean useProxy = false;

    private static SplashScreen splash = null;

    /**
     * A URL pointing to an image to display; optional, default antlogo.gif
     * from the classpath.
     * @param imgurl the url string pointing to the image
     */
    public void setImageURL(String imgurl) {
        this.imgurl = imgurl;
    }

    /**
     * flag to enable proxy settings; optional, deprecated : consider
     * using &lt;setproxy&gt; instead
     * @param useProxy if ture, enable proxy settings
     * @deprecated since 1.5.x.
     *             Use org.apache.tools.ant.taskdefs.optional.SetProxy
     */
    public void setUseproxy(boolean useProxy) {
        this.useProxy = useProxy;
    }

    /**
     * name of proxy; optional.
     * @param proxy the name of the proxy host
     */
    public void setProxy(String proxy) {
        this.proxy = proxy;
    }

    /**
     * Proxy port; optional, default 80.
     * @param port the proxy port
     */
    public void setPort(String port) {
        this.port = port;
    }

    /**
     * Proxy user; optional, default =none.
     * @param user the proxy user
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * Proxy password; required if <tt>user</tt> is set.
     * @param password the proxy password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * how long to show the splash screen in milliseconds,
     * optional; default 5000 ms.
     * @param duration the spash duration in milliseconds
     */
    public void setShowduration(int duration) {
        this.showDuration = duration;
    }


    /**
     * Execute the task.
     * @throws BuildException on error
     */
    public void execute() throws BuildException {
        if (splash != null) {
            splash.setVisible(false);
            getProject().removeBuildListener(splash);
            splash.dispose();
            splash = null;
        }

        log("Creating new SplashScreen", Project.MSG_VERBOSE);
        InputStream in = null;

        if (imgurl != null) {
            try {
                URLConnection conn = null;

                if (useProxy && (proxy != null && proxy.length() > 0)
                    && (port != null && port.length() > 0)) {

                    log("Using proxied Connection",  Project.MSG_DEBUG);
                    System.getProperties().put("http.proxySet", "true");
                    System.getProperties().put("http.proxyHost", proxy);
                    System.getProperties().put("http.proxyPort", port);

                    URL url = new URL(imgurl);

                    conn = url.openConnection();
                    if (user != null && user.length() > 0) {
                        // converted from sun internal classes to
                        // new Base64Converter
                        // utility class extracted from Get task
                        String encodedcreds =
                            new Base64Converter().encode(user + ":" + password);
                        conn.setRequestProperty("Proxy-Authorization",
                                                encodedcreds);
                    }

                } else {
                    System.getProperties().put("http.proxySet", "false");
                    System.getProperties().put("http.proxyHost", "");
                    System.getProperties().put("http.proxyPort", "");
                    log("Using Direction HTTP Connection", Project.MSG_DEBUG);
                    URL url = new URL(imgurl);
                    conn = url.openConnection();
                }
                conn.setDoInput(true);
                conn.setDoOutput(false);

                in = conn.getInputStream();

                // Catch everything - some of the above return nulls,
                // throw exceptions or generally misbehave
                // in the event of a problem etc

            } catch (Throwable ioe) {
                log("Unable to download image, trying default Ant Logo",
                    Project.MSG_DEBUG);
                log("(Exception was \"" + ioe.getMessage() + "\"",
                    Project.MSG_DEBUG);
            }
        }

        if (in == null) {
            ClassLoader cl = SplashTask.class.getClassLoader();
            if (cl != null) {
                in = cl.getResourceAsStream("images/ant_logo_large.gif");
            } else {
                in = ClassLoader
                    .getSystemResourceAsStream("images/ant_logo_large.gif");
            }
        }

        boolean success = false;
        if (in != null) {
            DataInputStream din = new DataInputStream(in);
            try {
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                int data;
                while ((data = din.read()) != -1) {
                    bout.write((byte) data);
                }

                log("Got ByteArray, creating splash",  Project.MSG_DEBUG);

                try {
                    ImageIcon img = new ImageIcon(bout.toByteArray());
                    splash = new SplashScreen(img);
                    success = true;
                } catch (Throwable e) {
                    logHeadless(e);
                }
            } catch (Exception e) {
                throw new BuildException(e);
            } finally {
                try {
                    din.close();
                } catch (IOException ioe) {
                    // swallow if there was an error before so that
                    // original error will be passed up
                    if (success) {
                        throw new BuildException(ioe);
                    }
                }
            }
        } else {
            try {
                splash = new SplashScreen("Image Unavailable.");
                success = true;
            } catch (Throwable e) {
                logHeadless(e);
            }
        }

        if (success) {
            splash.setVisible(true);
            splash.toFront();
            getProject().addBuildListener(splash);
            try {
                Thread.sleep(showDuration);
            } catch (InterruptedException e) {
                // Ignore Exception
            }
        }
    }

    private void logHeadless(Throwable e) {
        log("failed to display SplashScreen, caught "
            + e.getClass().getName() + " with message: " + e.getMessage(),
            Project.MSG_WARN);
    }
}
