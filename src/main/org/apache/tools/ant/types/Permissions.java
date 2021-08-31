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

package org.apache.tools.ant.types;

import java.lang.reflect.Constructor;
import java.net.SocketPermission;
import java.security.UnresolvedPermission;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PropertyPermission;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ExitException;

/**
 * This class implements a security manager meant for usage by tasks that run inside the
 * Ant VM. An examples are the Java Task and JUnitTask.
 *
 * The basic functionality is that nothing (except for a base set of permissions) is allowed, unless
 * the permission is granted either explicitly or implicitly.
 * If a permission is granted this can be overruled by explicitly revoking the permission.
 *
 * It is not permissible to add permissions (either granted or revoked) while the Security Manager
 * is active (after calling setSecurityManager() but before calling restoreSecurityManager()).
 *
 * @since Ant 1.6
 */
public class Permissions {

    private final List<Permission> grantedPermissions = new LinkedList<>();
    private final List<Permission> revokedPermissions = new LinkedList<>();
    private java.security.Permissions granted = null;
    private SecurityManager origSm = null;
    private boolean active = false;
    private final boolean delegateToOldSM;

    // Mandatory constructor for permission object.
    private static final Class<?>[] PARAMS = {String.class, String.class};

    /**
     * Create a set of Permissions.  Equivalent to calling
     * <code>new Permissions(false)</code>.
     */
    public Permissions() {
        this(false);
    }

    /**
     * Create a set of permissions.
     * @param delegateToOldSM  if <code>true</code> the old security manager
     * will be used if the permission has not been explicitly granted or revoked
     * in this instance.
     */
    public Permissions(final boolean delegateToOldSM) {
        this.delegateToOldSM = delegateToOldSM;
    }

    /**
     * Adds a permission to be granted.
     * @param perm The Permissions.Permission to be granted.
     */
    public void addConfiguredGrant(final Permissions.Permission perm) {
        grantedPermissions.add(perm);
    }

    /**
     * Adds a permission to be revoked.
     * @param perm The Permissions.Permission to be revoked
     */
    public void addConfiguredRevoke(final Permissions.Permission perm) {
        revokedPermissions.add(perm);
    }

    /**
     * To be used by tasks wishing to use this security model before executing the part to be
     * subject to these Permissions. Note that setting the SecurityManager too early may
     * prevent your part from starting, as for instance changing classloaders may be prohibited.
     * The classloader for the new situation is supposed to be present.
     * @throws BuildException on error
     */
    public synchronized void setSecurityManager() throws BuildException {
        origSm = System.getSecurityManager();
        init();
        System.setSecurityManager(new MySM());
        active = true;
    }

    /**
     * Initializes the list of granted permissions, checks the list of revoked permissions.
     */
    private void init() throws BuildException {
        granted = new java.security.Permissions();
        for (final Permissions.Permission p : revokedPermissions) {
            if (p.getClassName() == null) {
                throw new BuildException("Revoked permission " + p + " does not contain a class.");
            }
        }
        for (final Permissions.Permission p : grantedPermissions) {
            if (p.getClassName() == null) {
                throw new BuildException("Granted permission " + p
                        + " does not contain a class.");
            } else {
                final java.security.Permission perm = createPermission(p);
                granted.add(perm);
            }
        }
        // Add base set of permissions
        granted.add(new SocketPermission("localhost:1024-", "listen"));
        granted.add(new PropertyPermission("java.version", "read"));
        granted.add(new PropertyPermission("java.vendor", "read"));
        granted.add(new PropertyPermission("java.vendor.url", "read"));
        granted.add(new PropertyPermission("java.class.version", "read"));
        granted.add(new PropertyPermission("os.name", "read"));
        granted.add(new PropertyPermission("os.version", "read"));
        granted.add(new PropertyPermission("os.arch", "read"));
        granted.add(new PropertyPermission("file.encoding", "read"));
        granted.add(new PropertyPermission("file.separator", "read"));
        granted.add(new PropertyPermission("path.separator", "read"));
        granted.add(new PropertyPermission("line.separator", "read"));
        granted.add(new PropertyPermission("java.specification.version", "read"));
        granted.add(new PropertyPermission("java.specification.vendor", "read"));
        granted.add(new PropertyPermission("java.specification.name", "read"));
        granted.add(new PropertyPermission("java.vm.specification.version", "read"));
        granted.add(new PropertyPermission("java.vm.specification.vendor", "read"));
        granted.add(new PropertyPermission("java.vm.specification.name", "read"));
        granted.add(new PropertyPermission("java.vm.version", "read"));
        granted.add(new PropertyPermission("java.vm.vendor", "read"));
        granted.add(new PropertyPermission("java.vm.name", "read"));
    }

