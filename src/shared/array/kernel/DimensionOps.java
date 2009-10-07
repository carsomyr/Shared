/**
 * This file is part of the Shared Scientific Toolbox in Java ("this library"). <br />
 * <br />
 * Copyright (C) 2007 Roy Liu <br />
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

package shared.array.kernel;

import static shared.array.kernel.ArrayKernel.RD_PROD;
import static shared.array.kernel.ArrayKernel.RD_SUM;
import static shared.array.kernel.ArrayKernel.RI_GZERO;
import static shared.array.kernel.ArrayKernel.RI_LZERO;
import static shared.array.kernel.ArrayKernel.RI_MAX;
import static shared.array.kernel.ArrayKernel.RI_MIN;
import static shared.array.kernel.ArrayKernel.RI_SORT;
import static shared.array.kernel.ArrayKernel.RI_ZERO;
import static shared.array.kernel.ArrayKernel.RR_MAX;
import static shared.array.kernel.ArrayKernel.RR_MIN;
import static shared.array.kernel.ArrayKernel.RR_PROD;
import static shared.array.kernel.ArrayKernel.RR_SUM;
import static shared.array.kernel.ArrayKernel.RR_VAR;

import java.util.Arrays;

import shared.util.Arithmetic;
import shared.util.Control;

/**
 * A class for dimension operations in pure Java.
 * 
 * @apiviz.has shared.array.kernel.DimensionOps.RealReduceOperation - - - argument
 * @apiviz.has shared.array.kernel.DimensionOps.RealIndexOperation - - - argument
 * @apiviz.has shared.array.kernel.DimensionOps.RealDimensionOperation - - - argument
 * @apiviz.uses shared.array.kernel.PermutationEntry
 * @author Roy Liu
 */
public class DimensionOps {

    /**
     * Defines real reduce operations.
     */
    protected interface RealReduceOperation {

        /**
         * Performs a real reduce operation.
         */
        public void op(double[] working, int[] workingIndices, int size, int stride);
    }

    final static RealReduceOperation RRSumOp = new RealReduceOperation() {

        public void op(double[] working, int[] workingIndices, int size, int stride) {

            for (int workingIndex : workingIndices) {

                for (int j = 1, offset = workingIndex + stride; j < size; j++, offset += stride) {
                    working[workingIndex] += working[offset];
                }
            }
        }
    };

    final static RealReduceOperation RRProdOp = new RealReduceOperation() {

        public void op(double[] working, int[] workingIndices, int size, int stride) {

            for (int workingIndex : workingIndices) {

                for (int j = 1, offset = workingIndex + stride; j < size; j++, offset += stride) {
                    working[workingIndex] *= working[offset];
                }
            }
        }
    };

    final static RealReduceOperation RRMaxOp = new RealReduceOperation() {

        public void op(double[] working, int[] workingIndices, int size, int stride) {

            for (int workingIndex : workingIndices) {

                for (int j = 1, offset = workingIndex + stride; j < size; j++, offset += stride) {
                    working[workingIndex] = Math.max(working[offset], working[workingIndex]);
                }
            }
        }
    };

    final static RealReduceOperation RRMinOp = new RealReduceOperation() {

        public void op(double[] working, int[] workingIndices, int size, int stride) {

            for (int workingIndex : workingIndices) {

                for (int j = 1, offset = workingIndex + stride; j < size; j++, offset += stride) {
                    working[workingIndex] = Math.min(working[offset], working[workingIndex]);
                }
            }
        }
    };

    final static RealReduceOperation RRVarOp = new RealReduceOperation() {

        public void op(double[] working, int[] workingIndices, int size, int stride) {

            for (int workingIndex : workingIndices) {

                double mean = 0.0;

                for (int j = 0, offset = workingIndex; j < size; j++, offset += stride) {
                    mean += working[offset];
                }

                mean /= size;

                for (int j = 0, offset = workingIndex; j < size; j++, offset += stride) {

                    double diff = working[offset] - mean;
                    working[offset] = diff * diff;
                }

                for (int j = 1, offset = workingIndex + stride; j < size; j++, offset += stride) {
                    working[workingIndex] += working[offset];
                }

                working[workingIndex] /= size;
            }
        }
    };

    /**
     * Defines real index operations.
     */
    protected interface RealIndexOperation {

        /**
         * Performs a real index operation.
         */
        public void op(double[] srcV, int[] srcIndices, int[] dstV, int size, int stride);
    }

