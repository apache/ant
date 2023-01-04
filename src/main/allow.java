/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
//
// THIS CLASS IS INTENTIONALLY IN AN UNNAMED PACKAGE
// (see the class level javadoc of this class for more details)
//

import java.io.FileDescriptor;
import java.net.InetAddress;
import java.security.Permission;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class is only here to allow setting the {@code java.security.manager} system property
 * to a value of {@code allow}.
 * <p>
 * Certain versions of Java (like Java 8) do not recognize {@code allow}
 * as a valid textual value for the {@code java.security.manager}, but some higher versions of
 * Java do recognize this value. While launching Ant (from scripts for example), it isn't straightforward
 * to identify which runtime version of Java is used to launch Ant. That then causes additional and
 * complex scripting logic (that works across all supported OS platforms) to first identify the Java
 * runtime version being used and then deciding whether or not to set {@code allow} as a value for
 * that system property.
 * </p>
 * <p>
 * The system property value for this {@code java.security.manager} is considered some predefined
 * text or if it doesn't match that predefined text then is considered a fully qualified classname
 * of a class which extends the {@link SecurityManager} class. We use that knowledge to workaround
 * the problem we have with setting {@code allow} as the value for Java runtime which don't understand
 * that value. This {@code allow} class belongs to an unnamed package and is packaged within a jar
 * file {@code ant-launcher.jar} which Ant always adds to the classpath for launching Ant. That
 * way, this class is available in the classpath and any Java versions that don't recognize
 * {@code allow} as a predefined value will end up instantiating this class.
 * </p>
 * <p>
 * The implementation in this class doesn't really provide any {@code SecurityManager} expected
 * semantics. So this really isn't a {@code SecurityManager} implementation and shouldn't be used
 * as one. If/when this class gets instantiated and is set as a {@code SecurityManager}, it will
 * uninstall itself, on first use, by calling
 * {@link System#setSecurityManager(SecurityManager) System.setSecurityManager(null)}. First use is
 * defined as any call on a public method of this instance.
 * This class intentionally uninstalls itself on first use to preserve the semantics of {@code allow}
 * which merely implies that setting a security manager instance by the application code through the
 * use of {@link System#setSecurityManager(SecurityManager)} is allowed.
 * </p>
 *
 * @deprecated This isn't for public consumption and is an internal detail of Ant
 */
// This class has been copied over from the Apache NetBeans project
@Deprecated
public class allow extends SecurityManager {

    private final AtomicBoolean uninstalling = new AtomicBoolean();

    private void uninstall() {
        if (uninstalling.compareAndSet(false, true)) {
            // we set the security manager to null only when we ascertain that this class
            // instance is the current installed security manager. We do this to avoid any race
            // conditions where some other thread/caller had concurrently called
            // System.setSecurityManager and we end up "null"ing that set instance.
            // We rely on the (internal) detail that System.setSecurityManager is synchronized
            // on the System.class
            synchronized (System.class) {
                final SecurityManager currentSecManager = System.getSecurityManager();
                if (currentSecManager != this) {
                    return;
                }
                System.setSecurityManager(null);
            }
        }
    }

    @Override
    public void checkAccept(String host, int port) {
        uninstall();
    }

    @Override
    public void checkAccess(Thread t) {
        uninstall();
    }

    @Override
    public void checkAccess(ThreadGroup g) {
        uninstall();
    }

    //@Override
    public void checkAwtEventQueueAccess() {
        uninstall();
    }

    @Override
    public void checkConnect(String host, int port) {
        uninstall();
    }

    @Override
    public void checkConnect(String host, int port, Object context) {
        uninstall();
    }

    @Override
    public void checkCreateClassLoader() {
        uninstall();
    }

    @Override
    public void checkDelete(String file) {
        uninstall();
    }

    @Override
    public void checkExec(String cmd) {
        uninstall();
    }

    @Override
    public void checkExit(int status) {
        uninstall();
    }

    @Override
    public void checkLink(String lib) {
        uninstall();
    }

    @Override
    public void checkListen(int port) {
        uninstall();
    }

    //@Override
    public void checkMemberAccess(Class<?> clazz, int which) {
        uninstall();
    }

    @Override
    public void checkMulticast(InetAddress maddr) {
        uninstall();
    }

    @Override
    public void checkMulticast(InetAddress maddr, byte ttl) {
        uninstall();
    }

    @Override
    public void checkPackageAccess(String pkg) {
        uninstall();
    }

    @Override
    public void checkPackageDefinition(String pkg) {
        uninstall();
    }

    @Override
    public void checkPermission(Permission perm) {
        uninstall();
    }

    @Override
    public void checkPermission(Permission perm, Object context) {
        uninstall();
    }

    @Override
    public void checkPrintJobAccess() {
        uninstall();
    }

    @Override
    public void checkPropertiesAccess() {
        uninstall();
    }

    @Override
    public void checkPropertyAccess(String key) {
        uninstall();
    }

    @Override
    public void checkRead(FileDescriptor fd) {
        uninstall();
    }

    @Override
    public void checkRead(String file) {
        uninstall();
    }

    @Override
    public void checkRead(String file, Object context) {
        uninstall();
    }

    @Override
    public void checkSecurityAccess(String target) {
        uninstall();
    }

    @Override
    public void checkSetFactory() {
        uninstall();
    }

    //@Override
    public void checkSystemClipboardAccess() {
        uninstall();
    }

    //@Override
    public boolean checkTopLevelWindow(Object window) {
        uninstall();
        // we return false because we don't know what thread would be calling this permission
        // check method
        return false;
    }

    @Override
    public void checkWrite(FileDescriptor fd) {
        uninstall();
    }

    @Override
    public void checkWrite(String file) {
        uninstall();
    }
}

