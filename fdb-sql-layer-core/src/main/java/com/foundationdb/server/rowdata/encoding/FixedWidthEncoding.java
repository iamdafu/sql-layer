/**
 * Copyright (C) 2009-2013 FoundationDB, LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.foundationdb.server.rowdata.encoding;

import com.foundationdb.ais.model.Column;
import com.foundationdb.server.rowdata.FieldDef;

abstract class FixedWidthEncoding implements Encoding {
    @Override
    public int widthFromObject(FieldDef fieldDef, Object value) {
        return fieldDef.getMaxStorageSize();
    }

    @Override
    public long getMaxKeyStorageSize(Column column) {
        return maxKeyStorageSize;
    }

    FixedWidthEncoding(long maxKeyStorageSize) {
        this.maxKeyStorageSize = maxKeyStorageSize;
    }

    private final long maxKeyStorageSize;
}
