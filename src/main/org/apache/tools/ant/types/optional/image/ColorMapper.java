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
package org.apache.tools.ant.types.optional.image;

import java.awt.Color;
import java.util.Locale;

/**
 *
 * @see org.apache.tools.ant.taskdefs.optional.image.Image
 */
public final class ColorMapper {

    /** black string */
    public static final String COLOR_BLACK = "black";
    /** blue string */
    public static final String COLOR_BLUE = "blue";
    /** cyan string */
    public static final String COLOR_CYAN = "cyan";
    /** black string */
    public static final String COLOR_DARKGRAY = "darkgray";
    /** gray string */
    public static final String COLOR_GRAY = "gray";
    /** lightgray string */
    public static final String COLOR_LIGHTGRAY = "lightgray";
    // Gotta at least put in the proper spelling :-P
    /** darkgrey string */
    public static final String COLOR_DARKGREY = "darkgrey";
    /** grey string */
    public static final String COLOR_GREY = "grey";
    /** lightgrey string */
    public static final String COLOR_LIGHTGREY = "lightgrey";
    /** green string */
    public static final String COLOR_GREEN = "green";
    /** magenta string */
    public static final String COLOR_MAGENTA = "magenta";
    /** orange string */
    public static final String COLOR_ORANGE = "orange";
    /** pink string */
    public static final String COLOR_PINK = "pink";
    /** reg string */
    public static final String COLOR_RED = "red";
    /** white string */
    public static final String COLOR_WHITE = "white";
    /** yellow string */
    public static final String COLOR_YELLOW = "yellow";

    /**
     * Convert a color name to a color value.
     * @param colorName a string repr of the color.
     * @return the color value.
     * @todo refactor to use an EnumeratedAttribute (maybe?)
     */
    public static Color getColorByName(String colorName) {
        switch (colorName.toLowerCase(Locale.ENGLISH)) {
        case COLOR_BLUE:
            return Color.blue;
        case COLOR_CYAN:
            return Color.cyan;
        case COLOR_DARKGRAY:
        case COLOR_DARKGREY:
            return Color.darkGray;
        case COLOR_GRAY:
        case COLOR_GREY:
            return Color.gray;
        case COLOR_LIGHTGRAY:
        case COLOR_LIGHTGREY:
            return Color.lightGray;
        case COLOR_GREEN:
            return Color.green;
        case COLOR_MAGENTA:
            return Color.magenta;
        case COLOR_ORANGE:
            return Color.orange;
        case COLOR_PINK:
            return Color.pink;
        case COLOR_RED:
            return Color.red;
        case COLOR_WHITE:
            return Color.white;
        case COLOR_YELLOW:
            return Color.yellow;
        case COLOR_BLACK:
        default:
            return Color.black;
        }
    }

    /** private constructor for Utility class */
    private ColorMapper() {
    }

}
