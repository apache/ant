package org.apache.tools.ant.filters;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;

import org.apache.tools.ant.types.Parameter;
import org.apache.tools.ant.types.Parameterizable;

/**
 * Read the first n lines (Default is first 10 lines)
 *
 * Example:
 * =======
 *
 * <filterreader classname="org.apache.tools.ant.filters.HeadFilter">
 *    <param name="lines" value="3"/>
 * </filterreader>
 *
 * @author <a href="mailto:umagesh@apache.org">Magesh Umasankar</a>
 */
public final class HeadFilter
    extends FilterReader
    implements Parameterizable
{
    private static final String LINES_KEY = "lines";

    private Parameter[] parameters;

    private boolean initialized = false;

    private long linesRead = 0;

    private long lines = 10;

    private boolean ignoreLineFeed = false;

    /**
     * Create a new filtered reader.
     *
     * @param in  a Reader object providing the underlying stream.
     */
    public HeadFilter(final Reader in) {
        super(in);
    }

    public final int read() throws IOException {
        if (!initialized) {
            initialize();
            initialized = true;
        }

        int ch = -1;

        if (linesRead < lines) {

            ch = in.read();

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
                    break;
            }
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
