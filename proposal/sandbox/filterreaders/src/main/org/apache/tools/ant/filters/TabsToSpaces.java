package org.apache.tools.ant.filters;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Hashtable;

import org.apache.tools.ant.types.Parameter;
import org.apache.tools.ant.types.Parameterizable;

/**
 * Converts tabs to spaces.
 *
 * Example Usage:
 * =============
 * <filterreader classname="org.apache.tools.ant.filters.TabsToSpaces">
 *    <param name="tablength" value="8"/>
 * </filterreader>
 *
 * @author <a href="mailto:umagesh@apache.org">Magesh Umasankar</a>
 */
public final class TabsToSpaces
    extends FilterReader
    implements Parameterizable
{
    private static final int DEFAULT_TAB_LENGTH = 8;

    private static final String TAB_LENGTH_KEY = "tablength";

    private Parameter[] parameters;

    private boolean initialized;

    private int tabLength = DEFAULT_TAB_LENGTH;

    private int spacesRemaining = 0;

    /**
     * Create a new filtered reader.
     *
     * @param in  a Reader object providing the underlying stream.
     */
    public TabsToSpaces(final Reader in) {
        super(in);
    }

    /**
     * Convert tabs with spaces
     */
    public final int read() throws IOException {
        if (!initialized) {
            initialize();
            initialized = true;
        }

        int ch = -1;

        if (spacesRemaining > 0) {
            spacesRemaining--;
            ch = ' ';
        } else {
            ch = in.read();
            if (ch == '\t') {
                spacesRemaining = tabLength - 1;
                ch = ' ';
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

    /**
     * Initialize tokens and load the replacee-replacer hashtable.
     */
    private final void initialize() {
        if (parameters != null) {
            for (int i = 0; i < parameters.length; i++) {
                if (parameters[i] != null) {
                    if (TAB_LENGTH_KEY.equals(parameters[i].getName())) {
                        tabLength =
                            new Integer(parameters[i].getValue()).intValue();
                        break;
                    }
                }
            }
        }
    }
}
