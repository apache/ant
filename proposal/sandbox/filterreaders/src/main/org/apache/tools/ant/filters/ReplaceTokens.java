package org.apache.tools.ant.filters;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Hashtable;

import org.apache.tools.ant.types.Parameter;
import org.apache.tools.ant.types.Parameterizable;

/**
 * Replace tokens with user supplied values
 *
 * Example Usage:
 * =============
 * <filterreader classname="org.apache.tools.ant.filters.ReplaceTokens">
 *    <param type="tokenchar" name="begintoken" value="#"/>
 *    <param type="tokenchar" name="endtoken" value="#"/>
 *    <param type="token" name="DATE" value="${DATE}"/>
 * <filterreader>
 *
 * @author <a href="mailto:umagesh@apache.org">Magesh Umasankar</a>
 */
public final class ReplaceTokens
    extends FilterReader
    implements Parameterizable
{
    private static final char DEFAULT_BEGIN_TOKEN = '@';

    private static final char DEFAULT_END_TOKEN = '@';

    private String storedData = null;

    private Parameter[] parameters;

    private Hashtable hash = new Hashtable();

    private boolean initialized;

    private char beginToken = DEFAULT_BEGIN_TOKEN;

    private char endToken = DEFAULT_END_TOKEN;

    /**
     * Create a new filtered reader.
     *
     * @param in  a Reader object providing the underlying stream.
     */
    public ReplaceTokens(final Reader in) {
        super(in);
    }

    /**
     * Replace tokens with values.
     */
    public final int read() throws IOException {
        if (!initialized) {
            initialize();
            initialized = true;
        }

        if (storedData != null && storedData.length() > 0) {
            int ch = storedData.charAt(0);
            if (storedData.length() > 1) {
                storedData = storedData.substring(1);
            } else {
                storedData = null;
            }
            return ch;
        }

        int ch = in.read();
        if (ch == beginToken) {
            StringBuffer key = new StringBuffer("");
            do  {
                ch = in.read();
                if (ch != -1) {
                    key.append((char) ch);
                } else {
                    break;
                }
            } while (ch != endToken);

            if (ch == -1) {
                storedData = beginToken + key.toString();
                return read();
            } else {
                key.setLength(key.length() - 1);
                final String replaceWith = (String) hash.get(key.toString());
                if (replaceWith != null) {
                    storedData = replaceWith;
                    return read();
                } else {
                    storedData = beginToken + key.toString() + endToken;
                    return read();
                }
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
                    final String type = parameters[i].getType();
                    if ("tokenchar".equals(type)) {
                        final String name = parameters[i].getName();
                        if ("begintoken".equals(name)) {
                            beginToken = parameters[i].getValue().charAt(0);
                        } else if ("endtoken".equals(name)) {
                            endToken = parameters[i].getValue().charAt(0);
                        }
                    } else if ("token".equals(type)) {
                        final String name = parameters[i].getName();
                        final String value = parameters[i].getValue();
                        hash.put(name, value);
                    }
                }
            }
        }
    }
}
