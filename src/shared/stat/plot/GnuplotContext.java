/**
 * This file is part of the Shared Scientific Toolbox in Java ("this library"). <br />
 * <br />
 * Copyright (C) 2009 Roy Liu <br />
 * <br />
 * This library is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 2.1 of the License, or (at your option)
 * any later version. <br />
 * <br />
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details. <br />
 * <br />
 * You should have received a copy of the GNU Lesser General Public License along with this library. If not, see <a
 * href="http://www.gnu.org/licenses/">http://www.gnu.org/licenses/</a>.
 */

package shared.stat.plot;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import shared.array.RealArray;
import shared.stat.plot.Plot.AxisScaleType;
import shared.stat.plot.Plot.AxisType;
import shared.util.Control;

/**
 * A <a href="http://www.gnuplot.info/">Gnuplot</a>-backed implementation of {@link PlotContext}.
 * 
 * @apiviz.composedOf shared.stat.plot.GnuplotContext.Gnuplot
 * @apiviz.uses shared.stat.plot.PlotBase
 * @author Roy Liu
 */
public class GnuplotContext implements PlotContext<GnuplotContext, GnuplotContext.Gnuplot> {

    /**
     * An array of operating system dependent arguments to execute Gnuplot.
     */
    final protected static String[] GnuplotExecArgs;

    /**
     * The property that toggles the legend.
     */
    final public static String PROPERTY_LEGEND = "legend";

    /**
     * The property that toggles the grid.
     */
    final public static String PROPERTY_GRID = "grid";

    /**
     * The property that toggles the mesh.
     */
    final public static String PROPERTY_MESH = "mesh";

    /**
     * The property that toggles the colormap.
     */
    final public static String PROPERTY_COLORMAP = "colormap";

    static {

        boolean isWindows = System.getProperty("os.name").contains("Windows");

        GnuplotExecArgs = isWindows ? new String[] { "cmd", "/C", "gnuplot.exe" } //
                : new String[] { "gnuplot" };
    }

    final List<Gnuplot> plots;

    String fontName;

    int fontSize;

    String outputFormat;

    int outputWidth;
    int outputHeight;

    /**
     * Default constructor.
     */
    public GnuplotContext() {

        this.plots = new ArrayList<Gnuplot>();

        this.fontName = "Helvetica";
        this.fontSize = 8;
        this.outputFormat = "svg";
        this.outputWidth = 800;
        this.outputHeight = 600;
    }

    public Gnuplot addPlot(Plottable plottable) {

        final Gnuplot gp = new Gnuplot(plottable.getDatasets());

        for (int dim = 0, ndims = gp.ndims; dim < ndims; dim++) {

            AxisType axisType = AxisType.values()[dim];

            double[] range = plottable.getAxisRange(axisType);

            gp.setAxisRange(axisType, range[0], range[1]) //
                    .setAxisTitle(axisType, plottable.getAxisTitle(axisType));
        }

        gp.setTitle(plottable.getTitle()) //
                .setDataStyles(plottable.getDataStyles()) //
                .setDataTitles(plottable.getDataTitles()) //
                .setPropertyEnabled(PROPERTY_LEGEND, //
                        plottable.getPropertyEnabled(PROPERTY_LEGEND)) //
                .setPropertyEnabled(PROPERTY_GRID, //
                        plottable.getPropertyEnabled(PROPERTY_GRID)) //
                .setPropertyEnabled(PROPERTY_MESH, //
                        plottable.getPropertyEnabled(PROPERTY_MESH)) //
                .setPropertyEnabled(PROPERTY_COLORMAP, //
                        plottable.getPropertyEnabled(PROPERTY_COLORMAP));

        this.plots.add(gp);

        return gp;
    }

    public Gnuplot addPlot(RealArray... datasets) {

        final Gnuplot gp = new Gnuplot(datasets);

        this.plots.add(gp);

        return gp;
    }

