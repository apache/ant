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

package org.apache.tools.zip;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;

/**
 * Static helper functions for robustly encoding filenames in zip files. 
 */
abstract class ZipEncodingHelper {

    /**
     * Grow a byte buffer, so it has a minimal capacity or at least
     * the double capacity of the original buffer 
     * 
     * @param b The original buffer.
     * @param newCapacity The minimal requested new capacity.
     * @return A byte buffer <code>r</code> with
     *         <code>r.capacity() = max(b.capacity()*2,newCapacity)</code> and
     *         all the data contained in <code>b</code> copied to the beginning
     *         of <code>r</code>.
     *
     */
    static ByteBuffer growBuffer(ByteBuffer b, int newCapacity) {
        b.limit(b.position());
        b.rewind();

        int c2 = b.capacity() * 2;
        ByteBuffer on = ByteBuffer.allocate(c2 < newCapacity ? newCapacity : c2);

        on.put(b);
        return on;
    }


    /**
     * The hexadecimal digits <code>0,...,9,A,...,F</code> encoded as
     * ASCII bytes.
     */
    private static final byte[] HEX_DIGITS =
        new byte [] {
        0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x41,
        0x42, 0x43, 0x44, 0x45, 0x46
    };

    /**
     * Encode a filename or a comment to a byte array suitable for
     * storing it to a serialized zip entry.
     * 
     * Examples (in pseudo-notation, right hand side is C-style notation):
     * <pre>
     *  encodeName("\u20AC_for_Dollar.txt","CP437") = "%U20AC_for_Dollar.txt"
     *  encodeName("\u00D6lf\u00E4sser.txt","CP437") = "\231lf\204sser.txt"
     * </pre>
     * 
     * @param name The filename or comment with possible non-ASCII
     * unicode characters.  Must not be null.
     * @param encoding A valid encoding name. The standard zip
     *                 encoding is <code>"CP437"</code>,
     *                 <code>"UTF-8"</code> is supported in ZIP file
     *                 version <code>6.3</code> or later.  If null,
     *                 will use the platform's {@link
     *                 java.lang.String#getBytes default encoding}.
     * @return A byte array containing the mapped file
     *         name. Unmappable characters or malformed character
     *         sequences are mapped to a sequence of utf-16 words
     *         encoded in the format <code>%Uxxxx</code>.
     */
    static final byte[] encodeName(String name, String encoding) {
        if (encoding == null) {
            return name.getBytes();
        }

        Charset cs = Charset.forName(encoding);
        CharsetEncoder enc = cs.newEncoder();

        enc.onMalformedInput(CodingErrorAction.REPORT);
        enc.onUnmappableCharacter(CodingErrorAction.REPORT);

        CharBuffer cb = CharBuffer.wrap(name);
        ByteBuffer out = ByteBuffer.allocate(name.length()
                                             + (name.length() + 1) / 2);

        while (cb.remaining() > 0) {
            CoderResult res = enc.encode(cb, out,true);

            if (res.isUnmappable() || res.isMalformed()) {

                // write the unmappable characters in utf-16
                // pseudo-URL encoding style to ByteBuffer.
                if (res.length() * 6 > out.remaining()) {
                    out = growBuffer(out,out.position() + res.length() * 6);
                }

                for (int i=0; i<res.length(); ++i) {
                    out.put((byte) '%');
                    out.put((byte) 'U');

                    char c = cb.get();

                    out.put(HEX_DIGITS[(c >> 12)&0x0f]);
                    out.put(HEX_DIGITS[(c >> 8)&0x0f]);
                    out.put(HEX_DIGITS[(c >> 4)&0x0f]);
                    out.put(HEX_DIGITS[c & 0x0f]);
                }

            } else if (res.isOverflow()) {

                out = growBuffer(out, 0);

            } else if (res.isUnderflow()) {

                enc.flush(out);
                break;

            }
        }

        byte [] ret = new byte[out.position()];
        out.rewind();
        out.get(ret);

        return ret;
    }

    /**
     * Return, whether a filename or a comment may be encoded to a
     * byte array suitable for storing it to a serialized zip entry
     * without any losses.
     * 
     * Examples (in pseudo-notation, right hand side is C-style notation):
     * <pre>
     *  canEncodeName("\u20AC_for_Dollar.txt","CP437") = false
     *  canEncodeName("\u20AC_for_Dollar.txt","UTF-8") = true
     *  canEncodeName("\u00D6lf\u00E4sser.txt","CP437") = true
     * </pre>
     * 
     * @param name The filename or comment with possible non-ASCII
     * unicode characters.
     * @param encoding A valid encoding name. The standard zip
     *                 encoding is <code>"CP437"</code>,
     *                 <code>"UTF-8"</code> is supported in ZIP file
     *                 version <code>6.3</code> or later.
     * @return Whether the given encoding may encode the given name.
     */
    static final boolean canEncodeName(String name, String encoding) {

        Charset cs = Charset.forName(encoding);

        CharsetEncoder enc = cs.newEncoder();
        enc.onMalformedInput(CodingErrorAction.REPORT);
        enc.onUnmappableCharacter(CodingErrorAction.REPORT);

        return enc.canEncode(name);
    }

    /**
     * Decode a filename or a comment from a byte array.
     * 
     * @param name The filename or comment.
     * @param encoding A valid encoding name. The standard zip
     *                 encoding is <code>"CP437"</code>,
     *                 <code>"UTF-8"</code> is supported in ZIP file
     *                 version <code>6.3</code> or later.
     */
    static final String decodeName(byte[] name, String encoding)
        throws java.nio.charset.CharacterCodingException {
        Charset cs = Charset.forName(encoding);
        return cs.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(name)).toString();
    }
}
