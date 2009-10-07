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

import shared.array.sparse.SparseArrayState;
import shared.util.Arithmetic;

/**
 * A pure Java implementation of {@link ArrayKernel}.
 * 
 * @apiviz.uses shared.array.kernel.DimensionOps
 * @apiviz.uses shared.array.kernel.ElementOps
 * @apiviz.uses shared.array.kernel.IndexOps
 * @apiviz.uses shared.array.kernel.LinearAlgebraOps
 * @apiviz.uses shared.array.kernel.MappingOps
 * @apiviz.uses shared.array.kernel.MatrixOps
 * @apiviz.uses shared.array.kernel.SparseOps
 * @author Roy Liu
 */
public class JavaArrayKernel implements ArrayKernel {

    /**
     * Default constructor.
     */
    public JavaArrayKernel() {
    }

    //

    public void randomize() {
        Arithmetic.randomize();
    }

    public void derandomize() {
        Arithmetic.derandomize();
    }

    //

    public void map(int[] bounds, //
            Object srcV, int[] srcD, int[] srcS, //
            Object dstV, int[] dstD, int[] dstS) {
        MappingOps.map(bounds, srcV, srcD, srcS, dstV, dstD, dstS);
    }

    public void slice( //
            int[] slices, //
            Object srcV, int[] srcD, int[] srcS, //
            Object dstV, int[] dstD, int[] dstS) {
        MappingOps.slice(slices, srcV, srcD, srcS, dstV, dstD, dstS);
    }

    //

    public void rrOp(int type, //
            double[] srcV, int[] srcD, int[] srcS, //
            double[] dstV, int[] dstD, int[] dstS, //
            int... selectedDims) {
        DimensionOps.rrOp(type, srcV, srcD, srcS, dstV, dstD, dstS, selectedDims);
    }

    public void riOp(int type, //
            double[] srcV, int[] srcD, int[] srcS, int[] dstV, //
            int dim) {
        DimensionOps.riOp(type, srcV, srcD, srcS, dstV, dim);
    }

    public void rdOp(int type, //
            double[] srcV, int[] srcD, int[] srcS, double[] dstV, //
            int... selectedDims) {
        DimensionOps.rdOp(type, srcV, srcD, srcS, dstV, selectedDims);
    }

    //

    public double raOp(int type, double[] srcV) {
        return ElementOps.raOp(type, srcV);
    }

    public double[] caOp(int type, double[] srcV) {
        return ElementOps.caOp(type, srcV);
    }

    public void ruOp(int type, double a, double[] srcV) {
        ElementOps.ruOp(type, a, srcV);
    }

    public void cuOp(int type, double aRe, double aIm, double[] srcV) {
        ElementOps.cuOp(type, aRe, aIm, srcV);
    }

    public void iuOp(int type, int a, int[] srcV) {
        ElementOps.iuOp(type, a, srcV);
    }

    public void eOp(int type, Object lhsV, Object rhsV, Object dstV, boolean isComplex) {
        ElementOps.eOp(type, lhsV, rhsV, dstV, isComplex);
    }

    public void convert(int type, Object srcV, boolean isSrcComplex, Object dstV, boolean isDstComplex) {
        ElementOps.convert(type, srcV, isSrcComplex, dstV, isDstComplex);
    }

    //

    public void mul(double[] lhsV, double[] rhsV, int lr, int rc, double[] dstV, boolean isComplex) {
        MatrixOps.mul(lhsV, rhsV, lr, rc, dstV, isComplex);
    }

    public void diag(double[] srcV, double[] dstV, int size, boolean isComplex) {
        MatrixOps.diag(srcV, dstV, size, isComplex);
    }

    //

    public void svd(double[] srcV, int srcStrideRow, int srcStrideCol, //
            double[] uV, double[] sV, double[] vV, //
            int nrows, int ncols) {
        LinearAlgebraOps.svd(srcV, srcStrideRow, srcStrideCol, uV, sV, vV, nrows, ncols);
    }

    public void eigs(double[] srcV, double[] vecV, double[] valV, int size) {
        LinearAlgebraOps.eigs(srcV, vecV, valV, size);
    }

    public void invert(double[] srcV, double[] dstV, int size) {
        LinearAlgebraOps.invert(srcV, dstV, size);
    }

    //

    public int[] find(int[] srcV, int[] srcD, int[] srcS, int[] logical) {
        return IndexOps.find(srcV, srcD, srcS, logical);
    }

    //

    public <V> SparseArrayState<V> insertSparse( //
            V oldV, int[] oldD, int[] oldS, int[] oldDO, int[] oldI, //
            V newV, int[] newI) {
        return SparseOps.insert(oldV, oldD, oldS, oldDO, oldI, newV, newI);
    }

    public <V> SparseArrayState<V> sliceSparse(int[] slices, //
            V srcV, int[] srcD, int[] srcS, int[] srcDO, //
            int[] srcI, int[] srcIO, int[] srcII, //
            V dstV, int[] dstD, int[] dstS, int[] dstDO, //
            int[] dstI, int[] dstIO, int[] dstII) {
        return SparseOps.slice(slices, //
                srcV, srcD, srcS, srcDO, //
                srcI, srcIO, srcII, //
                dstV, dstD, dstS, dstDO, //
                dstI, dstIO, dstII);
    }
}
