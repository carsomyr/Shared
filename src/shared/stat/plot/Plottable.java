/**
 * <p>
 * Copyright (C) 2009 The Regents of the University of California<br />
 * All rights reserved.
 * </p>
 * <p>
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 * </p>
 * <ul>
 * <li>Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer.</li>
 * <li>Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials provided with the distribution.</li>
 * <li>Neither the name of the author nor the names of any contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.</li>
 * </ul>
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * </p>
 */

package shared.stat.plot;

import shared.array.RealArray;
import shared.stat.plot.Plot.AxisType;

/**
 * Defines something as suitable for having its values plotted.
 */
public interface Plottable {

    /**
     * Gets the datasets.
     */
    public RealArray[] getDatasets();

    /**
     * Gets the title.
     */
    public String getTitle();

    /**
     * Gets the axis title.
     * 
     * @param axisType
     *            the {@link AxisType}.
     */
    public String getAxisTitle(AxisType axisType);

    /**
     * Gets the axis range.
     * 
     * @param axisType
     *            the {@link AxisType}.
     */
    public double[] getAxisRange(AxisType axisType);

    /**
     * Gets the data titles.
     */
    public String[] getDataTitles();

    /**
     * Gets the {@link DataStyle}s.
     */
    public DataStyle[] getDataStyles();

    /**
     * Gets whether the given property is enabled.
     * 
     * @param property
     *            the given property.
     * @return whether the given property is enabled.
     */
    public boolean getPropertyEnabled(String property);
}