    final static RealIndexOperation RIMaxOp = new RealIndexOperation() {

        public void op(double[] srcV, int[] srcIndices, int[] dstV, int size, int stride) {

            if (srcIndices != null) {

                int maxStride = stride * size;

                for (int i = 0, nindices = srcIndices.length; i < nindices; i++) {

                    double acc = -Double.MAX_VALUE;

                    for (int offset = 0; offset < maxStride; offset += stride) {
                        acc = Math.max(acc, srcV[srcIndices[i] + offset]);
                    }

                    int count = 0;

                    for (int offset = 0; offset < maxStride; offset += stride) {

                        if (srcV[srcIndices[i] + offset] == acc) {

                            dstV[srcIndices[i] + count] = offset / stride;
                            count += stride;
                        }
                    }

                    for (int offset = count; offset < maxStride; offset += stride) {
                        dstV[srcIndices[i] + offset] = -1;
                    }
                }

            } else {

                double maxValue = Arithmetic.max(srcV);

                for (int i = 0, n = srcV.length; i < n; i++) {
                    dstV[i] = (srcV[i] == maxValue) ? 1 : 0;
                }
            }
        }
    };

    final static RealIndexOperation RIMinOp = new RealIndexOperation() {

        public void op(double[] srcV, int[] srcIndices, int[] dstV, int size, int stride) {

            if (srcIndices != null) {

                int maxStride = stride * size;

                for (int i = 0, nindices = srcIndices.length; i < nindices; i++) {

                    double acc = Double.MAX_VALUE;

                    for (int offset = 0; offset < maxStride; offset += stride) {
                        acc = Math.min(acc, srcV[srcIndices[i] + offset]);
                    }

                    int count = 0;

                    for (int offset = 0; offset < maxStride; offset += stride) {

                        if (srcV[srcIndices[i] + offset] == acc) {

                            dstV[srcIndices[i] + count] = offset / stride;
                            count += stride;
                        }
                    }

                    for (int offset = count; offset < maxStride; offset += stride) {
                        dstV[srcIndices[i] + offset] = -1;
                    }
                }

            } else {

                double minValue = Arithmetic.min(srcV);

                for (int i = 0, n = srcV.length; i < n; i++) {
                    dstV[i] = (srcV[i] == minValue) ? 1 : 0;
                }
            }
        }
    };

    final static RealIndexOperation RIZeroOp = new RealIndexOperation() {

        public void op(double[] srcV, int[] srcIndices, int[] dstV, int size, int stride) {

            if (srcIndices != null) {

                int maxStride = stride * size;

                for (int i = 0, nindices = srcIndices.length; i < nindices; i++) {

                    int count = 0;

                    for (int offset = 0; offset < maxStride; offset += stride) {

                        if (srcV[srcIndices[i] + offset] == 0.0) {

                            dstV[srcIndices[i] + count] = offset / stride;
                            count += stride;
                        }
                    }

                    for (int offset = count; offset < maxStride; offset += stride) {
                        dstV[srcIndices[i] + offset] = -1;
                    }
                }

            } else {

                for (int i = 0, n = srcV.length; i < n; i++) {
                    dstV[i] = (srcV[i] == 0.0) ? 1 : 0;
                }
            }
        }
    };

    final static RealIndexOperation RIGZeroOp = new RealIndexOperation() {

        public void op(double[] srcV, int[] srcIndices, int[] dstV, int size, int stride) {

            if (srcIndices != null) {

                int maxStride = stride * size;

                for (int i = 0, nindices = srcIndices.length; i < nindices; i++) {

                    int count = 0;

                    for (int offset = 0; offset < maxStride; offset += stride) {

                        if (srcV[srcIndices[i] + offset] > 0.0) {

                            dstV[srcIndices[i] + count] = offset / stride;
                            count += stride;
                        }
                    }

                    for (int offset = count; offset < maxStride; offset += stride) {
                        dstV[srcIndices[i] + offset] = -1;
                    }
                }

            } else {

                for (int i = 0, n = srcV.length; i < n; i++) {
                    dstV[i] = (srcV[i] > 0.0) ? 1 : 0;
                }
            }
        }
    };

    final static RealIndexOperation RILZeroOp = new RealIndexOperation() {

        public void op(double[] srcV, int[] srcIndices, int[] dstV, int size, int stride) {

            if (srcIndices != null) {

                int maxStride = stride * size;

                for (int i = 0, nindices = srcIndices.length; i < nindices; i++) {

                    int count = 0;

                    for (int offset = 0; offset < maxStride; offset += stride) {

                        if (srcV[srcIndices[i] + offset] < 0.0) {

                            dstV[srcIndices[i] + count] = offset / stride;
                            count += stride;
                        }
                    }

                    for (int offset = count; offset < maxStride; offset += stride) {
                        dstV[srcIndices[i] + offset] = -1;
                    }
                }

            } else {

                for (int i = 0, n = srcV.length; i < n; i++) {
                    dstV[i] = (srcV[i] < 0.0) ? 1 : 0;
                }
            }
        }
    };

