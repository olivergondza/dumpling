/*
 * The MIT License
 *
 * Copyright (c) 2014 Red Hat, Inc.
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

import groovy.lang.GroovyShell;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;

/**
 * Groovy interpreter config suitable for running Dumpling CLI.
 *
 * @author ogondza
 */
public class GroovyInterpretterConfig {

    private static final List<String> STATIC_IMPORTS = Arrays.asList("com.github.olivergondza.dumpling.model.ProcessThread");
    private static final List<String> IMPORTS = Arrays.asList(
            "com.github.olivergondza.dumpling.cli",
            "com.github.olivergondza.dumpling.factory",
            "com.github.olivergondza.dumpling.model",
            "com.github.olivergondza.dumpling.query"
    );
    /**
     * All class imports.
     */
    public Collection<String> getStarImports() {
        return IMPORTS;
    }

    /**
     * All static imports.
     */
    public Collection<String> getStaticStars() {
        return STATIC_IMPORTS;
    }

    public CompilerConfiguration getCompilerConfiguration() {
        CompilerConfiguration cc = new CompilerConfiguration();
        ImportCustomizer imports = new ImportCustomizer();
        for (String starImport: IMPORTS) {
            imports.addStarImports(starImport);
        }
        for (String staticStar: STATIC_IMPORTS) {
            imports.addStaticStars(staticStar);
        }
        cc.addCompilationCustomizers(imports);
        return cc;
    }

    /**
     * Decorate Dumpling API with groovy extensions.
     */
    public void setupDecorateMethods() {
        synchronized (IMPORTS) {
            if (DECORATED) return;

            GroovyShell shell = new GroovyShell(getCompilerConfiguration());
            try {
                shell.run(
                        "import org.codehaus.groovy.runtime.DefaultGroovyMethods;" +
                        "def mc = ThreadSet.metaClass;" +
                        "mc.asImmutable << { -> delegate };" +
                        "mc.toSet << { -> delegate };" +
                        "mc.grep << { Object filter -> delegate.derive(DefaultGroovyMethods.grep(delegate.threadsAsSet, filter)) };" +
                        "mc.grep << { -> delegate.derive(delegate.threadsAsSet.grep()) };" +
                        "mc.findAll << { Closure closure -> delegate.derive(DefaultGroovyMethods.findAll((Object) delegate.threadsAsSet, closure)) };" +
                        "mc.findAll << { -> delegate.derive(delegate.threadsAsSet.findAll()) };" +
                        "mc.intersect << { rhs -> if (!delegate.getProcessRuntime().equals(rhs.getProcessRuntime())) throw new IllegalArgumentException('Unable to intersect ThreadSets bound to different ProcessRuntimes'); return delegate.derive(DefaultGroovyMethods.intersect(delegate.threadsAsSet, rhs.threadsAsSet)) };" +
                        "mc.plus << { rhs -> if (!delegate.getProcessRuntime().equals(rhs.getProcessRuntime())) throw new IllegalArgumentException('Unable to merge ThreadSets bound to different ProcessRuntimes'); return delegate.derive(DefaultGroovyMethods.plus(delegate.threadsAsSet, rhs.threadsAsSet)) };",
                        "dumpling-metaclass-setup",
                        Arrays.asList()
                );
            } catch (Exception ex) {
                throw new AssertionError("Unable to decorate object model", ex);
            }
            DECORATED = true;
        }
    }
    private static boolean DECORATED = false;
}