    public void toFile(File file) throws IOException {

        File errFile = new File(file.toString().concat(".log"));

        FileOutputStream out = new FileOutputStream(file);
        FileOutputStream errOut = new FileOutputStream(errFile);

        try {

            Control.execAndWaitFor(new ByteArrayInputStream(toString().getBytes()), out, errOut, //
                    GnuplotExecArgs);

        } catch (IOException e) {

            e.printStackTrace(new PrintStream(errOut));

        } finally {

            Control.close(out);
            Control.close(errOut);
        }

        if (errFile.length() == 0) {
            Control.delete(errFile);
        }
    }

    /**
     * Creates Gnuplot directives for rendering this context.
     */
    @Override
    public String toString() {

        final String configStr;

        if (this.outputFormat.equals("eps")) {

            configStr = String.format("postscript eps color font \"%s\" %d", //
                    this.fontName, this.fontSize);

        } else if (this.outputFormat.equals("png")) {

            configStr = String.format("png truecolor font \"%s\" %d size %d, %d", //
                    this.fontName, this.fontSize, //
                    this.outputWidth, this.outputHeight);

        } else if (this.outputFormat.equals("svg")) {

            configStr = String.format("svg fname \"%s\" fsize %d size %d, %d", //
                    this.fontName, this.fontSize, //
                    this.outputWidth, this.outputHeight);

        } else {

            throw new IllegalArgumentException( //
                    String.format("Output format '%s' not recognized", this.outputFormat));
        }

        Formatter f = new Formatter();

        f.format("set terminal %s%n", configStr);
        f.format("set output%n");
        f.format("set size 1.0, 1.0%n");
        // Create a better-looking colormap.
        f.format("set palette rgbformulae 22, 13, -31%n");
        f.format("set multiplot%n");

        int maxWidth = 1;
        int maxHeight = 1;

        for (Gnuplot plot : this.plots) {

            maxWidth = Math.max(maxWidth, plot.panelX + plot.panelWidth);
            maxHeight = Math.max(maxHeight, plot.panelY + plot.panelHeight);
        }

        for (Gnuplot plot : this.plots) {

            int ndims = plot.ndims;
            int nclasses = plot.datasets.length;

            f.format("set origin %.4f, %.4f%n", //
                    plot.panelX / (double) maxWidth, //
                    (maxHeight - plot.panelY - plot.panelHeight) / (double) maxHeight);
            f.format("set size %.4f, %.4f%n", //
                    plot.panelWidth / (double) maxWidth, //
                    plot.panelHeight / (double) maxHeight);
            f.format("set title \"%s\"%n", plot.title);
            f.format("%s%n", plot.isLegendEnabled ? "set key box below right" : "unset key");

            int meshSize = 0;

            if (plot.isMeshEnabled) {

                for (RealArray data : plot.datasets) {
                    meshSize = Math.max(data.size(0), meshSize);
                }
            }

            meshSize = (int) Math.sqrt(meshSize) + 1;

            f.format("%s%n", plot.isGridEnabled ? "set grid" : "unset grid");
            f.format("%s%n", plot.isMeshEnabled ? String.format("set dgrid3d %d, %d", meshSize, meshSize)
                    : "unset dgrid3d");
            f.format("%s%n", plot.isColormapEnabled ? "set pm3d" : "unset pm3d");
            f.format("%s%n", (plot.viewportParameters != null) ? String.format("set view %.2f, %.2f", //
                    plot.viewportParameters[0], plot.viewportParameters[1]) : "unset view");

            for (int dim = 0; dim < ndims; dim++) {

                String axisName = AxisType.values()[dim].toString().toLowerCase();

                f.format("set %srange [%.4e:%.4e]%n", axisName, //
                        plot.axisRanges[2 * dim], plot.axisRanges[2 * dim + 1]);
                f.format("set %slabel \"%s\"%n", axisName, plot.axisTitles[dim]);
                f.format("%s logscale %s%n", //
                        (plot.axisScaleTypes[dim] == AxisScaleType.LOG) ? "set" : "unset", //
                        axisName);
            }

            for (int i = 0; i < nclasses; i++) {
                f.format("set style line %d%s%n", i + 1, createLineStyleDefinition(plot.dataStyles[i]));
            }

            final String plotStr;

            switch (plot.ndims) {

            case 2:
                plotStr = "plot";
                break;

            case 3:
                plotStr = "splot";
                break;

            default:
                throw new IllegalArgumentException();
            }

            f.format("%s ", plotStr);

            for (int i = 0; i < nclasses; i++) {

                DataStyle style = plot.dataStyles[i];

                String styleStr;

                switch (style.getType()) {

                case LINES:
                    styleStr = "lines";
                    break;

                case POINTS:
                    styleStr = "points";
                    break;

                case LINESPOINTS:
                    styleStr = "linespoints";
                    break;

                case BARS:
                    styleStr = "boxes";
                    break;

                case SURFACE:
                    styleStr = "pm3d";
                    break;

                default:
                    throw new IllegalArgumentException();
                }

                f.format("\"-\" title \"%s\" with %s ls %d", plot.dataTitles[i], styleStr, i + 1);
                f.format((i < nclasses - 1) ? ", " : "%n");
            }

            for (int i = 0; i < nclasses; i++) {

                RealArray data = plot.datasets[i];

                for (int j = 0, m = data.size(0); j < m; j++) {

                    for (int dim = 0; dim < ndims; dim++) {

                        f.format("%.4e", data.get(j, dim));
                        f.format((dim < ndims - 1) ? " " : "%n");
                    }
                }

                f.format("e%n");
            }

            f.format("unset style line%n");
        }

        f.format("unset multiplot%n");

        return f.toString();
    }

