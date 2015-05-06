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

import ec.nbdemetra.ui.awt.ListenableBean;

/**
 *
 * @author Philippe Charles
 * @author Mats Maggi
 */
public final class DfmController extends ListenableBean {

    public enum DfmState {

        READY, STARTED, DONE, FAILED, CANCELLING, CANCELLED;
        
        public boolean isFinished(){
            return this == DONE || this == CANCELLED || this == FAILED;
        }
    };

    public static final String DFM_STATE_PROPERTY = "dfmState";
    public static final String SIMULATION_STATE_PROPERTY = "simulationState";

    private DfmState dfmState;
    private DfmState simulationState;

    public DfmController() {
        this.dfmState = DfmState.READY;
        this.simulationState = DfmState.READY;
    }

    public DfmState getDfmState() {
        return dfmState;
    }   

    public void setDfmState(DfmState state) {
        this.dfmState = state;
        firePropertyChange(DFM_STATE_PROPERTY, null, this.dfmState); // force refreshing in all cases
    }
    
    public DfmState getSimulationState() {
        return simulationState;
    }

    public void setSimulationState(DfmState simulationState) {
        this.simulationState = simulationState;
        firePropertyChange(SIMULATION_STATE_PROPERTY, null, this.simulationState); // force refreshing in all cases
    }
    
    
}
