/*
 * Copyright 2013 National Bank of Belgium
 * 
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved 
 * by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * 
 * http://ec.europa.eu/idabc/eupl
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and 
 * limitations under the Licence.
 */
package be.nbb.demetra.dfm;

import ec.tstoolkit.utilities.Arrays2;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 *
 * @author Philippe Charles
 */
public final class Arrays3 {

    private Arrays3() {
        // static class
    }

    @Nonnull
    public static <X extends Enum<?>> String enumToString(@Nullable X[] array) {
        if (Arrays2.isNullOrEmpty(array)) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        result.append(array[0].name());
        for (int i = 1; i < array.length; i++) {
            result.append(", ").append(array[i].name());
        }
        return result.toString();
    }

    @Nullable
    public static <X> X[] cloneIfNotNull(@Nullable X[] array) {
        return array != null ? array.clone() : null;
    }

}
