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

    private static final List<String> STATIC_IMPORTS = Arrays.asList(
            "com.github.olivergondza.dumpling.model.ProcessThread",
            "com.github.olivergondza.dumpling.groovy.Factories"
    );
    private static final List<String> STAR_IMPORTS = Arrays.asList(
            "com.github.olivergondza.dumpling.cli",
            "com.github.olivergondza.dumpling.factory",
            "com.github.olivergondza.dumpling.model",
            "com.github.olivergondza.dumpling.query"
    );
    /**
     * All class imports.
     */
    public Collection<String> getStarImports() {
        return STAR_IMPORTS;
    }

    /**
     * All static imports.
     */
    public Collection<String> getStaticStars() {
        return STATIC_IMPORTS;
    }

    /**
     * Get default compiler configuration for {@link GroovyShell}.
     */
    public CompilerConfiguration getCompilerConfiguration() {
        CompilerConfiguration cc = new CompilerConfiguration();
        ImportCustomizer imports = new ImportCustomizer();
        for (String starImport: STAR_IMPORTS) {
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
     *
     * @deprecated This is no longer needed to be called.
     */
    @Deprecated
    public void setupDecorateMethods() {
    }

    /**
     * Decorate Dumpling API with groovy extensions.
     *
     * @deprecated This is no longer needed to be called.
     */
    @Deprecated
    public void setupDecorateMethods(ClassLoader cl) {
    }
}
