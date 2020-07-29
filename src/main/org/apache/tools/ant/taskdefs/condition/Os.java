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

package org.apache.tools.ant.taskdefs.condition;

import java.io.File;
import java.util.Locale;

import org.apache.tools.ant.BuildException;

/**
 * Condition that tests the OS type.
 *
 * @since Ant 1.4
 */
public class Os implements Condition {
    private static final String OS_NAME =
        System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
    private static final String OS_ARCH =
        System.getProperty("os.arch").toLowerCase(Locale.ENGLISH);
    private static final String OS_VERSION =
        System.getProperty("os.version").toLowerCase(Locale.ENGLISH);
    private static final String PATH_SEP = File.pathSeparator;

    /**
     * OS family that can be tested for. {@value}
     */
    public static final String FAMILY_WINDOWS = "windows";
    /**
     * OS family that can be tested for. {@value}
     */
    public static final String FAMILY_9X = "win9x";
    /**
     * OS family that can be tested for. {@value}
     */
    public static final String FAMILY_NT = "winnt";
    /**
     * OS family that can be tested for. {@value}
     */
    public static final String FAMILY_OS2 = "os/2";
    /**
     * OS family that can be tested for. {@value}
     */
    public static final String FAMILY_NETWARE = "netware";
    /**
     * OS family that can be tested for. {@value}
     */
    public static final String FAMILY_DOS = "dos";
    /**
     * OS family that can be tested for. {@value}
     */
    public static final String FAMILY_MAC = "mac";
    /**
     * OS family that can be tested for. {@value}
     */
    public static final String FAMILY_TANDEM = "tandem";
    /**
     * OS family that can be tested for. {@value}
     */
    public static final String FAMILY_UNIX = "unix";
    /**
     * OS family that can be tested for. {@value}
     */
    public static final String FAMILY_VMS = "openvms";
    /**
     * OS family that can be tested for. {@value}
     */
    public static final String FAMILY_ZOS = "z/os";
    /** OS family that can be tested for. {@value} */
    public static final String FAMILY_OS400 = "os/400";

    /**
     * OpenJDK is reported to call MacOS X "Darwin"
     * @see <a href="https://issues.apache.org/bugzilla/show_bug.cgi?id=44889">Bugzilla</a>
     * @see <a href="https://issues.apache.org/jira/browse/HADOOP-3318">Jira</a>
     */
    private static final String DARWIN = "darwin";

    /**
     * OS family to look for
     */
    private String family;
    /**
     * Name of OS
     */
    private String name;
    /**
     * version of OS
     */
    private String version;
    /**
     * OS architecture
     */
    private String arch;

    /**
     * Default constructor
     *
     */
    public Os() {
        //default
    }

    /**
     * Constructor that sets the family attribute
     * @param family a String value
     */
    public Os(String family) {
        setFamily(family);
    }

    /**
     * Sets the desired OS family type
     *
     * @param f      The OS family type desired
     *               <p>Possible values:</p>
     *               <ul>
     *               <li>dos</li>
     *               <li>mac</li>
     *               <li>netware</li>
     *               <li>os/2</li>
     *               <li>tandem</li>
     *               <li>unix</li>
     *               <li>windows</li>
     *               <li>win9x</li>
     *               <li>z/os</li>
     *               <li>os/400</li>
     *               </ul>
     */
    public void setFamily(String f) {
        family = f.toLowerCase(Locale.ENGLISH);
    }

    /**
     * Sets the desired OS name
     *
     * @param name   The OS name
     */
    public void setName(String name) {
        this.name = name.toLowerCase(Locale.ENGLISH);
    }

    /**
     * Sets the desired OS architecture
     *
     * @param arch   The OS architecture
     */
    public void setArch(String arch) {
        this.arch = arch.toLowerCase(Locale.ENGLISH);
    }

    /**
     * Sets the desired OS version
     *
     * @param version   The OS version
     */
    public void setVersion(String version) {
        this.version = version.toLowerCase(Locale.ENGLISH);
    }

