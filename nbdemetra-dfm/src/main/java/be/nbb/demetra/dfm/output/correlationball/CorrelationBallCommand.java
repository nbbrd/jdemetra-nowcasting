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
package be.nbb.demetra.dfm.output.correlationball;

import ec.tstoolkit.design.UtilityClass;
import ec.ui.commands.ComponentCommand;
import ec.util.various.swing.JCommand;
import javax.annotation.Nonnull;

/**
 *
 * @author Mats Maggi
 */
@UtilityClass(CorrelationBall.class)
public class CorrelationBallCommand {
    
    @Nonnull
    public static JCommand<CorrelationBall> applyColorScale(double scale) {
        return new ColorScaleCommand(scale);
    }
    
    public static final class ColorScaleCommand extends ComponentCommand<CorrelationBall> {

        private final double colorScale;

        public ColorScaleCommand(double colorScale) {
            super(CorrelationBall.COLOR_SCALE_PROPERTY);
            this.colorScale = colorScale;
        }

        @Override
        public boolean isSelected(CorrelationBall component) {
            return colorScale == component.getColorScale();
        }

        @Override
        public void execute(CorrelationBall component) throws Exception {
            component.setColorScale(colorScale);
        }
    }
}
