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
package org.apache.tools.ant.listener;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Properties;

import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;

/**
 * Uses ANSI Color Code Sequences to colorize messages
 * sent to the console.
 *
 * If used with the -logfile option, the output file
 * will contain all the necessary escape codes to
 * display the text in colorized mode when displayed
 * in the console using applications like cat, more,
 * etc.
 *
 * This is designed to work on terminals that support ANSI
 * color codes.  It works on XTerm, ETerm, Mindterm, etc.
 * It also works on Win9x (with ANSI.SYS loaded.)
 *
 * NOTE:
 * It doesn't work on WinNT's COMMAND.COM even with
 * ANSI.SYS loaded.
 *
 * The default colors used for differentiating
 * the message levels can be changed by editing the
 * /org/apache/tools/ant/listener/defaults.properties
 * file.
 * This file contains 5 key/value pairs:
 * AnsiColorLogger.ERROR_COLOR=2;31
 * AnsiColorLogger.WARNING_COLOR=2;35
 * AnsiColorLogger.INFO_COLOR=2;36
 * AnsiColorLogger.VERBOSE_COLOR=2;32
 * AnsiColorLogger.DEBUG_COLOR=2;34
 *
 * Another option is to pass a system variable named
 * ant.logger.defaults, with value set to the path of
 * the file that contains user defined Ansi Color
 * Codes, to the <B>java</B> command using -D option.
 *
 * To change these colors use the following chart:
 *
 *      <B>ANSI COLOR LOGGER CONFIGURATION</B>
 *
 * Format for AnsiColorLogger.*=
 *  Attribute;Foreground;Background
 *
 *  Attribute is one of the following:
 *  0 -> Reset All Attributes (return to normal mode)
 *  1 -> Bright (Usually turns on BOLD)
 *  2 -> Dim
 *  3 -> Underline
 *  5 -> link
 *  7 -> Reverse
 *  8 -> Hidden
 *
 *  Foreground is one of the following:
 *  30 -> Black
 *  31 -> Red
 *  32 -> Green
 *  33 -> Yellow
 *  34 -> Blue
 *  35 -> Magenta
 *  36 -> Cyan
 *  37 -> White
 *
 *  Background is one of the following:
 *  40 -> Black
 *  41 -> Red
 *  42 -> Green
 *  43 -> Yellow
 *  44 -> Blue
 *  45 -> Magenta
 *  46 -> Cyan
 *  47 -> White
 *
 * @author Magesh Umasankar
 */
public final class AnsiColorLogger extends DefaultLogger {
    private static final int ATTR_NORMAL = 0;
    private static final int ATTR_BRIGHT = 1;
    private static final int ATTR_DIM = 2;
    private static final int ATTR_UNDERLINE = 3;
    private static final int ATTR_BLINK = 5;
    private static final int ATTR_REVERSE = 7;
    private static final int ATTR_HIDDEN = 8;

    private static final int FG_BLACK = 30;
    private static final int FG_RED = 31;
    private static final int FG_GREEN = 32;
    private static final int FG_YELLOW = 33;
    private static final int FG_BLUE = 34;
    private static final int FG_MAGENTA = 35;
    private static final int FG_CYAN = 36;
    private static final int FG_WHITE = 37;

    private static final int BG_BLACK = 40;
    private static final int BG_RED = 41;
    private static final int BG_GREEN = 42;
    private static final int BG_YELLOW = 44;
    private static final int BG_BLUE = 44;
    private static final int BG_MAGENTA = 45;
    private static final int BG_CYAN = 46;
    private static final int BG_WHITE = 47;

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
    private final void setColors() {
        String userColorFile = System.getProperty("ant.logger.defaults");
        String systemColorFile =
            "/org/apache/tools/ant/listener/defaults.properties";

        InputStream in = null;

        try {
            Properties prop = new Properties();

            if (userColorFile != null) {
                in = new FileInputStream(userColorFile);
            } else {
                in = getClass().getResourceAsStream(systemColorFile);
            }

            if (in != null) {
                prop.load(in);
            }

            String err = prop.getProperty("AnsiColorLogger.ERROR_COLOR");
            String warn = prop.getProperty("AnsiColorLogger.WARNING_COLOR");
            String info = prop.getProperty("AnsiColorLogger.INFO_COLOR");
            String verbose = prop.getProperty("AnsiColorLogger.VERBOSE_COLOR");
            String debug = prop.getProperty("AnsiColorLogger.DEBUG_COLOR");
            if (err != null) {
                errColor = PREFIX + err + SUFFIX;
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
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    //Ignore - We do not want this to stop the build.
                }
            }
        }
    }

    /**
     * @see DefaultLogger#printMessage
     */
    protected final void printMessage(final String message,
                                      final PrintStream stream,
                                      final int priority) {
        if (message != null && stream != null) {
            if (!colorsSet) {
                setColors();
                colorsSet = true;
            }

            final StringBuffer msg = new StringBuffer(message);
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
                    msg.insert(0, debugColor);
                    msg.append(END_COLOR);
                    break;
            }
            final String strmessage = msg.toString();
            stream.println(strmessage);
        }
    }
}
