package org.apache.tools.ant.filters;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;

/**
 * This is a java comment and string stripper reader that filters
 * these lexical tokens out for purposes of simple Java parsing.
 * (if you have more complex Java parsing needs, use a real lexer).
 * Since this class heavily relies on the single char read function,
 * you are reccomended to make it work on top of a buffered reader.
 */
public final class StripJavaComments extends FilterReader {

    /**
     * Create a new filtered reader.
     *
     * @param in  a Reader object providing the underlying stream.
     */
    public StripJavaComments(final Reader in) {
        super(in);
    }

    public final int read() throws IOException {
        int ch = in.read();
        if (ch == '/') {
            ch = in.read();
            if (ch == '/') {
                while (ch != '\n' && ch != -1) {
                    ch = in.read();
                }
            } else if (ch == '*') {
                while (ch != -1) {
                    ch = in.read();
                    if (ch == '*') {
                        ch = in.read();
                        while (ch == '*' && ch != -1) {
                            ch = in.read();
                        }

                        if (ch == '/') {
                            ch = read();
                            break;
                        }
                    }
                }
            }
        }

        if (ch == '"') {
            while (ch != -1) {
                ch = in.read();
                if (ch == '\\') {
                    ch = in.read();
                } else if (ch == '"') {
                    ch = read();
                    break;
                }
            }
        }

        if (ch == '\'') {
            ch = in.read();
            if (ch == '\\') {
                ch = in.read();
            }
            ch = in.read();
            ch = read();
        }

        return ch;
    }

    public final int read(final char cbuf[], final int off,
                          final int len) throws IOException {
        for (int i = 0; i < len; i++) {
            final int ch = read();
            if (ch == -1) {
                if (i == 0) {
                    return -1;
                } else {
                    return i;
                }
            }
            cbuf[off + i] = (char) ch;
        }
        return len;
    }

    public final long skip(final long n) throws IOException {
        for (long i = 0; i < n; i++) {
            if (in.read() == -1) return i;
        }
        return n;
    }
}
