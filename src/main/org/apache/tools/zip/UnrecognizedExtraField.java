/* 
 * Copyright  2001-2002,2004 Apache Software Foundation
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

package org.apache.tools.zip;

/**
 * Simple placeholder for all those extra fields we don't want to deal
 * with.
 *
 * <p>Assumes local file data and central directory entries are
 * identical - unless told the opposite.</p>
 *
 * @author Stefan Bodewig
 * @version $Revision$
 */
public class UnrecognizedExtraField implements ZipExtraField {

    /**
     * The Header-ID.
     *
     * @since 1.1
     */
    private ZipShort headerId;

    public void setHeaderId(ZipShort headerId) {
        this.headerId = headerId;
    }

    public ZipShort getHeaderId() {
        return headerId;
    }

    /**
     * Extra field data in local file data - without
     * Header-ID or length specifier.
     *
     * @since 1.1
     */
    private byte[] localData;

    public void setLocalFileDataData(byte[] data) {
        localData = data;
    }

    public ZipShort getLocalFileDataLength() {
        return new ZipShort(localData.length);
    }

    public byte[] getLocalFileDataData() {
        return localData;
    }

    /**
     * Extra field data in central directory - without
     * Header-ID or length specifier.
     *
     * @since 1.1
     */
    private byte[] centralData;

    public void setCentralDirectoryData(byte[] data) {
        centralData = data;
    }

    public ZipShort getCentralDirectoryLength() {
        if (centralData != null) {
            return new ZipShort(centralData.length);
        }
        return getLocalFileDataLength();
    }

    public byte[] getCentralDirectoryData() {
        if (centralData != null) {
            return centralData;
        }
        return getLocalFileDataData();
    }

    public void parseFromLocalFileData(byte[] data, int offset, int length) {
        byte[] tmp = new byte[length];
        System.arraycopy(data, offset, tmp, 0, length);
        setLocalFileDataData(tmp);
    }
}
