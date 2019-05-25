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
package org.apache.tools.ant.listener;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.FileUtils;

/**
 * Uses ANSI Color Code Sequences to colorize messages
 * sent to the console.
 *
 * <p>If used with the -logfile option, the output file
 * will contain all the necessary escape codes to
 * display the text in colorized mode when displayed
 * in the console using applications like cat, more,
 * etc.</p>
 *
 * <p>This is designed to work on terminals that support ANSI
 * color codes.  It works on XTerm, ETerm, Mindterm, etc.
 * It also works on Win9x (with ANSI.SYS loaded.)</p>
 *
 * <p>NOTE:
 * It doesn't work on WinNT's COMMAND.COM even with
 * ANSI.SYS loaded.</p>
 *
 * <p>The default colors used for differentiating
 * the message levels can be changed by editing the
 * /org/apache/tools/ant/listener/defaults.properties
 * file.
 * This file contains 5 key/value pairs:</p>
 * <pre>
 * AnsiColorLogger.ERROR_COLOR=2;31
 * AnsiColorLogger.WARNING_COLOR=2;35
 * AnsiColorLogger.INFO_COLOR=2;36
 * AnsiColorLogger.VERBOSE_COLOR=2;32
 * AnsiColorLogger.DEBUG_COLOR=2;34
 * </pre>
 *
 * <p>Another option is to pass a system variable named
 * ant.logger.defaults, with value set to the path of
 * the file that contains user defined Ansi Color
 * Codes, to the <B>java</B> command using -D option.</p>
 *
 * To change these colors use the following chart:
 *
 *      <h2>ANSI COLOR LOGGER CONFIGURATION</h2>
 *
 * Format for AnsiColorLogger.*=
 *  Attribute;Foreground;Background
 *
 *  Attribute is one of the following: <pre>
 *  0 -&gt; Reset All Attributes (return to normal mode)
 *  1 -&gt; Bright (Usually turns on BOLD)
 *  2 -&gt; Dim
 *  3 -&gt; Underline
 *  5 -&gt; link
 *  7 -&gt; Reverse
 *  8 -&gt; Hidden
 *  </pre>
 *
 *  Foreground is one of the following:<pre>
 *  30 -&gt; Black
 *  31 -&gt; Red
 *  32 -&gt; Green
 *  33 -&gt; Yellow
 *  34 -&gt; Blue
 *  35 -&gt; Magenta
 *  36 -&gt; Cyan
 *  37 -&gt; White
 *  </pre>
 *
 *  Background is one of the following:<pre>
 *  40 -&gt; Black
 *  41 -&gt; Red
 *  42 -&gt; Green
 *  43 -&gt; Yellow
 *  44 -&gt; Blue
 *  45 -&gt; Magenta
 *  46 -&gt; Cyan
 *  47 -&gt; White
 *  </pre>
 */
public class AnsiColorLogger extends DefaultLogger {
    // private static final int ATTR_NORMAL = 0;
    // private static final int ATTR_BRIGHT = 1;
    private static final int ATTR_DIM = 2;
    // private static final int ATTR_UNDERLINE = 3;
    // private static final int ATTR_BLINK = 5;
    // private static final int ATTR_REVERSE = 7;
    // private static final int ATTR_HIDDEN = 8;

    // private static final int FG_BLACK = 30;
    private static final int FG_RED = 31;
    private static final int FG_GREEN = 32;
    // private static final int FG_YELLOW = 33;
    private static final int FG_BLUE = 34;
    private static final int FG_MAGENTA = 35;
    private static final int FG_CYAN = 36;
    // private static final int FG_WHITE = 37;

    // private static final int BG_BLACK = 40;
    // private static final int BG_RED = 41;
    // private static final int BG_GREEN = 42;
    // private static final int BG_YELLOW = 44;
    // private static final int BG_BLUE = 44;
    // private static final int BG_MAGENTA = 45;
    // private static final int BG_CYAN = 46;
    // private static final int BG_WHITE = 47;

    private static final String PREFIX = "\u001b[";
    private static final String SUFFIX = "m";
    private static final char SEPARATOR = ';';
    private static final String END_COLOR = PREFIX + SUFFIX;

    private String errColor
        = PREFIX + ATTR_DIM + SEPARATOR + FG_RED + SUFFIX;
    private String warnColor
        = PREFIX + ATTR_DIM + SEPARATOR + FG_MAGENTA + SUFFIX;
    private String infoColor
        = PREFIX + ATTR_DIM + SEPARATOR + FG_CYAN + SUFFIX;
    private String verboseColor
        = PREFIX + ATTR_DIM + SEPARATOR + FG_GREEN + SUFFIX;
    private String debugColor
        = PREFIX + ATTR_DIM + SEPARATOR + FG_BLUE + SUFFIX;

    private boolean colorsSet = false;

    /**
     * Set the colors to use from a property file specified by the
     * special ant property ant.logger.defaults
     */
    private void setColors() {
        String userColorFile = System.getProperty("ant.logger.defaults");
        String systemColorFile =
            "/org/apache/tools/ant/listener/defaults.properties";

        InputStream in = null;

        try {
            Properties prop = new Properties();

            if (userColorFile != null) {
                in = Files.newInputStream(Paths.get(userColorFile));
            } else {
                in = getClass().getResourceAsStream(systemColorFile);
            }

            if (in != null) {
                prop.load(in);
            }

            String errC = prop.getProperty("AnsiColorLogger.ERROR_COLOR");
            String warn = prop.getProperty("AnsiColorLogger.WARNING_COLOR");
            String info = prop.getProperty("AnsiColorLogger.INFO_COLOR");
            String verbose = prop.getProperty("AnsiColorLogger.VERBOSE_COLOR");
            String debug = prop.getProperty("AnsiColorLogger.DEBUG_COLOR");
            if (errC != null) {
                errColor = PREFIX + errC + SUFFIX;
            }
            if (warn != null) {
                warnColor = PREFIX + warn + SUFFIX;
            }
            if (info != null) {
                infoColor = PREFIX + info + SUFFIX;
            }
            if (verbose != null) {
                verboseColor = PREFIX + verbose + SUFFIX;
            }
            if (debug != null) {
                debugColor = PREFIX + debug + SUFFIX;
            }
        } catch (IOException ioe) {
            //Ignore - we will use the defaults.
        } finally {
            FileUtils.close(in);
        }
    }

    /**
     * @see DefaultLogger#printMessage
     * {@inheritDoc}.
     */
    @Override
    protected void printMessage(final String message,
                                final PrintStream stream,
                                final int priority) {
        if (message != null && stream != null) {
            if (!colorsSet) {
                setColors();
                colorsSet = true;
            }

            final StringBuilder msg = new StringBuilder(message);
            switch (priority) {
                case Project.MSG_ERR:
                    msg.insert(0, errColor);
                    msg.append(END_COLOR);
                    break;
                case Project.MSG_WARN:
                    msg.insert(0, warnColor);
                    msg.append(END_COLOR);
                    break;
                case Project.MSG_INFO:
                    msg.insert(0, infoColor);
                    msg.append(END_COLOR);
                    break;
                case Project.MSG_VERBOSE:
                    msg.insert(0, verboseColor);
                    msg.append(END_COLOR);
                    break;
                case Project.MSG_DEBUG:
                    // Fall through
                default:
                    msg.insert(0, debugColor);
                    msg.append(END_COLOR);
                    break;
            }
            final String strmessage = msg.toString();
            stream.println(strmessage);
        }
    }
}
