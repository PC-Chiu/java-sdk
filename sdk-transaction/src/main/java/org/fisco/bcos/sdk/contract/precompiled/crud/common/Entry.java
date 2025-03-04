/*
 * Copyright 2014-2020  [fisco-dev]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package org.fisco.bcos.sdk.contract.precompiled.crud.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.fisco.bcos.sdk.contract.precompiled.crud.KVTablePrecompiled;
import org.fisco.bcos.sdk.contract.precompiled.crud.TablePrecompiled;

public class Entry {
    private Map<String, String> fieldNameToValue = new HashMap<>();

    public Entry() {}

    public Entry(Map<String, String> fieldNameToValue) {
        this.fieldNameToValue = fieldNameToValue;
    }

    @Deprecated
    public Entry(TablePrecompiled.Entry entry) {
        for (TablePrecompiled.KVField field : entry.fields.getValue()) {
            this.fieldNameToValue.put(field.key, field.value);
        }
    }

    public Entry(KVTablePrecompiled.Entry entry) {
        for (KVTablePrecompiled.KVField field : entry.fields.getValue()) {
            this.fieldNameToValue.put(field.key, field.value);
        }
    }

    public Map<String, String> getFieldNameToValue() {
        return fieldNameToValue;
    }

    @Deprecated
    public TablePrecompiled.Entry getTablePrecompiledEntry() {
        List<TablePrecompiled.KVField> fields = new ArrayList<>();
        fieldNameToValue.forEach(
                (String k, String v) -> {
                    TablePrecompiled.KVField kvField = new TablePrecompiled.KVField(k, v);
                    fields.add(kvField);
                });
        return new TablePrecompiled.Entry(fields);
    }

    public KVTablePrecompiled.Entry getKVPrecompiledEntry() {
        List<KVTablePrecompiled.KVField> fields = new ArrayList<>();
        fieldNameToValue.forEach(
                (String k, String v) -> {
                    KVTablePrecompiled.KVField kvField = new KVTablePrecompiled.KVField(k, v);
                    fields.add(kvField);
                });
        return new KVTablePrecompiled.Entry(fields);
    }

    public void setFieldNameToValue(Map<String, String> fieldNameToValue) {
        this.fieldNameToValue = fieldNameToValue;
    }
}
