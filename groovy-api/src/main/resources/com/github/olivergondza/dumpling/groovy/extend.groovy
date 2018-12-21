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

import com.github.olivergondza.dumpling.model.ThreadSet
import com.github.olivergondza.dumpling.model.dump.ThreadDumpThreadSet
import com.github.olivergondza.dumpling.model.jmx.JmxThreadSet
import com.github.olivergondza.dumpling.model.jvm.JvmThreadSet

import org.codehaus.groovy.runtime.DefaultGroovyMethods

class ThreadSetExtensions extends DelegatingMetaClass {
    ThreadSetExtensions(MetaClass delegate) {
        super(delegate)
    }

    ThreadSetExtensions(Class theClass) {
        super(theClass)
    }

    @Override
    Object invokeMethod(Object object, String methodName, Object[] arguments) {
        ThreadSet lhs = (ThreadSet) object
        switch (methodName) {
            case "asImmutable":
            case "toSet":
                if (arguments.size() != 0) break
                return object
            case "grep":
                if (arguments.size() > 1) break
                if (arguments.size() == 0) return lhs.derive(lhs.threadsAsSet.grep())
                return lhs.derive(DefaultGroovyMethods.grep(lhs.threadsAsSet, arguments[0]))
            case "findAll":
                if (arguments.size() > 1) break
                if (arguments.size() == 0) return lhs.derive(lhs.threadsAsSet.findAll())
                return lhs.derive(DefaultGroovyMethods.findAll((Object) lhs.threadsAsSet, arguments[0]))
            case "intersect":
                if (arguments.size() != 1) break
                ThreadSet rhs = assertSetsCompatible(lhs, arguments[0])
                return lhs.derive(DefaultGroovyMethods.intersect((Collection) lhs.threadsAsSet, (Collection) rhs.threadsAsSet))
            case "plus":
                if (arguments.size() != 1) break
                ThreadSet rhs = assertSetsCompatible(lhs, arguments[0])
                return lhs.derive(DefaultGroovyMethods.plus(lhs.threadsAsSet, rhs.threadsAsSet))
        }

        return super.invokeMethod(object, methodName, arguments)
    }

    ThreadSet assertSetsCompatible(ThreadSet lhs, ThreadSet rhs) {
        if (!lhs.getProcessRuntime().equals(rhs.getProcessRuntime())) {
            throw new IllegalArgumentException('Arguments bound to different ProcessRuntimes')
        }

        return rhs
    }
}

ThreadSet.metaClass = new ThreadSetExtensions(ThreadSet.class)
JvmThreadSet.metaClass = new ThreadSetExtensions(JvmThreadSet.class)
JmxThreadSet.metaClass = new ThreadSetExtensions(JmxThreadSet.class)
ThreadDumpThreadSet.metaClass = new ThreadSetExtensions(ThreadDumpThreadSet.class)
