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
package org.apache.tools.ant.util;

/**
 * BASE 64 encoding of a String or an array of bytes.
 *
 * Based on RFC 1421.
 *
 **/
public class Base64Converter {

    private static final int BYTE      = 8;
    private static final int WORD      = 16;
    private static final int BYTE_MASK = 0xFF;
    private static final int POS_0_MASK = 0x0000003F;
    private static final int POS_1_MASK = 0x00000FC0;
    private static final int POS_1_SHIFT = 6;
    private static final int POS_2_MASK = 0x0003F000;
    private static final int POS_2_SHIFT = 12;
    private static final int POS_3_MASK = 0x00FC0000;
    private static final int POS_3_SHIFT = 18;


    private static final char[] ALPHABET = {
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H',  //  0 to  7
        'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',  //  8 to 15
        'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',  // 16 to 23
        'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f',  // 24 to 31
        'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',  // 32 to 39
        'o', 'p', 'q', 'r', 's', 't', 'u', 'v',  // 40 to 47
        'w', 'x', 'y', 'z', '0', '1', '2', '3',  // 48 to 55
        '4', '5', '6', '7', '8', '9', '+', '/'}; // 56 to 63

    // CheckStyle:ConstantNameCheck OFF - bc
    /** Provided for BC purposes */
    public static final char[] alphabet = ALPHABET; //NOSONAR
    // CheckStyle:ConstantNameCheck ON


    /**
     * Encode a string into base64 encoding.
     * @param s the string to encode.
     * @return the encoded string.
     */
    public String encode(String s) {
        return encode(s.getBytes());
    }

    /**
     * Encode a byte array into base64 encoding.
     * @param octetString the byte array to encode.
     * @return the encoded string.
     */
    public String encode(byte[] octetString) {
        int bits24;
        int bits6;

        // CheckStyle:MagicNumber OFF
        char[] out = new char[((octetString.length - 1) / 3 + 1) * 4];
        // CheckStyle:MagicNumber ON
        int outIndex = 0;
        int i = 0;

        // CheckStyle:MagicNumber OFF
        while ((i + 3) <= octetString.length) {
        // CheckStyle:MagicNumber ON
            // store the octets
            bits24 = (octetString[i++] & BYTE_MASK) << WORD;
            bits24 |= (octetString[i++] & BYTE_MASK) << BYTE;
            bits24 |= octetString[i++] & BYTE_MASK;

            bits6 = (bits24 & POS_3_MASK) >> POS_3_SHIFT;
            out[outIndex++] = ALPHABET[bits6];
            bits6 = (bits24 & POS_2_MASK) >> POS_2_SHIFT;
            out[outIndex++] = ALPHABET[bits6];
            bits6  = (bits24 & POS_1_MASK) >> POS_1_SHIFT;
            out[outIndex++] = ALPHABET[bits6];
            bits6 = (bits24 & POS_0_MASK);
            out[outIndex++] = ALPHABET[bits6];
        }
        if (octetString.length - i == 2) {
            // store the octets
            bits24 = (octetString[i] & BYTE_MASK) << WORD;
            bits24 |= (octetString[i + 1] & BYTE_MASK) << BYTE;
            bits6 = (bits24 & POS_3_MASK) >> POS_3_SHIFT;
            out[outIndex++] = ALPHABET[bits6];
            bits6 = (bits24 & POS_2_MASK) >> POS_2_SHIFT;
            out[outIndex++] = ALPHABET[bits6];
            bits6 = (bits24 & POS_1_MASK) >> POS_1_SHIFT;
            out[outIndex++] = ALPHABET[bits6];

            // padding
            out[outIndex++] = '=';
        } else if (octetString.length - i == 1) {
            // store the octets
            bits24 = (octetString[i] & BYTE_MASK) << WORD;
            bits6 = (bits24 & POS_3_MASK) >> POS_3_SHIFT;
            out[outIndex++] = ALPHABET[bits6];
            bits6 = (bits24 & POS_2_MASK) >> POS_2_SHIFT;
            out[outIndex++] = ALPHABET[bits6];

            // padding
            out[outIndex++] = '=';
            out[outIndex++] = '=';
        }
        return new String(out);
    }
}
