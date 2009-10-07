/**
 * This file is part of the Shared Scientific Toolbox in Java ("this library"). <br />
 * <br />
 * Copyright (C) 2008 Roy Liu <br />
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

package shared.array;

import static shared.util.Control.LineSeparator;

import java.lang.reflect.Array;
import java.util.Formatter;

import shared.array.Array.IndexingOrder;
import shared.array.kernel.ModalArrayIOKernel;
import shared.array.kernel.ModalArrayKernel;
import shared.fft.ModalFFTService;
import shared.util.Arithmetic;
import shared.util.Control;

/**
 * A collection of useful static methods and data structures common to all {@link Array} implementations.
 * 
 * @author Roy Liu
 */
public class ArrayBase {

    /**
     * The array operations kernel.
     */
    final public static ModalArrayKernel OpKernel = new ModalArrayKernel();

    /**
     * The array I/O kernel.
     */
    final public static ModalArrayIOKernel IOKernel = new ModalArrayIOKernel();

    /**
     * A global service providing access to FFT operations.
     */
    final public static ModalFFTService FFTService = new ModalFFTService();

    /**
     * The default storage order shall be {@link shared.array.Array.IndexingOrder#FAR}.
     */
    final public static IndexingOrder DEFAULT_ORDER = IndexingOrder.FAR;

    /**
     * The field width to use when printing.
     */
    public static int FIELD_WIDTH = 8;

    /**
     * The field precision to use when printing.
     */
    public static int FIELD_PRECISION = 2;

    /**
     * Canonicalizes the alternate slicing specification.
     */
    final public static int[] canonicalizeSlices(int[][] srcSlices, int[] srcDims, int[][] dstSlices, int[] dstDims) {

        int ndims = srcSlices.length;

        Control.checkTrue(ndims == dstSlices.length //
                && ndims == srcDims.length //
                && ndims == dstDims.length, //
                "Dimensionality mismatch");

        int nslices = 0;

        for (int dim = 0; dim < ndims; dim++) {
            nslices += Control.checkEquals(srcSlices[dim].length, dstSlices[dim].length, //
                    "Dimension mismatch");
        }

        int[] slices = new int[3 * nslices];

        for (int dim = 0, offset = 0; dim < ndims; dim++) {

            int[] srcSlice = srcSlices[dim];
            int[] dstSlice = dstSlices[dim];

            for (int j = 0, m = srcSlice.length; j < m; j++, offset += 3) {

                slices[offset] = srcSlice[j];
                slices[offset + 1] = dstSlice[j];
                slices[offset + 2] = dim;
            }
        }

        return slices;
    }

    /**
     * Canonicalizes the alternate slicing specification.
     */
    final public static int[] canonicalizeSlices(int[] srcDims, int[] dstDims, int[][] dstSlices) {

        int ndims = dstSlices.length;

        Control.checkTrue(ndims == srcDims.length //
                && ndims == dstDims.length, //
                "Dimensionality mismatch");

        int nslices = 0;

        for (int dim = 0; dim < ndims; dim++) {
            nslices += Control.checkEquals(srcDims[dim], dstSlices[dim].length, //
                    "Dimension mismatch");
        }

        int[] slices = new int[3 * nslices];

        for (int dim = 0, offset = 0; dim < ndims; dim++) {

            int[] dstSlice = dstSlices[dim];

            for (int j = 0, m = dstSlice.length; j < m; j++, offset += 3) {

                slices[offset] = j;
                slices[offset + 1] = dstSlice[j];
                slices[offset + 2] = dim;
            }
        }

        return slices;
    }

    /**
     * Canonicalizes the alternate slicing specification.
     */
    final public static int[] canonicalizeSlices(int nslices, int[] srcDims, int[][] srcSlices) {

        int[] slices = new int[3 * nslices];

        for (int dim = 0, offset = 0, ndims = srcDims.length; dim < ndims; dim++) {

            int[] srcSlice = srcSlices[dim];

            for (int j = 0, m = srcSlice.length; j < m; j++, offset += 3) {

                slices[offset] = srcSlice[j];
                slices[offset + 1] = j;
                slices[offset + 2] = dim;
            }
        }

        return slices;
    }

    /**
     * Creates a slicing specification for {@link shared.array.Array#reverse(int...)}.
     */
    final public static int[] createReverseSlices(int[] srcDims, int[] selectedDims) {

        int ndims = srcDims.length;
        int nindices = selectedDims.length;

        int nslices = Arithmetic.sum(srcDims);
        int[] slices = new int[3 * nslices];

        boolean[] sentinel = new boolean[ndims];

        for (int i = 0; i < nindices; i++) {
            sentinel[selectedDims[i]] = true;
        }

        for (int dim = 0, offset = 0; dim < ndims; dim++) {

            int dimSize = srcDims[dim];

            if (sentinel[dim]) {

                for (int j = 0, k = dimSize - 1; j < dimSize; j++, k--, offset += 3) {

                    slices[offset] = j;
                    slices[offset + 1] = k;
                    slices[offset + 2] = dim;
                }

            } else {

                for (int j = 0; j < dimSize; j++, offset += 3) {

                    slices[offset] = j;
                    slices[offset + 1] = j;
                    slices[offset + 2] = dim;
                }
            }
        }

        return slices;
    }

