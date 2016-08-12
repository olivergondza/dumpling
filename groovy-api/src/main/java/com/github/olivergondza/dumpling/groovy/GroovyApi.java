/*
 * The MIT License
 *
 * Copyright (c) Red Hat, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.github.olivergondza.dumpling.groovy;

import javax.annotation.Nonnull;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * @author ogondza.
 */
/*package*/ class GroovyApi {
    private static final String NL = System.getProperty("line.separator");

    protected final @Nonnull String initIndent;

    /*package*/
    public GroovyApi(@Nonnull String initIndent) {
        this.initIndent = initIndent;
    }

    /**
     * Generate refdoc.
     */
    @Override
    public final String toString() {
        StringBuilder sb = new StringBuilder();
        print(sb, getClass(), initIndent);
        return sb.toString();
    }

    private void print(StringBuilder sb, Class<?> base, String indent) {
        for (Method m : base.getMethods()) {

            ApiDoc doc = m.getAnnotation(ApiDoc.class);

            // Is declared in API?
            if (!isApi(m.getDeclaringClass())) continue;

            String header = groovyHeader(m);

            if (doc != null) {
                sb.append(indent).append(header).append(": ").append(type(m.getReturnType())).append(NL);
                final char[] padding = new char[indent.length()];
                Arrays.fill(padding, ' ');
                sb.append(padding).append(doc.text()).append(NL).append(NL);
            }

            Class<?> ret = m.getReturnType();
            if (isApi(ret)) {
                print(sb, ret, indent + header + '.');
            }
        }
    }

    private boolean isApi(Class<?> type) {
        return GroovyApi.class.isAssignableFrom(type);
    }

    private String groovyHeader(Method m) {
        String name = m.getName();
        Class<?>[] args = m.getParameterTypes();

        StringBuilder sb = new StringBuilder();
        if (args.length == 0) {
            if (!name.startsWith("get") || name.length() < 4) return String.format("%s()", name);

            // Property getter
            return String.format("%s%s", Character.toLowerCase(name.charAt(3)), name.substring(4));
        } else {
            sb.append(name).append('(');
            boolean first = true;
            for (Class<?> a : args) {
                if (!first) {
                    sb.append(", ");
                }
                sb.append(type(a));
                first = false;
            }
            sb.append(')');
        }

        return sb.toString();
    }

    private String type(Class<?> cls) {
        final Package pkg = cls.getPackage();

        return pkg != null && "java.lang".equals(pkg.getName())
                ? cls.getSimpleName()
                : cls.getCanonicalName()
        ;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    /*package*/ @interface ApiDoc {
        String text();
    }
}
