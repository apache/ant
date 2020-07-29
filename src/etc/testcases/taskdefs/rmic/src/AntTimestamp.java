/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
import java.rmi.RemoteException;
import java.util.Calendar;

import org.apache.tools.ant.util.DateUtils;

/**
 * This class imports a dependency on the Ant runtime classes,
 * so tests that classpath setup include them
 */
public class AntTimestamp implements RemoteTimestamp {

    /**
     * return the phase of the moon.
     * Note the completely different semantics of the other implementation,
     * which goes to show why signature is an inadequate way of verifying
     * how well an interface is implemented.
     *
     * @return the phase of the moon
     * @throws RemoteException hopefully never
     */
    public long when() throws RemoteException {
        Calendar cal = Calendar.getInstance();
        return DateUtils.getPhaseOfMoon(cal);
    }
}
