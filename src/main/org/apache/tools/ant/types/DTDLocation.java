/* 
 * Copyright  2002,2004 Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * 
 */
package org.apache.tools.ant.types;

/**
 * <p>Helper class to handle the DTD nested element.  Instances of
 * this class correspond to the <code>PUBLIC</code> catalog entry type
 * of the <a
 * href="http://oasis-open.org/committees/entity/spec-2001-08-06.html">
 * OASIS "Open Catalog" standard</a>.</p>
 *
 * <p>Possible Future Enhancement: Bring the Ant element name into
 * conformance with the OASIS standard.</p>
 *
 * @see org.apache.xml.resolver.Catalog
 * @author Conor MacNeill
 * @author dIon Gillard
 * @author <a href="mailto:cstrong@arielpartners.com">Craeg Strong</a>
 * @version $Id$
 */
public class DTDLocation extends ResourceLocation {
}
