/* 
 * Copyright  2003-2004 Apache Software Foundation
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

package org.apache.tools.ant.types;

import java.security.UnresolvedPermission;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ExitException;

/**
 * This class implements a security manager meant for useage by tasks that run inside the 
 * ant VM. An examples are the Java Task and JUnitTask.
 * 
 * The basic functionality is that nothing (except for a base set of permissions) is allowed, unless 
 * the permission is granted either explicitly or implicitly.
 * If an permission is granted this can be overruled by explicitly revoking the permission.
 * 
 * It is not permissible to add permissions (either granted or revoked) while the Security Manager
 * is active (after calling setSecurityManager() but before calling restoreSecurityManager()).
 * 
 * @since Ant 1.6
 * @author <a href="mailto:martijn@kruithof.xs4all.nl">Martijn Kruithof</a>
 */
public class Permissions {
    
    private List grantedPermissions = new LinkedList();
    private List revokedPermissions = new LinkedList();
    private java.security.Permissions granted = null;
    private SecurityManager origSm = null;
    private boolean active = false;
    private boolean delegateToOldSM = false;

    /**
     * default constructor
     */
    public Permissions() {
    }
    /**
     * create a new set of permissions
     * @param delegateToOldSM  if <code>true</code> the old security manager
     * will be used if the permission has not been explicitly granted or revoked
     * in this instance
     * if false, it behaves like the default constructor
     */
    public Permissions(boolean delegateToOldSM) {
        this.delegateToOldSM = delegateToOldSM;
    }
    /**
     * Adds a permission to be granted.
     * @param perm The Permissions.Permission to be granted. 
     */
    public void addConfiguredGrant(Permissions.Permission perm) {
        grantedPermissions.add(perm);
    }    

    /** 
     * Adds a permission to be revoked.
     * @param perm The Permissions.Permission to be revoked 
     */
    public void addConfiguredRevoke(Permissions.Permission perm) {
        revokedPermissions.add(perm);
    }
    
    /** 
     * To be used by tasks wishing to use this security model before executing the part to be
     * subject to these Permissions. Note that setting the SecurityManager too early may 
     * prevent your part from starting, as for instance changing classloaders may be prohibited.
     * The classloader for the new situation is supposed to be present. 
     */
    public void setSecurityManager() throws BuildException{
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
        for (Iterator i = revokedPermissions.listIterator(); i.hasNext();) {
            Permissions.Permission p = (Permissions.Permission) i.next();
            if (p.getClassName() == null) {
                throw new BuildException("Revoked permission " + p + " does not contain a class.");
            }
        }
        for (Iterator i = grantedPermissions.listIterator(); i.hasNext();) {
            Permissions.Permission p = (Permissions.Permission) i.next();
            if (p.getClassName() == null) {
                throw new BuildException("Granted permission " + p + " does not contain a class.");
            } else {
                java.security.Permission perm =  new UnresolvedPermission(p.getClassName(),p.getName(),p.getActions(),null);                    
                granted.add(perm);
            }
        }
        // Add base set of permissions
        granted.add(new java.net.SocketPermission("localhost:1024-", "listen"));
        granted.add(new java.util.PropertyPermission("java.version", "read"));
        granted.add(new java.util.PropertyPermission("java.vendor", "read"));
        granted.add(new java.util.PropertyPermission("java.vendor.url", "read"));
        granted.add(new java.util.PropertyPermission("java.class.version", "read"));
        granted.add(new java.util.PropertyPermission("os.name", "read"));
        granted.add(new java.util.PropertyPermission("os.version", "read"));
        granted.add(new java.util.PropertyPermission("os.arch", "read"));
        granted.add(new java.util.PropertyPermission("file.encoding", "read"));
        granted.add(new java.util.PropertyPermission("file.separator", "read"));
        granted.add(new java.util.PropertyPermission("path.separator", "read"));
        granted.add(new java.util.PropertyPermission("line.separator", "read"));
        granted.add(new java.util.PropertyPermission("java.specification.version", "read"));
        granted.add(new java.util.PropertyPermission("java.specification.vendor", "read"));
        granted.add(new java.util.PropertyPermission("java.specification.name", "read"));
        granted.add(new java.util.PropertyPermission("java.vm.specification.version", "read"));
        granted.add(new java.util.PropertyPermission("java.vm.specification.vendor", "read"));
        granted.add(new java.util.PropertyPermission("java.vm.specification.name", "read"));
        granted.add(new java.util.PropertyPermission("java.vm.version", "read"));
        granted.add(new java.util.PropertyPermission("java.vm.vendor", "read"));
        granted.add(new java.util.PropertyPermission("java.vm.name", "read"));
    }