    private java.security.Permission createPermission(
            final Permissions.Permission permission) {
        try {
            // First add explicitly already resolved permissions will not be
            // resolved when added as unresolved permission.
            final Class<? extends java.security.Permission> clazz = Class.forName(
                    permission.getClassName()).asSubclass(java.security.Permission.class);
            final String name = permission.getName();
            final String actions = permission.getActions();
            final Constructor<? extends java.security.Permission> ctr = clazz.getConstructor(PARAMS);
            return ctr.newInstance(name, actions);
        } catch (final Exception e) {
            // Let the UnresolvedPermission handle it.
            return new UnresolvedPermission(permission.getClassName(),
                    permission.getName(), permission.getActions(), null);
        }
    }

    /**
     * To be used by tasks that just finished executing the parts subject to these permissions.
     */
    public synchronized void restoreSecurityManager() {
        active = false;
        System.setSecurityManager(origSm);
    }

    /**
     * This inner class implements the actual SecurityManager that can be used by tasks
     * supporting Permissions.
     */
    private class MySM extends SecurityManager {

        /**
         * Exit is treated in a special way in order to be able to return the exit code
         * towards tasks.
         * An ExitException is thrown instead of a simple SecurityException to indicate the exit
         * code.
         * Overridden from java.lang.SecurityManager
         * @param status The exit status requested.
         */
        @Override
        public void checkExit(final int status) {
            final java.security.Permission perm = new RuntimePermission("exitVM", null);
            try {
                checkPermission(perm);
            } catch (final SecurityException e) {
                throw new ExitException(e.getMessage(), status);
            }
        }

        /**
         * The central point in checking permissions.
         * Overridden from java.lang.SecurityManager
         *
         * @param perm The permission requested.
         */
        @Override
        public void checkPermission(final java.security.Permission perm) {
            if (active) {
                if (delegateToOldSM && !perm.getName().equals("exitVM")) {
                    boolean permOK = granted.implies(perm);
                    checkRevoked(perm);
                    /*
                     if the permission was not explicitly granted or revoked
                     the original security manager will do its work
                    */
                    if (!permOK && origSm != null) {
                        origSm.checkPermission(perm);
                    }
                }  else {
                    if (!granted.implies(perm)) {
                        throw new SecurityException("Permission " + perm + " was not granted.");
                    }
                    checkRevoked(perm);
                }
            }
        }

        /**
         * throws an exception if this permission is revoked
         * @param perm the permission being checked
         */
        private void checkRevoked(final java.security.Permission perm) {
            for (final Permissions.Permission revoked : revokedPermissions) {
                if (revoked.matches(perm)) {
                    throw new SecurityException("Permission " + perm + " was revoked.");
                }
            }
        }
    }

    /** Represents a permission. */
    public static class Permission {
        private String className;
        private String name;
        private String actionString;
        private Set<String> actions;

        /**
         * Set the class, mandatory.
         * @param aClass The class name of the permission.
         */
        public void setClass(final String aClass) {
            className = aClass.trim();
        }

        /**
         * Get the class of the permission.
         * @return The class name of the permission.
         */
        public String getClassName() {
            return className;
        }

        /**
         * Set the name of the permission.
         * @param aName The name of the permission.
         */
        public void setName(final String aName) {
            name = aName.trim();
        }

        /**
         * Get the name of the permission.
         * @return The name of the permission.
         */
        public String getName() {
            return name;
        }

        /**
         * Set the actions.
         * @param actions The actions of the permission.
         */
        public void setActions(final String actions) {
            actionString = actions;
            if (!actions.isEmpty()) {
                this.actions = parseActions(actions);
            }
        }

        /**
         * Get the actions.
         * @return The actions of the permission.
         */
        public String getActions() {
            return actionString;
        }

        /**
         * Learn whether the permission matches in case of a revoked permission.
         * @param perm The permission to check against.
         */
        boolean matches(final java.security.Permission perm) {
            if (!className.equals(perm.getClass().getName())) { //NOSONAR
                return false;
            }
            if (name != null) {
                if (name.endsWith("*")) {
                    if (!perm.getName().startsWith(name.substring(0, name.length() - 1))) {
                        return false;
                    }
                } else if (!name.equals(perm.getName())) {
                    return false;
                }
            }
            if (actions != null) {
                final Set<String> as = parseActions(perm.getActions());
                final int size = as.size();
                as.removeAll(actions);
                // If no actions removed, then all allowed
                return as.size() != size;
            }
            return true;
        }

        /**
         * Parses the actions into a set of separate strings.
         * @param actions The actions to be parsed.
         */
        private Set<String> parseActions(final String actions) {
            final Set<String> result = new HashSet<>();
            final StringTokenizer tk = new StringTokenizer(actions, ",");
            while (tk.hasMoreTokens()) {
                final String item = tk.nextToken().trim();
                if (!item.isEmpty()) {
                    result.add(item);
                }
            }
            return result;
        }

        /**
         * Get a string description of the permissions.
         * @return string description of the permissions.
         */
        @Override
        public String toString() {
            return ("Permission: " + className + " (\"" + name + "\", \"" + actions + "\")");
        }
    }
}
