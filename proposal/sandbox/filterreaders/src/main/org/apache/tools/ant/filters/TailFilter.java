package org.apache.tools.ant.filters;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;

import org.apache.tools.ant.types.Parameter;
import org.apache.tools.ant.types.Parameterizable;

/**
 * Read the last n lines.  Default is last 10 lines.
 *
 * Example:
 * =======
 *
 * <filterreader classname="org.apache.tools.ant.filters.TailFilter">
 *    <param name="lines" value="3"/>
 * </filterreader>
 *
 * @author <a href="mailto:umagesh@apache.org">Magesh Umasankar</a>
 */
public final class TailFilter
    extends FilterReader
    implements Parameterizable
{
    private static final String LINES_KEY = "lines";

    private Parameter[] parameters;

    private boolean initialized = false;

    private long linesRead = 0;

    private long lines = 10;

    private boolean ignoreLineFeed = false;

    private char[] buffer = new char[4096];

    private int returnedCharPos = -1;

    private boolean completedReadAhead = false;

    private int bufferPos = 0;

    /**
     * Create a new filtered reader.
     *
     * @param in  a Reader object providing the underlying stream.
     */
    public TailFilter(final Reader in) {
        super(in);
    }

    /**
     * Read ahead and keep in buffer last n lines only at any given
     * point.  Grow buffer as needed.
     */
    public final int read() throws IOException {
        if (!initialized) {
            initialize();
            initialized = true;
        }

        if (!completedReadAhead) {
            int ch = -1;
            while ((ch = in.read()) != -1) {
                if (buffer.length == bufferPos) {
                    if (returnedCharPos != -1) {
                        final char[] tmpBuffer = new char[buffer.length];
                        System.arraycopy(buffer, returnedCharPos + 1, tmpBuffer,
                                         0, buffer.length - (returnedCharPos + 1));
                        buffer = tmpBuffer;
                        bufferPos = bufferPos - (returnedCharPos + 1);
                        returnedCharPos = -1;
                    } else {
                        final char[] tmpBuffer = new char[buffer.length * 2];
                        System.arraycopy(buffer, 0, tmpBuffer, 0, bufferPos);
                        buffer = tmpBuffer;
                    }
                }

                if (ignoreLineFeed) {
                    if (ch == '\n') {
                        ch = in.read();
                    }
                    ignoreLineFeed = false;
                }

                switch (ch) {
                    case '\r':
                        ch = '\n';
                        ignoreLineFeed = true;
                        //fall through
                    case '\n':
                        linesRead++;

                        if (linesRead == lines + 1) {
                            int i = 0;
                            for (i = returnedCharPos + 1; buffer[i] != '\n'; i++) {
                            }
                            returnedCharPos = i;
                            linesRead--;
                        }
                        break;
                }
                if (ch == -1) {
                    break;
                }

                buffer[bufferPos] = (char) ch;
                bufferPos++;
            }
            completedReadAhead = true;
        }

        ++returnedCharPos;
        if (returnedCharPos >= bufferPos) {
            return -1;
        } else {
            return buffer[returnedCharPos];
        }
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

    public final long skip(long n) throws IOException {
        for (long i = 0; i < n; i++) {
            if (in.read() == -1) {
                return i;
            }
        }
        return n;
    }

    /**
     * Set Parameters
     */
    public final void setParameters(final Parameter[] parameters) {
        this.parameters = parameters;
        initialized = false;
    }

    private final void initialize() {
        if (parameters != null) {
            for (int i = 0; i < parameters.length; i++) {
                if (LINES_KEY.equals(parameters[i].getName())) {
                    lines = new Long(parameters[i].getValue()).longValue();
                    break;
                }
            }
        }
    }
}
