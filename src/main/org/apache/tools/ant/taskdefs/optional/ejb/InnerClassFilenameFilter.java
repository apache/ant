package org.apache.tools.ant.taskdefs.optional.ejb;

import java.io.*;

public class InnerClassFilenameFilter implements FilenameFilter {
	private String baseClassName;

	InnerClassFilenameFilter(String baseclass){
		int extidx = baseclass.lastIndexOf(".class");
		if (extidx == -1) {
			extidx = baseclass.length()-1;
		}
		baseClassName = baseclass.substring(0,extidx);
	}

	public boolean accept (File Dir, String filename){
        	if ((filename.lastIndexOf(".") != filename.lastIndexOf(".class")) 
		    || (filename.indexOf(baseClassName + "$") != 0)) {
				return false;
		}
		return true;
	}
}
