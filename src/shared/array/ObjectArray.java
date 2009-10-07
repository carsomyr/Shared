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

import static shared.array.ArrayBase.formatEmptyArray;
import static shared.array.ArrayBase.formatSlice;

import java.lang.reflect.Array;
import java.util.Formatter;

import shared.array.kernel.DimensionOps;
import shared.array.kernel.MappingOps;
import shared.array.kernel.PermutationEntry;
import shared.util.Arithmetic;
import shared.util.Control;

/**
 * A multidimensional object array class.
 * 
 * @apiviz.uses shared.array.ArrayBase
 * @param <T>
 *            the storage type.
 * @author Roy Liu
 */
public class ObjectArray<T> extends ProtoArray<ObjectArray<T>, T[], T> {

    /**
     * Default constructor.
     */
    public ObjectArray(Class<T> clazz, int... dims) {
        this(0, clazz, IndexingOrder.FAR, dims);
    }

    /**
     * Alternate constructor.
     */
    public ObjectArray(Class<T> clazz, IndexingOrder order, int... dims) {
        this(0, clazz, order, dims);
    }

    /**
     * Internal constructor with a distinctive signature.
     */
    @SuppressWarnings("unchecked")
    protected ObjectArray(int unused, Class<T> clazz, IndexingOrder order, int[] dims) {
        super((T[]) Array.newInstance(clazz, Arithmetic.product(dims)), order, dims, order.strides(dims));

        Control.checkTrue(dims.length > 0);
    }

    /**
     * Alternate constructor.
     */
    public ObjectArray(T[] values, int... dims) {
        this(0, values, IndexingOrder.FAR, //
                AbstractArray.inferDimensions(dims, values.length, false));
    }

    /**
     * Alternate constructor.
     */
    public ObjectArray(T[] values, IndexingOrder order, int... dims) {
        this(0, values, order, //
                AbstractArray.inferDimensions(dims, values.length, false));
    }

    /**
     * Alternate constructor.
     */
    public ObjectArray(ObjectArray<T> array) {
        this(0, array.values.clone(), array.order, array.dims);
    }

    /**
     * Internal constructor with a distinctive signature.
     */
    protected ObjectArray(int unused, T[] values, IndexingOrder order, int[] dims) {
        super(values, order, dims, order.strides(dims));

        Control.checkTrue(dims.length > 0 //
                && values.length == Arithmetic.product(dims));
    }

    /**
     * Internal constructor for package use only.
     */
    @SuppressWarnings("unchecked")
    protected ObjectArray(Class<T> clazz, IndexingOrder order, int[] dims, int[] strides) {
        super((T[]) Array.newInstance(clazz, Arithmetic.product(dims)), order, dims, strides);
    }

    @Override
    protected ObjectArray<T> wrap(IndexingOrder order, int[] dims, int[] strides) {
        return new ObjectArray<T>(getComponentType(), order, dims, strides);
    }

    @Override
    protected ObjectArray<T> wrap(T value, IndexingOrder order, int[] dims, int[] strides) {

        ObjectArray<T> tmp = wrap(order, dims, strides);
        java.util.Arrays.fill(tmp.values, value);

        return tmp;
    }

    @SuppressWarnings("unchecked")
    public Class<T> getComponentType() {
        return (Class<T>) this.values.getClass().getComponentType();
    }

    /**
     * Gets the value at the given logical index.
     */
    public T get(int... s) {
        return this.values[physical(s)];
    }

    /**
     * Sets the value at the given logical index.
     */
    public void set(T value, int... s) {
        this.values[physical(s)] = value;
    }

    @Override
    public T[] values() {
        return this.values;
    }

    @Override
    public byte[] getBytes() {
        throw new UnsupportedOperationException("Serialization of object arrays is not yet supported");
    }

    @Override
    public String toString() {

        T[] values = this.values;
        int[] dims = this.dims;
        int[] strides = this.strides;

        int ndims = dims.length;
        int nrows = (ndims == 1) ? 1 : size(ndims - 2);
        int ncols = size(ndims - 1);
        int sliceSize = nrows * ncols;

        Formatter f = new Formatter();

        if (values.length == 0) {

            formatEmptyArray(f, dims);

            return f.toString();
        }

        int[] indices = MappingOps.assignMappingIndices(Arithmetic.product(dims), //
                dims, strides);

        strides = IndexingOrder.FAR.strides(dims);

        if (ndims <= 2) {

            f.format("%n");

            formatSlice(f, " \"%s\"", //
                    values, indices, 0, nrows, ncols, false);

            return f.toString();
        }

        for (int offset = 0, m = values.length; offset < m; offset += sliceSize) {

            f.format("%n[slice (");

            for (int i = 0, n = ndims - 2, offsetAcc = offset; i < n; offsetAcc %= strides[i], i++) {
                f.format("%d, ", offsetAcc / strides[i]);
            }

            f.format(":, :)]%n");

            formatSlice(f, " \"%s\"", //
                    values, indices, offset, nrows, ncols, false);
        }

        return f.toString();
    }

    /**
     * Sorts along the given dimension.
     */
    @SuppressWarnings("unchecked")
    public IntegerArray iSort(int dim) {

        ObjectArray<T> src = this;
        IntegerArray dst = new IntegerArray(src.order, src.dims);

        T[] srcV = src.values;
        int[] srcD = src.dims;
        int[] srcS = src.strides;

        int[] dstV = dst.values();

        int srcLen = MappingOps.checkDimensions(srcV.length, srcD, srcS);

        Control.checkTrue(srcLen == dstV.length, //
                "Invalid arguments");

        if (srcLen == 0) {
            return dst;
        }

        if (dim != -1) {

            PermutationEntry.iSort(((ObjectArray<Comparable>) this).values(), //
                    DimensionOps.assignBaseIndices(srcLen / srcD[dim], srcD, srcS, dim), //
                    dstV, //
                    srcD[dim], srcS[dim]);

        } else {

            PermutationEntry.iSort(((ObjectArray<Comparable>) this).values(), //
                    null, dstV, -1, -1);
        }

        return dst;
    }
}
