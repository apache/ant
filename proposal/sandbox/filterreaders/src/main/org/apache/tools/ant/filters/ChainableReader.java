package org.apache.tools.ant.filters;

import java.io.Reader;

public interface ChainableReader {
    public Reader chain(Reader rdr);
}