    final static RealIndexOperation RISortOp = new RealIndexOperation() {

        public void op(double[] srcV, int[] srcIndices, int[] dstV, int size, int stride) {

            Double[] srcVBoxed = shared.util.Arrays.box(srcV);

            PermutationEntry.iSort(srcVBoxed, srcIndices, dstV, size, stride);

            for (int i = 0, n = srcV.length; i < n; i++) {
                srcV[i] = srcVBoxed[i];
            }
        }
    };

    /**
     * Defines real dimension operations.
     */
    protected interface RealDimensionOperation {

        /**
         * Performs a real dimension operation.
         */
        public void op(double[] srcV, int[] srcD, int[] srcS, double[] dstV, int[] selectedDims);
    }

    final static RealDimensionOperation RDSumOp = new RealDimensionOperation() {

        public void op(double[] srcV, int[] srcD, int[] srcS, double[] dstV, int[] selectedDims) {

            int len = Control.checkEquals(srcV.length, dstV.length);
            int ndims = Control.checkEquals(srcD.length, srcS.length);

            boolean[] indicator = new boolean[ndims];

            for (int dim : selectedDims) {
                indicator[dim] = true;
            }

            int[] srcIndices = MappingOps.assignMappingIndices(len, srcD, srcS);

            //

            System.arraycopy(srcV, 0, dstV, 0, len);

            for (int dim = 0, indexBlockIncrement = len; dim < ndims; indexBlockIncrement /= srcD[dim++]) {

                if (!indicator[dim]) {
                    continue;
                }

                int size = srcD[dim];
                int stride = srcS[dim];

                for (int lower = 0, upper = indexBlockIncrement / size; //
                lower < len; //
                lower += indexBlockIncrement, upper += indexBlockIncrement) {

                    for (int indexIndex = lower; indexIndex < upper; indexIndex++) {

                        double acc = 0.0;

                        for (int k = 0, physical = srcIndices[indexIndex]; k < size; k++, physical += stride) {

                            acc += dstV[physical];
                            dstV[physical] = acc;
                        }
                    }
                }
            }
        }
    };

    final static RealDimensionOperation RDProdOp = new RealDimensionOperation() {

        public void op(double[] srcV, int[] srcD, int[] srcS, double[] dstV, int[] selectedDims) {

            int len = Control.checkEquals(srcV.length, dstV.length);
            int ndims = Control.checkEquals(srcD.length, srcS.length);

            boolean[] indicator = new boolean[ndims];

            for (int dim : selectedDims) {
                indicator[dim] = true;
            }

            int[] srcIndices = MappingOps.assignMappingIndices(len, srcD, srcS);

            //

            System.arraycopy(srcV, 0, dstV, 0, len);

            for (int dim = 0, indexBlockIncrement = len; dim < ndims; indexBlockIncrement /= srcD[dim++]) {

                if (!indicator[dim]) {
                    continue;
                }

                int size = srcD[dim];
                int stride = srcS[dim];

                for (int lower = 0, upper = indexBlockIncrement / size; //
                lower < len; //
                lower += indexBlockIncrement, upper += indexBlockIncrement) {

                    for (int indexIndex = lower; indexIndex < upper; indexIndex++) {

                        double acc = 1.0;

                        for (int k = 0, physical = srcIndices[indexIndex]; k < size; k++, physical += stride) {

                            acc *= dstV[physical];
                            dstV[physical] = acc;
                        }
                    }
                }
            }
        }
    };

    /**
     * Assigns base indices when excluding a dimension.
     * 
     * @param nindices
     *            the number of indices.
     * @param srcD
     *            the dimensions.
     * @param srcS
     *            the strides.
     * @param dim
     *            the dimension to exclude.
     * @return the base physical indices.
     */
    final public static int[] assignBaseIndices(int nindices, int[] srcD, int[] srcS, int dim) {

        int ndims = Control.checkEquals(srcD.length, srcS.length, //
                "Invalid arguments");

        int[] dModified = new int[ndims - 1];
        int[] sModified = new int[ndims - 1];

        System.arraycopy(srcD, 0, dModified, 0, dim);
        System.arraycopy(srcD, dim + 1, dModified, dim, (ndims - 1) - dim);

        System.arraycopy(srcS, 0, sModified, 0, dim);
        System.arraycopy(srcS, dim + 1, sModified, dim, (ndims - 1) - dim);

        return MappingOps.assignMappingIndices(nindices, dModified, sModified);
    }

