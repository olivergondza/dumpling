/*
 * The MIT License
 *
 * Copyright (c) 2015 Red Hat, Inc.
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
package com.github.olivergondza.dumpling.groovy

import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ImportCustomizer
import org.junit.Test

class GroovyRuntimeTest {

    private static final GroovyInterpretterConfig CONFIG = new GroovyInterpretterConfig();

    def runScript(String script) {
        CONFIG.setupDecorateMethods();

        CompilerConfiguration cc = new CompilerConfiguration();
        ImportCustomizer imports = new ImportCustomizer();
        for (String starImport: CONFIG.getStarImports()) {
            imports.addStarImports(starImport);
        }
        for (String staticStar: CONFIG.getStaticStars()) {
            imports.addStaticStars(staticStar);
        }
        cc.addCompilationCustomizers(imports);

        GroovyShell shell = new GroovyShell(cc);
        return shell.run("def rt = new JvmRuntimeFactory().currentRuntime();" + script, "dumpling-script", Arrays.asList());
    }

    @Test
    void size() {
        runScript("assert rt.threads.size() > 1");
    }

    @Test
    void queries() {
        runScript("rt.threads.query(blockingTree()) instanceof BlockingTree.Result")
        runScript("rt.threads.query(deadlocks()) instanceof Deadlocks.Result")
        runScript("rt.threads.query(topContenders()) instanceof TopContenders.Result")
    }
}
