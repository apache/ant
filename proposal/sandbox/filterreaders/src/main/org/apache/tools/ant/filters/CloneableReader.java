package org.apache.tools.ant.filters;

import java.io.Reader;

public interface CloneableReader {
    public Reader clone(Reader rdr);
}
