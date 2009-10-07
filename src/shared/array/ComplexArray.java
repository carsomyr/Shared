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

package shared.array;

import static shared.array.ArrayBase.DEFAULT_ORDER;
import static shared.array.ArrayBase.IOKernel;
import static shared.array.ArrayBase.OpKernel;
import shared.util.Arithmetic;
import shared.util.Control;

/**
 * A multidimensional complex array class.
 * 
 * @author Roy Liu
 */
public class ComplexArray extends AbstractComplexArray<ComplexArray, RealArray> implements
        Matrix<ComplexArray, AbstractComplexArray.Complex> {

    /**
     * Default constructor.
     */
    public ComplexArray(int... dims) {
        this(0, dims.clone());
    }

    /**
     * Internal constructor with a distinctive signature.
     */
    protected ComplexArray(int unused, int[] dims) {
        super(new double[Arithmetic.product(dims)], INVALID_PARITY, dims, DEFAULT_ORDER.strides(dims));

        Control.checkTrue(dims.length >= 2 && dims[dims.length - 1] == 2);
    }

    /**
     * Alternate constructor.
     */
    public ComplexArray(double[] values, int... dims) {
        this(0, values, inferDimensions(dims, values.length, true));
    }

    /**
     * Alternate constructor.
     */
    public ComplexArray(ComplexArray array) {
        this(0, array.values.clone(), array.dims);
    }

    /**
     * Internal constructor with a distinctive signature.
     */
    protected ComplexArray(int unused, double[] values, int[] dims) {
        super(values, INVALID_PARITY, dims, DEFAULT_ORDER.strides(dims));

        Control.checkTrue(dims.length >= 2 && dims[dims.length - 1] == 2 //
                && values.length == Arithmetic.product(dims));
    }

    /**
     * Internal constructor for package use only.
     */
    protected ComplexArray(int parity, int[] dims, int[] strides) {
        super(new double[Arithmetic.product(dims)], parity, dims, strides);
    }

    /**
     * Internal constructor for package use only.
     */
    protected ComplexArray(double[] values, int parity, int[] dims) {
        super(values, parity, dims, DEFAULT_ORDER.strides(dims));
    }

    @Override
    protected ComplexArray wrap(int parity, IndexingOrder order, int[] dims, int[] strides) {
        return new ComplexArray(parity, dims, strides);
    }

    @Override
    protected RealArray wrapDown(int parity, IndexingOrder order, int[] dims, int[] strides) {
        return new RealArray(order, dims, strides);
    }

    public ComplexArray mMul(ComplexArray b) {

        ComplexArray a = this;

        Control.checkTrue(Control.checkEquals(a.dims.length, b.dims.length, //
                "Dimensionality mismatch") == 3, //
                "Arrays must have exactly three dimensions");

        // Matrices are already in matrix order.
        ComplexArray res = new ComplexArray(a.dims[0], b.dims[1], 2);

        OpKernel.mul(a.values, b.values, a.dims[0], b.dims[1], res.values, true);

        return res;
    }

    public ComplexArray mDiag() {

        ComplexArray a = this;

        Control.checkTrue(a.dims.length == 3, //
                "Array must have exactly three dimensions");

        int n = Control.checkEquals(a.dims[0], a.dims[1], //
                "Dimensionality mismatch");

        // Matrices are already in matrix order.
        ComplexArray res = new ComplexArray(n, 1, 2);

        OpKernel.diag(a.values, res.values, n, true);

        return res;
    }

    public ComplexArray mTranspose() {

        ComplexArray a = this;

        Control.checkTrue(a.dims.length == 3, //
                "Array must have exactly three dimensions");

        return transpose(1, 0, 2);
    }

    @Override
    public byte[] getBytes() {
        return IOKernel.getBytes(this);
    }

    /**
     * Parses an array from {@code byte}s.
     */
    final public static ComplexArray parse(byte[] data) {
        return IOKernel.parse(data);
    }

    public ComplexArray[] mSVD() {
        throw new UnsupportedOperationException(
                "Complex matrices currently do not support singular value decompositions");
    }

    public ComplexArray[] mEigs() {
        throw new UnsupportedOperationException("Complex matrices currently do not support eigenvalue decompositions");
    }

    public ComplexArray mInvert() {
        throw new UnsupportedOperationException("Complex matrices currently do not support inverses");
    }
}