    /**
     * Determines if the OS on which Ant is executing matches the type of
     * that set in setFamily.
     * @return true if the os matches.
     * @throws BuildException if there is an error.
     * @see Os#setFamily(String)
     */
    @Override
    public boolean eval() throws BuildException {
        return isOs(family, name, arch, version);
    }

    /**
     * Determines if the OS on which Ant is executing matches the
     * given OS family.
     * @param family the family to check for
     * @return true if the OS matches
     * @since 1.5
     */
    public static boolean isFamily(String family) {
        return isOs(family, null, null, null);
    }

    /**
     * Determines if the OS on which Ant is executing matches the
     * given OS name.
     *
     * @param name the OS name to check for
     * @return true if the OS matches
     * @since 1.7
     */
    public static boolean isName(String name) {
        return isOs(null, name, null, null);
    }

    /**
     * Determines if the OS on which Ant is executing matches the
     * given OS architecture.
     *
     * @param arch the OS architecture to check for
     * @return true if the OS matches
     * @since 1.7
     */
    public static boolean isArch(String arch) {
        return isOs(null, null, arch, null);
    }

    /**
     * Determines if the OS on which Ant is executing matches the
     * given OS version.
     *
     * @param version the OS version to check for
     * @return true if the OS matches
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
     * @return true if the OS matches
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

                //windows probing logic relies on the word 'windows' in
                //the OS
                boolean isWindows = OS_NAME.contains(FAMILY_WINDOWS);
                boolean is9x = false;
                boolean isNT = false;
                if (isWindows) {
                    //there are only four 9x platforms that we look for
                    is9x = (OS_NAME.contains("95")
                            || OS_NAME.contains("98")
                            || OS_NAME.contains("me")
                            //wince isn't really 9x, but crippled enough to
                            //be a muchness. Ant doesn't run on CE, anyway.
                            || OS_NAME.contains("ce"));
                    isNT = !is9x;
                }
                switch (family) {
                    case FAMILY_WINDOWS:
                        isFamily = isWindows;
                        break;
                    case FAMILY_9X:
                        isFamily = isWindows && is9x;
                        break;
                    case FAMILY_NT:
                        isFamily = isWindows && isNT;
                        break;
                    case FAMILY_OS2:
                        isFamily = OS_NAME.contains(FAMILY_OS2);
                        break;
                    case FAMILY_NETWARE:
                        isFamily = OS_NAME.contains(FAMILY_NETWARE);
                        break;
                    case FAMILY_DOS:
                        isFamily = PATH_SEP.equals(";") && !isFamily(FAMILY_NETWARE);
                        break;
                    case FAMILY_MAC:
                        isFamily = OS_NAME.contains(FAMILY_MAC)
                                || OS_NAME.contains(DARWIN);
                        break;
                    case FAMILY_TANDEM:
                        isFamily = OS_NAME.contains("nonstop_kernel");
                        break;
                    case FAMILY_UNIX:
                        isFamily = PATH_SEP.equals(":")
                                && !isFamily(FAMILY_VMS)
                                && (!isFamily(FAMILY_MAC) || OS_NAME.endsWith("x")
                                || OS_NAME.contains(DARWIN));
                        break;
                    case FAMILY_ZOS:
                        isFamily = OS_NAME.contains(FAMILY_ZOS)
                                || OS_NAME.contains("os/390");
                        break;
                    case FAMILY_OS400:
                        isFamily = OS_NAME.contains(FAMILY_OS400);
                        break;
                    case FAMILY_VMS:
                        isFamily = OS_NAME.contains(FAMILY_VMS);
                        break;
                    default:
                        throw new BuildException(
                                "Don't know how to detect os family \""
                                        + family + "\"");
                }
            }
            if (name != null) {
                isName = name.equals(OS_NAME);
            }
            if (arch != null) {
                isArch = arch.equals(OS_ARCH);
            }
            if (version != null) {
                isVersion = version.equals(OS_VERSION);
            }
            retValue = isFamily && isName && isArch && isVersion;
        }
        return retValue;
    }
}