    /**
     * Dimension reduce operations in support of
     * {@link JavaArrayKernel#rrOp(int, double[], int[], int[], double[], int[], int[], int...)}.
     */
    final public static void rrOp(int type, //
            double[] srcV, int[] srcD, int[] srcS, //
            double[] dstV, int[] dstD, int[] dstS, //
            int[] selectedDims) {

        int srcLen = MappingOps.checkDimensions(srcV.length, srcD, srcS);
        int dstLen = MappingOps.checkDimensions(dstV.length, dstD, dstS);

        final RealReduceOperation op;

        switch (type) {

        case RR_SUM:
            op = RRSumOp;
            break;

        case RR_PROD:
            op = RRProdOp;
            break;

        case RR_MAX:
            op = RRMaxOp;
            break;

        case RR_MIN:
            op = RRMinOp;
            break;

        case RR_VAR:
            op = RRVarOp;
            break;

        default:
            throw new IllegalArgumentException();
        }

        Arrays.sort(selectedDims);

        int nselectedDims = selectedDims.length;
        int ndims = Control.checkEquals(srcD.length, dstD.length, //
                "Dimensionality mismatch");

        for (int i = 1; i < nselectedDims; i++) {
            Control.checkTrue(selectedDims[i - 1] != selectedDims[i], //
                    "Duplicate selected dimensions not allowed");
        }

        int acc = dstLen;

        for (int i = 0; i < nselectedDims; i++) {

            int dim = selectedDims[i];

            Control.checkTrue(dim >= 0 && dim < ndims, //
                    "Invalid dimension");

            Control.checkTrue(dstD[dim] <= 1, //
                    "Selected dimension must have singleton or zero length");

            acc *= srcD[dim];
        }

        Control.checkTrue(acc == srcLen, //
                "Invalid arguments");

        if (srcLen == 0) {
            return;
        }

        double[] workingV = srcV.clone();
        int[] workingD = srcD.clone();

        acc = srcLen;

        for (int i = 0; i < nselectedDims; i++) {

            int dim = selectedDims[i];

            acc /= srcD[dim];

            op.op(workingV, assignBaseIndices(acc, workingD, srcS, dim), //
                    workingD[dim], srcS[dim]);

            workingD[dim] = 1;
        }

        MappingOps.assign( //
                workingV, MappingOps.assignMappingIndices(dstLen, dstD, srcS), //
                dstV, MappingOps.assignMappingIndices(dstLen, dstD, dstS));
    }

    /**
     * Dimension index operations in support of {@link ArrayKernel#riOp(int, double[], int[], int[], int[], int)}.
     */
    final public static void riOp(int type, //
            double[] srcV, int[] srcD, int[] srcS, int[] dstV, //
            int dim) {

        int srcLen = MappingOps.checkDimensions(srcV.length, srcD, srcS);

        final RealIndexOperation op;

        switch (type) {

        case RI_MAX:
            op = RIMaxOp;
            break;

        case RI_MIN:
            op = RIMinOp;
            break;

        case RI_ZERO:
            op = RIZeroOp;
            break;

        case RI_GZERO:
            op = RIGZeroOp;
            break;

        case RI_LZERO:
            op = RILZeroOp;
            break;

        case RI_SORT:
            op = RISortOp;
            break;

        default:
            throw new IllegalArgumentException();
        }

        Control.checkTrue(srcLen == dstV.length, //
                "Invalid arguments");

        if (srcLen == 0) {
            return;
        }

        if (dim != -1) {

            op.op(srcV, assignBaseIndices(srcLen / srcD[dim], srcD, srcS, dim), //
                    dstV, srcD[dim], srcS[dim]);

        } else {

            op.op(srcV, null, dstV, -1, -1);
        }
    }

    /**
     * Dimension operations in support of {@link ArrayKernel#rdOp(int, double[], int[], int[], double[], int...)}.
     */
    final public static void rdOp(int type, //
            double[] srcV, int[] srcD, int[] srcS, double[] dstV, //
            int[] selectedDims) {

        int srcLen = MappingOps.checkDimensions(srcV.length, srcD, srcS);

        final RealDimensionOperation op;

        switch (type) {

        case RD_SUM:
            op = RDSumOp;
            break;

        case RD_PROD:
            op = RDProdOp;
            break;

        default:
            throw new IllegalArgumentException();
        }

        int ndims = Control.checkEquals(srcD.length, srcS.length, //
                "Dimensionality mismatch");

        for (int dim : selectedDims) {
            Control.checkTrue(dim >= 0 && dim < ndims, //
                    "Invalid dimension");
        }

        if (srcLen == 0) {
            return;
        }

        op.op(srcV, srcD, srcS, dstV, selectedDims);
    }

    // Dummy constructor.
    DimensionOps() {
    }
}
