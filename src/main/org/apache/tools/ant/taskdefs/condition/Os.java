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
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
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

package org.apache.tools.ant.taskdefs.condition;

import java.util.Locale;
import org.apache.tools.ant.BuildException;

/**
 * Condition that tests the OS type.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:umagesh@apache.org">Magesh Umasankar</a>
 * @since Ant 1.4
 * @version $Revision$
 */
public class Os implements Condition {
    private static final String osName =
        System.getProperty("os.name").toLowerCase(Locale.US);
    private static final String osArch =
        System.getProperty("os.arch").toLowerCase(Locale.US);
    private static final String osVersion =
        System.getProperty("os.version").toLowerCase(Locale.US);
    private static final String pathSep = System.getProperty("path.separator");

    private String family;
    private String name;
    private String version;
    private String arch;

    public Os() {}

    public Os(String family) {
        setFamily(family);
    }

    /**
     * Sets the desired OS family type
     *
     * @param f      The OS family type desired<br />
     *               Possible values:<br />
     *               <ul>
     *               <li>dos</li>
     *               <li>mac</li>
     *               <li>netware</li>
     *               <li>os/2</li>
     *               <li>unix</li>
     *               <li>windows</li>
     *               <li>win9x</li>
     *               <li>z/os</li>
     *               </ul>
     */
    public void setFamily(String f) {family = f.toLowerCase(Locale.US);}

    /**
     * Sets the desired OS name
     *
     * @param name   The OS name
     */
    public void setName(String name) {
        this.name = name.toLowerCase(Locale.US);
    }

    /**
     * Sets the desired OS architecture
     *
     * @param arch   The OS architecture
     */
    public void setArch(String arch) {
        this.arch = arch.toLowerCase(Locale.US);
    }

    /**
     * Sets the desired OS version
     *
     * @param version   The OS version
     */
    public void setVersion(String version) {
        this.version = version.toLowerCase(Locale.US);
    }

    /**
     * Determines if the OS on which Ant is executing matches the type of
     * that set in setFamily.
     * @see Os#setFamily(String)
     */
    public boolean eval() throws BuildException {
        return isOs(family, name, arch, version);
    }

    /**
     * Determines if the OS on which Ant is executing matches the
     * given OS family.
     *
     * @since 1.5
     */
    public static boolean isFamily(String family) {
        return isOs(family, null, null, null);
    }

    /**
     * Determines if the OS on which Ant is executing matches the
     * given OS name.
     *
     * @since 1.7
     */
    public static boolean isName(String name) {
        return isOs(null, name, null, null);
    }

    /**
     * Determines if the OS on which Ant is executing matches the
     * given OS architecture.
     *
     * @since 1.7
     */
    public static boolean isArch(String arch) {
        return isOs(null, null, arch, null);
    }

    /**
     * Determines if the OS on which Ant is executing matches the
     * given OS version.
     *
     * @since 1.7
     */
    public static boolean isVersion(String version) {
        return isOs(null, null, null, version);
    }

    /**
     * Determines if the OS on which Ant is executing matches the
     * given OS family, name, architecture and version
     *
     * @param family   The OS family
     * @param name   The OS name
     * @param arch   The OS architecture
     * @param version   The OS version
     *
     * @since 1.7
     */
    public static boolean isOs(String family, String name, String arch,
                               String version) {
        boolean retValue = false;

        if (family != null || name != null || arch != null
            || version != null) {

            boolean isFamily = true;
            boolean isName = true;
            boolean isArch = true;
            boolean isVersion = true;

            if (family != null) {
                if (family.equals("windows")) {
                    isFamily = osName.indexOf("windows") > -1;
                } else if (family.equals("os/2")) {
                    isFamily = osName.indexOf("os/2") > -1;
                } else if (family.equals("netware")) {
                    isFamily = osName.indexOf("netware") > -1;
                } else if (family.equals("dos")) {
                    isFamily = pathSep.equals(";") && !isFamily("netware");
                } else if (family.equals("mac")) {
                    isFamily = osName.indexOf("mac") > -1;
                } else if (family.equals("unix")) {
                    isFamily = pathSep.equals(":")
                        && (!isFamily("mac") || osName.endsWith("x"));
                } else if (family.equals("win9x")) {
                    isFamily = isFamily("windows") &&
                        !(osName.indexOf("nt") >= 0 ||
                          osName.indexOf("2000") >= 0 ||
                          osName.indexOf("xp") >= 0);
                } else if (family.equals("z/os")) {
                    isFamily = osName.indexOf("z/os") > -1
                        || osName.indexOf("os/390") > -1;
                } else {
                    throw new BuildException(
                        "Don\'t know how to detect os family \""
                        + family + "\"");
                }
            }
            if (name != null) {
                isName = name.equals(osName);
            }
            if (arch != null) {
                isArch = arch.equals(osArch);
            }
            if (version != null) {
                isVersion = version.equals(osVersion);
            }
            retValue = isFamily && isName && isArch && isVersion;
        }
        return retValue;
    }
}
