# Copyright (c) 2004 The Apache Software Foundation.  All rights reserved.
for arg in $@ ; do
	echo $arg out
	sleep 1
	echo $arg err>&2
done