    /**
     * To be used by tasks that just finished executing the parts subject to these permissions.
     */
    public void restoreSecurityManager() {
        active = false;
        System.setSecurityManager(origSm);
    }
    
    /**
     * This inner class implements the actual SecurityManager that can be used by tasks
     * supporting Permissions. 
     */
    private class MySM extends SecurityManager {
        
        /**
         * Exit is treated in a special way in order to be able to return the exit code towards tasks.
         * An ExitException is thrown instead of a simple SecurityException to indicate the exit
         * code.
         * Overridden from java.lang.SecurityManager
         * @param status The exit status requested.
         */
        public void checkExit(int status) {
            java.security.Permission perm = new java.lang.RuntimePermission("exitVM",null);
            try {
                checkPermission(perm);  
            } catch (SecurityException e) {
                throw new ExitException(e.getMessage(), status);
            }
        }
        
        /**
         * The central point in checking permissions.
         * Overridden from java.lang.SecurityManager
         * 
         * @param perm The permission requested.
         */
        public void checkPermission(java.security.Permission perm) {
            if (active) {
                if (delegateToOldSM && !perm.getName().equals("exitVM")) {
                    boolean permOK = false;
                    if (granted.implies(perm)) {
                        permOK = true;
                    }
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
        private void checkRevoked(java.security.Permission perm) {
            for (Iterator i = revokedPermissions.listIterator(); i.hasNext();) {
                if (((Permissions.Permission)i.next()).matches(perm)) {
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
        private Set actions;
    
        /** 
         * Sets the class, mandatory.
         * @param aClass The class name of the permission. 
         */
        public void setClass(String aClass) {
                className = aClass.trim();
        }
        
        /** Get the class of the permission
         * @return The class name of the permission.
         */
        public String getClassName() {
            return className;
        }

        /** 
         * Sets the name of the permission.
         * @param aName The name of the permission.
         */        
        public void setName(String aName) {
            name = aName.trim();
        }
        
        /** 
         * Get the name of the permission.
         * @return  The name of the permission.
         */
        public String getName() {
            return name;
        }
    
        /**
         * Sets the actions.
         * @param actions The actions of the permission. 
         */
        public void setActions(String actions) {
            actionString = actions;
            if (actions.length() > 0) {
                this.actions = parseActions(actions);
            }
        }
        
        /**
         * Gets the actions.
         * @return The actions of the permission. 
         */
        public String getActions() {
            return actionString;
        }
        
        /**
         *  Checks if the permission matches in case of a revoked permission.
         * @param perm The permission to check against.
         */
        boolean matches(java.security.Permission perm) {
            
            if (!className.equals(perm.getClass().getName())) {
                return false;
            }
            
            if (name != null) {
                if (name.endsWith("*")) {
                    if (!perm.getName().startsWith(name.substring(0, name.length() - 1))) {
                        return false;
                    }
                } else {
                    if (!name.equals(perm.getName())) {
                        return false;
                    }
                }
            }
            
            if (actions != null) {
                Set as = parseActions(perm.getActions());
                int size = as.size();
                as.removeAll(actions);
                if (as.size() == size) {
                    // None of the actions revoked, so all allowed.
                    return false;
                }
            }
            
            return true;
        }
  
        /** 
         * Parses the actions into a set of separate strings.
         * @param actions The actions to be parsed.
         */
        private Set parseActions(String actions) {
            Set result = new HashSet();
            StringTokenizer tk = new StringTokenizer(actions, ",");
            while (tk.hasMoreTokens()) {
                String item = tk.nextToken().trim();
                if (!item.equals("")) {
                    result.add(item);
                }
            }
            return result;
        }
        /**
         * get a string description of the permissions
         * @return  string description of the permissions
         */
        public String toString() {
            return ("Permission: " + className + " (\"" + name + "\", \"" + actions + "\")");
        }
    }   
}