    public GnuplotContext setFont(String fontName, int fontSize) {

        this.fontName = fontName;
        this.fontSize = fontSize;

        return this;
    }

    public GnuplotContext setOutputFormat(String outputFormat) {

        this.outputFormat = outputFormat;

        return this;
    }

    public GnuplotContext setOutputSize(int outputWidth, int outputHeight) {

        this.outputHeight = outputHeight;
        this.outputWidth = outputWidth;

        return this;
    }

    /**
     * Creates a Gnuplot line style definition from the given {@link DataStyle}.
     */
    final protected static String createLineStyleDefinition(DataStyle style) {

        Formatter f = new Formatter();

        String lt = style.getLineStyle();
        String lc = style.getLineColor();
        Double lw = style.getLineSize();
        String pt = style.getPointStyle();
        Double ps = style.getPointSize();

        if (lt != null) {
            f.format(" lt %s", lt);
        }

        if (lc != null) {
            f.format(" lc rgb \"%s\"", lc);
        }

        if (lw != null) {
            f.format(" lw %.2f", lw);
        }

        if (pt != null) {
            f.format(" pt %s", pt);
        }

        if (ps != null) {
            f.format(" ps %.2f", ps);
        }

        String res = f.toString();
        return !res.equals("") ? f.toString() : " default";
    }

    /**
     * An internal implementation of {@link Plot}.
     */
    public static class Gnuplot implements Plot<Gnuplot> {

        final String[] axisTitles;
        final AxisScaleType[] axisScaleTypes;
        final double[] axisRanges;

        final int ndims;
        final RealArray[] datasets;
        final String[] dataTitles;
        final DataStyle[] dataStyles;

        double[] viewportParameters;

        String title;

        boolean isLegendEnabled;
        boolean isGridEnabled;
        boolean isMeshEnabled;
        boolean isColormapEnabled;

        int panelX;
        int panelY;
        int panelWidth;
        int panelHeight;

