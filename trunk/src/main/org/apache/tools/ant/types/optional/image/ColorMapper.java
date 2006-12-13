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
package org.apache.tools.ant.types.optional.image;

import java.awt.Color;

/**
 *
 * @see org.apache.tools.ant.taskdefs.optional.image.Image
 */
public final class ColorMapper {
    /** private constructor for Utility class */
    private ColorMapper() {
    }

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
    // Gotta atleast put in the proper spelling :-P
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
        colorName = colorName.toLowerCase();

        if (colorName.equals(COLOR_BLACK)) {
            return Color.black;
        } else if (colorName.equals(COLOR_BLUE)) {
            return Color.blue;
        } else if (colorName.equals(COLOR_CYAN)) {
            return Color.cyan;
        } else if (colorName.equals(COLOR_DARKGRAY) || colorName.equals(COLOR_DARKGREY)) {
            return Color.darkGray;
        } else if (colorName.equals(COLOR_GRAY) || colorName.equals(COLOR_GREY)) {
            return Color.gray;
        } else if (colorName.equals(COLOR_LIGHTGRAY) || colorName.equals(COLOR_LIGHTGREY)) {
            return Color.lightGray;
        } else if (colorName.equals(COLOR_GREEN)) {
            return Color.green;
        } else if (colorName.equals(COLOR_MAGENTA)) {
            return Color.magenta;
        } else if (colorName.equals(COLOR_ORANGE)) {
            return Color.orange;
        } else if (colorName.equals(COLOR_PINK)) {
            return Color.pink;
        } else if (colorName.equals(COLOR_RED)) {
            return Color.red;
        } else if (colorName.equals(COLOR_WHITE)) {
            return Color.white;
        } else if (colorName.equals(COLOR_YELLOW)) {
            return Color.yellow;
        }
        return Color.black;
    }

}
