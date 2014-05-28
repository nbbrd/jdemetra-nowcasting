/*
 * Copyright 2013 National Bank of Belgium
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved 
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

import ec.tstoolkit.timeseries.simplets.TsData;
import ec.tstoolkit.timeseries.simplets.TsFrequency;

/**
 *
 * @author Mats Maggi
 */
public class Data {
    private static final double[] g_prod = {
        59.2, 58.3, 63.4, 59.7, 58.9, 62.7, 47.6, 58.6, 64.4, 66.4, 64.2, 62.2, 61.7, 62.2, 65.5, 64.6, 64.6, 62.2, 53.2, 62.5, 68.5, 73.5, 67.1, 68.6,
        69.1, 65.5, 72.7, 73, 70.3, 73.5, 61.5, 67.6, 77.7, 81.7, 73.5, 75.4, 70.6, 70.8, 76.9, 77.7, 71.1, 77.3, 63.1, 70.8, 80.5, 82.7, 75.8, 79.3,
        72.3, 74, 82.7, 79.1, 74.4, 79.5, 61.9, 73.5, 83.1, 82.9, 78, 80.4, 77.7, 79, 88.1, 79.5, 80.9, 85.7, 61.2, 78.7, 87.6, 91.5, 88.5, 86.6,
        86.8, 84.7, 94.1, 86.9, 90.2, 86.1, 68.8, 86.9, 90.7, 99.6, 94.9, 88.2, 95.2, 91.9, 97.5, 96.4, 95.2, 91.8, 74.7, 86.7, 96.2, 100.6, 89.7, 85.7,
        88.5, 83.8, 86.3, 86.7, 79, 84.2, 64.6, 72.6, 88.2, 91.1, 84, 85.8, 86.1, 88, 97.6, 95.3, 89.1, 93.5, 69.4, 86, 99.1, 97.3, 92.9, 92.7,
        90.2, 89.7, 102.3, 92, 89.1, 95.2, 67, 88.1, 95.6, 94.2, 93, 92.2, 91.5, 88.9, 99.1, 93.6, 91.5, 94.6, 67.6, 89.8, 99.3, 103.7, 100.3, 94.8,
        92.2, 93.8, 103.5, 98.8, 99.2, 99.5, 75.6, 96, 102.1, 109.3, 103.3, 96.3, 104.5, 102.8, 105.8, 102.3, 93.7, 99, 73, 87.9, 100.1, 103.8, 90.9, 89.1,
        91.6, 92.5, 100.3, 97.5, 90.4, 96.4, 70.8, 86.7, 102.5, 103.7, 96.8, 93.7, 93.4, 92.5, 99.9, 99.6, 91.5, 99.7, 70.6, 88.1, 102, 101.1, 94, 92.3,
        126.8, 117.7, 126.6, 123, 118.1, 123.7, 93.5, 105.4, 125, 131.9, 119.9, 110.3, 126.2, 121.6, 130.9, 123.6, 116.1, 126.9, 95, 107.6, 128.4, 127.1, 116.3, 109.5,
        113.4, 114, 128.5, 118.3, 108.6, 124.2, 86.7, 104.2, 124.1, 121.2, 112.6, 114.1, 120.3, 117.6, 133.6, 117.7, 113.8, 126.6, 81.6, 108.7, 125.9, 123, 120.7, 109.7};

    public static final TsData P = new TsData(TsFrequency.Monthly, 1967, 0, g_prod, false);
}
