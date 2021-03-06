/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package jdk.vm.ci.code.test;

import java.util.Arrays;

import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.JavaValue;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaField;
import jdk.vm.ci.meta.ResolvedJavaType;
import jdk.vm.ci.runtime.JVMCI;

public abstract class VirtualObjectTestBase {

    public static class SimpleObject {
        int i1;
        int i2;
        int i3;
        int i4;
        int i5;
        int i6;
    }

    public static JavaConstant getValue(JavaKind kind) {
        long dummyValue = kind.ordinal();
        dummyValue = dummyValue | dummyValue << 8;
        dummyValue = dummyValue | dummyValue << 16;
        dummyValue = dummyValue | dummyValue << 32;
        if (kind.isNumericInteger()) {
            return JavaConstant.forIntegerKind(kind, dummyValue);
        } else if (kind == JavaKind.Float) {
            return JavaConstant.forDouble(Double.longBitsToDouble(dummyValue));
        } else if (kind == JavaKind.Float) {
            return JavaConstant.forFloat(Float.intBitsToFloat((int) dummyValue));
        } else {
            return JavaConstant.NULL_POINTER;
        }
    }

    public static JavaValue[] getJavaValues(JavaKind[] kinds) {
        JavaValue[] values = new JavaValue[kinds.length];
        for (int i = 0; i < kinds.length; i++) {
            values[i] = getValue(kinds[i]);
        }
        return values;
    }

    /**
     * Subclasses are expected to override this method to provide their own verification logic using
     * the normal JUnit {@link org.junit.Assert} methods.
     *
     * @param klass class for the {@link jdk.vm.ci.code.VirtualObject}
     * @param kinds {@link JavaKind Javakinds} for values
     * @param values {@link JavaValue values} for materializing the
     *            {@link jdk.vm.ci.code.VirtualObject}
     * @param malformed indicates whether the resulting virtual object is considered to be properly
     *            formed relative to the fields of {@code klass}
     * @throws AssertionError if a problem is detected
     */
    protected abstract void test(ResolvedJavaType klass, JavaValue[] kinds, JavaKind[] values, boolean malformed);

    public void testBase() {
        MetaAccessProvider metaAccess = JVMCI.getRuntime().getHostJVMCIBackend().getMetaAccess();

        ResolvedJavaType simple = metaAccess.lookupJavaType(SimpleObject.class);
        ResolvedJavaField[] fields = simple.getInstanceFields(true);

        JavaKind[] fieldKinds = new JavaKind[fields.length];
        for (int i = 0; i < fields.length; i++) {
            fieldKinds[i] = fields[i].getType().getJavaKind();
        }

        // Generate a straightforward VirtualObject with values that match to declared field types.
        JavaKind[] kinds = fieldKinds.clone();
        JavaValue[] values = getJavaValues(kinds);
        test(simple, values, kinds, false);

        // Spread a long value across two int fields
        kinds = Arrays.copyOf(fieldKinds, fieldKinds.length - 1);
        kinds[1] = JavaKind.Long;
        test(simple, getJavaValues(kinds), kinds, false);

        // Produce a long value for the final int field so there is no matching int field for the
        // second half of the long
        kinds = fieldKinds.clone();
        kinds[kinds.length - 1] = JavaKind.Long;
        test(simple, getJavaValues(kinds), kinds, true);

        // Not enough values for the fields.
        kinds = Arrays.copyOf(fieldKinds, fieldKinds.length - 1);
        test(simple, getJavaValues(kinds), kinds, true);

        // Too many values for the fields.
        kinds = Arrays.copyOf(fieldKinds, fieldKinds.length + 1);
        kinds[kinds.length - 1] = JavaKind.Int;
        test(simple, getJavaValues(kinds), kinds, true);
    }
}
