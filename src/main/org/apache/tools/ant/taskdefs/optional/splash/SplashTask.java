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

package org.apache.tools.ant.taskdefs.optional.splash;

import java.io.InputStream;
import java.io.DataInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import javax.swing.ImageIcon;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
 
/**
 * Creates a splash screen. The splash screen is displayed
 * for the duration of the build and includes a handy progress bar as
 * well. Use in conjunction with the sound task to provide interest
 * whilst waiting for your builds to complete...
 * @since Ant1.5 
 * @author Les Hughes (leslie.hughes@rubus.com)
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
     */
    public void setImageURL(String imgurl) {
        this.imgurl = imgurl;
    }
    
    /**
     * flag to enable proxy settings; optional, deprecated : consider
     * using &lt;setproxy&gt; instead 
     * @deprecated use org.apache.tools.ant.taskdefs.optional.SetProxy
     */
    public void setUseproxy(boolean useProxy) {
        this.useProxy = useProxy;
    }

    /**
     * name of proxy; optional.
     */
    public void setProxy(String proxy){
        this.proxy = proxy;
    }
    
    /**
     * Proxy port; optional, default 80. 
     */
    public void setPort(String port){
        this.port = port;
    }

    /**
     * Proxy user; optional, default =none. 
     */
    public void setUser(String user){
        this.user = user;
    }
    
    /**
     * Proxy password; required if <tt>user</tt> is set.
     */
     public void setPassword(String password){
        this.password = password;
    }
    
    /**
     * how long to show the splash screen in milliseconds,
     * optional; default 5000 ms.
     */
    public void setShowduration(int duration) {
        this.showDuration = duration;
    }
   

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
                
                if (useProxy &&
                   (proxy != null && proxy.length() > 0) &&
                   (port != null && port.length() > 0)) {
                    
                    log("Using proxied Connection",  Project.MSG_DEBUG);
                    System.getProperties().put("http.proxySet", "true");
                    System.getProperties().put("http.proxyHost", proxy);
                    System.getProperties().put("http.proxyPort", port);
                    
                    URL url = new URL(imgurl);
                    
                    conn = url.openConnection();
                    if (user != null && user.length() > 0) {
                        String encodedcreds = 
                            new sun.misc.BASE64Encoder().encode((new String(user + ":" + password)).getBytes());
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

                // Catch everything - some of the above return nulls, throw exceptions or generally misbehave
                // in the event of a problem etc
                
            } catch (Throwable ioe) {
                log("Unable to download image, trying default Ant Logo", 
                    Project.MSG_DEBUG);
                log("(Exception was \"" + ioe.getMessage() + "\"", 
                    Project.MSG_DEBUG);
            }
        }

        if (in == null) {
            in = SplashTask.class.getClassLoader().getResourceAsStream("images/ant_logo_large.gif");
        }

        if (in != null) {
            DataInputStream din = new DataInputStream(in);
            boolean success = false;
            try {
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                int data;
                while ((data = din.read()) != -1) {
                    bout.write((byte) data);
                }
                
                log("Got ByteArray, creating splash",  Project.MSG_DEBUG);
                ImageIcon img = new ImageIcon(bout.toByteArray());
                
                splash = new SplashScreen(img);
                success = true;
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
            splash = new SplashScreen("Image Unavailable.");
        }

        splash.setVisible(true);
        splash.toFront();
        getProject().addBuildListener(splash);
        try {
            Thread.currentThread().sleep(showDuration);
        } catch (InterruptedException e) {
        }
        
    }
}