    /**
     * Sets the formatting parameters {@link #FIELD_WIDTH} and {@link #FIELD_PRECISION}.
     * 
     * @param width
     *            the number width.
     * @param precision
     *            the number precision.
     */
    final public static void format(int width, int precision) {

        Control.checkTrue(width >= precision + 4 && precision >= 1, //
                "Invalid formatting parameters");

        FIELD_WIDTH = width;
        FIELD_PRECISION = precision;
    }

    /**
     * Formats a two-dimensional slice of an array for display.
     * 
     * @param f
     *            the target {@link Formatter}.
     * @param format
     *            the formatting string.
     * @param values
     *            the values.
     * @param indices
     *            the physical indices.
     * @param offset
     *            the physical index offset.
     * @param nrows
     *            the number of rows in the slice.
     * @param ncols
     *            the number of columns in the slice.
     * @param isComplex
     *            whether the values are complex.
     */
    final public static void formatSlice(Formatter f, String format, //
            Object values, int[] indices, //
            int offset, int nrows, int ncols, boolean isComplex) {

        if (!isComplex) {

            for (int j = 0, k = 0; j < nrows; j++) {

                for (int i = 0; i < ncols; i++, k++) {
                    f.format(format, Array.get(values, indices[offset + k]));
                }

                f.format(LineSeparator);
            }

        } else {

            for (int j = 0, k = 0; j < nrows; j++) {

                for (int i = 0; i < ncols; i++, k += 2) {
                    f.format(format, //
                            Array.get(values, indices[offset + k]), //
                            Array.get(values, indices[offset + k + 1]));
                }

                f.format(LineSeparator);
            }
        }
    }

    /**
     * Formats an empty array message.
     * 
     * @param f
     *            the target {@link Formatter}.
     * @param dims
     *            the dimensions.
     */
    final public static void formatEmptyArray(Formatter f, int[] dims) {

        int ndims = dims.length;

        f.format(LineSeparator).format("[empty (");

        for (int i = 0, n = ndims - 1; i < n; i++) {
            f.format("%d, ", dims[i]);
        }

        f.format("%d)]", dims[ndims - 1]).format(LineSeparator);
    }

    /**
     * Formats a potential rescale message if the given exponent is too large or too small.
     * 
     * @param f
     *            the target {@link Formatter}.
     * @param exponent
     *            the exponent.
     * @param values
     *            the values.
     * @return the potentially rescaled values.
     */
    final public static double[] formatRescale(Formatter f, int exponent, double[] values) {

        if (FIELD_WIDTH < exponent + FIELD_PRECISION + 4 || exponent < 0) {

            f.format("%s[rescale 10^%d]%s", //
                    LineSeparator, exponent, LineSeparator);

            double scale = Math.pow(10.0, -exponent);

            double[] res = values.clone();

            for (int i = 0, n = res.length; i < n; i++) {
                res[i] *= scale;
            }

            return res;

        } else {

            return values;
        }
    }

    /**
     * Formats a sparse array for display.
     * 
     * @param f
     *            the target {@link Formatter}.
     * @param valueFormat
     *            the formatting string for values.
     * @param indexFormat
     *            the formatting string for logical indices.
     * @param values
     *            the values.
     * @param indices
     *            the physical indices.
     * @param strides
     *            the strides.
     */
    final public static void formatSparseArray(Formatter f, String valueFormat, String indexFormat, //
            Object values, int[] indices, int[] strides) {

        int ndims = strides.length;

        for (int i = 0, n = Control.checkEquals(Array.getLength(values), indices.length); i < n; i++) {

            int physical = indices[i];

            f.format("(");

            for (int j = 0, m = ndims - 1; j < m; j++) {

                f.format(indexFormat, physical / strides[j]);
                f.format(",");

                physical %= strides[j];
            }

            f.format(indexFormat, physical / strides[ndims - 1]);
            f.format(")");
            f.format(valueFormat, Array.get(values, i));
            f.format(LineSeparator);
        }
    }

    /**
     * Computes the logical index from the given physical index and strides.
     * 
     * @param physical
     *            the physical index.
     * @param strides
     *            the strides.
     * @param logical
     *            the logical index.
     * @return the logical index.
     */
    final public static int[] logical(int physical, int[] strides, int[] logical) {

        for (int dim = 0, ndims = Control.checkEquals(strides.length, logical.length); dim < ndims; dim++) {

            logical[dim] = physical / strides[dim];
            physical %= strides[dim];
        }

        return logical;
    }

    // Dummy constructor.
    ArrayBase() {
    }
}
