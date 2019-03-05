/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.mycompany.app

import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Source
import java.io.IOException
import javax.script.Invocable
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager

/**
 * Simple benchmark for Graal.js via GraalVM Polyglot Context and ScriptEngine.
 */
object App {

    val WARMUP = 15
    val ITERATIONS = 10
    val BENCHFILE = "src/bench.js"

    val SOURCE = (""
            + "var N = 2000;\n"
            + "var EXPECTED = 17393;\n"
            + "\n"
            + "function Natural() {\n"
            + "    x = 2;\n"
            + "    return {\n"
            + "        'next' : function() { return x++; }\n"
            + "    };\n"
            + "}\n"
            + "\n"
            + "function Filter(number, filter) {\n"
            + "    var self = this;\n"
            + "    this.number = number;\n"
            + "    this.filter = filter;\n"
            + "    this.accept = function(n) {\n"
            + "      var filter = self;\n"
            + "      for (;;) {\n"
            + "          if (n % filter.number === 0) {\n"
            + "              return false;\n"
            + "          }\n"
            + "          filter = filter.filter;\n"
            + "          if (filter === null) {\n"
            + "              break;\n"
            + "          }\n"
            + "      }\n"
            + "      return true;\n"
            + "    };\n"
            + "    return this;\n"
            + "}\n"
            + "\n"
            + "function Primes(natural) {\n"
            + "    var self = this;\n"
            + "    this.natural = natural;\n"
            + "    this.filter = null;\n"
            + "\n"
            + "    this.next = function() {\n"
            + "        for (;;) {\n"
            + "            var n = self.natural.next();\n"
            + "            if (self.filter === null || self.filter.accept(n)) {\n"
            + "                self.filter = new Filter(n, self.filter);\n"
            + "                return n;\n"
            + "            }\n"
            + "        }\n"
            + "    };\n"
            + "}\n"
            + "\n"
            + "function primesMain() {\n"
            + "    var primes = new Primes(Natural());\n"
            + "    var primArray = [];\n"
            + "    for (var i=0;i<=N;i++) { primArray.push(primes.next()); }\n"
            + "    if (primArray[N] != EXPECTED) { throw new Error('wrong prime found: '+primArray[N]); }\n"
            + "}\n")

    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        println("Hello, I have: ${System.getProperty("SOMEFOO")}")
        benchGraalPolyglotContext()
        benchGraalScriptEngine()
        benchNashornScriptEngine()
    }

    @Throws(IOException::class)
    internal fun benchGraalPolyglotContext(): Long {
        println("=== Graal.js via org.graalvm.polyglot.Context === ")
        var took: Long = 0
        Context.create().use { context ->
            context.eval(Source.newBuilder("js", SOURCE, "src.js").build())
            val primesMain = context.getBindings("js").getMember("primesMain")
            println("warming up ...")
            for (i in 0 until WARMUP) {
                primesMain.execute()
            }
            println("warmup finished, now measuring")
            for (i in 0 until ITERATIONS) {
                val start = System.currentTimeMillis()
                primesMain.execute()
                took = System.currentTimeMillis() - start
                println("iteration: $took")
            }
        } // context.close() is automatic
        return took
    }

    @Throws(IOException::class)
    internal fun benchNashornScriptEngine(): Long {
        println("=== Nashorn via javax.script.ScriptEngine ===")
        val nashornEngine = ScriptEngineManager().getEngineByName("nashorn")
        if (nashornEngine == null) {
            println("*** Nashorn not found ***")
            return 0
        } else {
            return benchScriptEngineIntl(nashornEngine)
        }
    }

    @Throws(IOException::class)
    internal fun benchGraalScriptEngine(): Long {
        println("=== Graal.js via javax.script.ScriptEngine ===")
        val graaljsEngine = ScriptEngineManager().getEngineByName("graal.js")
        if (graaljsEngine == null) {
            println("*** Graal.js not found ***")
            return 0
        } else {
            return benchScriptEngineIntl(graaljsEngine)
        }
    }

    @Throws(IOException::class)
    private fun benchScriptEngineIntl(eng: ScriptEngine): Long {
        var took = 0L
        try {
            eng.eval(SOURCE)
            val inv = eng as Invocable
            println("warming up ...")
            for (i in 0 until WARMUP) {
                inv.invokeFunction("primesMain")
            }
            println("warmup finished, now measuring")
            for (i in 0 until ITERATIONS) {
                val start = System.currentTimeMillis()
                inv.invokeFunction("primesMain")
                took = System.currentTimeMillis() - start
                println("iteration: $took")
            }
        } catch (ex: Exception) {
            println(ex)
        }

        return took
    }

}
