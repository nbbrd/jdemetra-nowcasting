/*
 * Copyright 2014 National Bank of Belgium
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
package ec.tss.Dfm;

import ec.tstoolkit.algorithm.IProcResults;
import ec.tstoolkit.dfm.DfmInformationSet;
import ec.tstoolkit.dfm.DynamicFactorModel;
import ec.tstoolkit.eco.Likelihood;
import ec.tstoolkit.information.InformationMapper;
import ec.tstoolkit.information.InformationSet;
import ec.tstoolkit.maths.matrices.Matrix;
import ec.tstoolkit.mssf2.MFilteringResults;
import ec.tstoolkit.mssf2.MSmoothingResults;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author Jean Palate
 */
public class DfmResults implements IProcResults {

    private final DynamicFactorModel model;
    private final DfmInformationSet input;
    // optimization (if any)
    private Likelihood likelihood;
    private Matrix hessian;
    private double[] gradient;
    // smoothing/filtering
    private MSmoothingResults smoothing;
    private MFilteringResults filtering;

    public DfmResults(DynamicFactorModel model, DfmInformationSet input) {
        this.model = model;
        this.input = input;
    }

    public DynamicFactorModel getModel() {
        return model;
    }

    public DfmInformationSet getInput() {
        return input;
    }

    @Override
    public Map<String, Class> getDictionary() {
        return dictionary();
    }

    @Override
    public <T> T getData(String id, Class<T> tclass) {
        if (InformationSet.isPrefix(id, MODEL)) {
            return model.getData(InformationSet.removePrefix(id), tclass);
        } else {
            return mapper.getData(this, id, tclass);
        }
    }

    @Override
    public boolean contains(String id) {
        if (InformationSet.isPrefix(id, MODEL)) {
            return model.contains(InformationSet.removePrefix(id));
        } else {
            return mapper.contains(id);
        }
    }

    public static void fillDictionary(String prefix, Map<String, Class> map) {
        mapper.fillDictionary(prefix, map);
        DynamicFactorModel.fillDictionary(MODEL, map);
    }

    public static Map<String, Class> dictionary() {
        LinkedHashMap<String, Class> map = new LinkedHashMap<>();
        fillDictionary(null, map);
        return map;
    }

    public static <T> void addMapping(String name, InformationMapper.Mapper<DfmResults, T> mapping) {
        synchronized (mapper) {
            mapper.add(name, mapping);
        }
    }

    private static final InformationMapper<DfmResults> mapper = new InformationMapper<>();

    public static final String MODEL = "model", NLAGS = "nlags", NFACTORS = "nfactors", VPARAMS = "vparams", VCOVAR = "vcovar",
            MVARS = "mvars", MCOEFFS = "mcoeffs";

    static {
        mapper.add(InformationSet.item(MODEL, NLAGS), new InformationMapper.Mapper<DfmResults, Integer>(Integer.class) {
            @Override
            public Integer retrieve(DfmResults source) {
                return source.model.getTransition().nlags;
            }
        });
    }

}