        /**
         * Default constructor.
         */
        public Gnuplot(RealArray[] datasets) {

            int nclasses = datasets.length;

            this.ndims = PlotBase.inferDimensionality(datasets);

            Control.checkTrue(this.ndims >= 2 && this.ndims <= 3, //
                    "The dimensionality must be either two or three");

            this.axisTitles = shared.util.Arrays.newArray(String.class, this.ndims, "");
            this.axisScaleTypes = shared.util.Arrays.newArray(AxisScaleType.class, this.ndims, AxisScaleType.NORMAL);
            this.axisRanges = new double[2 * this.ndims];

            this.datasets = datasets;
            this.dataTitles = shared.util.Arrays.newArray(String.class, nclasses, "");
            this.dataStyles = shared.util.Arrays.newArray(DataStyle.class, nclasses, DataStyle.Points);

            for (int i = 0, n = 2 * this.ndims; i < n; i += 2) {

                this.axisRanges[i] = 0.0;
                this.axisRanges[i + 1] = 1.0;
            }

            this.viewportParameters = null;
            this.title = "";
            this.isLegendEnabled = false;
            this.isGridEnabled = false;
            this.isMeshEnabled = false;
            this.isColormapEnabled = false;
            this.panelX = 0;
            this.panelY = 0;
            this.panelWidth = 1;
            this.panelHeight = 1;
        }

        public Gnuplot setTitle(String title) {

            this.title = title;

            return this;
        }

        public Gnuplot setAxis(AxisType axisType, String axisTitle, double lower, double upper,
                AxisScaleType axisScaleType) {

            int dim = axisType.ordinal();

            Control.checkTrue(dim < this.ndims, //
                    "Invalid axis type");

            this.axisTitles[dim] = axisTitle;
            this.axisRanges[2 * dim] = lower;
            this.axisRanges[2 * dim + 1] = upper;
            this.axisScaleTypes[dim] = axisScaleType;

            return this;
        }

        public Gnuplot setAxisTitle(AxisType axisType, String axisTitle) {

            int dim = axisType.ordinal();

            Control.checkTrue(dim < this.ndims, //
                    "Invalid axis type");

            this.axisTitles[dim] = axisTitle;

            return this;
        }

        public Gnuplot setAxisRange(AxisType axisType, double lower, double upper) {

            int dim = axisType.ordinal();

            Control.checkTrue(dim < this.ndims, //
                    "Invalid axis type");

            this.axisRanges[2 * dim] = lower;
            this.axisRanges[2 * dim + 1] = upper;

            return this;
        }

        public Gnuplot setAxisScale(AxisType axisType, AxisScaleType axisScaleType) {

            int dim = axisType.ordinal();

            Control.checkTrue(dim < this.ndims, //
                    "Invalid axis type");

            this.axisScaleTypes[dim] = axisScaleType;

            return this;
        }

        public Gnuplot setDataTitles(String... dataTitles) {

            System.arraycopy(dataTitles, 0, this.dataTitles, 0, //
                    Control.checkEquals(this.dataTitles.length, dataTitles.length));

            return this;
        }

        public Gnuplot setDataStyles(DataStyle... dataStyles) {

            System.arraycopy(dataStyles, 0, this.dataStyles, 0, //
                    Control.checkEquals(this.dataStyles.length, dataStyles.length));

            return this;
        }

        public Gnuplot setViewport(double... viewportParameters) {

            Control.checkTrue(this.ndims == 3, //
                    "Only three-dimensional plots may have their viewports specified");

            Control.checkTrue(viewportParameters.length == 2, //
                    "Invalid arguments");

            this.viewportParameters = viewportParameters;

            return this;
        }

        public Gnuplot setPropertyEnabled(String property, boolean isPropertyEnabled) {

            if (property.equals(PROPERTY_LEGEND)) {

                this.isLegendEnabled = isPropertyEnabled;

            } else if (property.equals(PROPERTY_GRID)) {

                this.isGridEnabled = isPropertyEnabled;

            } else if (property.equals(PROPERTY_MESH)) {

                this.isMeshEnabled = isPropertyEnabled;

            } else if (property.equals(PROPERTY_COLORMAP)) {

                this.isColormapEnabled = isPropertyEnabled;

            } else {

                throw new IllegalArgumentException("Invalid Gnuplot property");
            }

            return this;
        }

        public Gnuplot setPanelLocation(int panelX, int panelY) {

            this.panelX = panelX;
            this.panelY = panelY;

            return this;
        }

        public Gnuplot setPanelSize(int panelWidth, int panelHeight) {

            this.panelWidth = panelWidth;
            this.panelHeight = panelHeight;

            return this;
        }
    }
}
