/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.logging.slf4j

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.config.Environment
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

class Log4jToSlf4jTest : JavaRecipeTest {
    override val parser: JavaParser = JavaParser.fromJavaVersion()
        .logCompilationWarningsAndErrors(true)
        .classpath("log4j", "slf4j")
        .build()

    override val recipe: Recipe = Environment.builder()
        .scanRuntimeClasspath("org.openrewrite.java.logging.slf4j")
        .build()
        .activateRecipes("org.openrewrite.java.logging.slf4j.SLF4JBestPractices")

    @Test
    fun migratesLoggerToLoggerFactory() = assertChanged(
        before = """
            import org.apache.log4j.Logger;

            class Test {
                Logger logger = Logger.getLogger(Test.class);
            }
        """,
        after = """
            import org.slf4j.Logger;
            import org.slf4j.LoggerFactory;

            class Test {
                Logger logger = LoggerFactory.getLogger(Test.class);
            }
        """
    )

    @Test
    fun migratesFatalToError() = assertChanged(
        before = """
            import org.apache.log4j.Logger;

            class Test {
                Logger logger = Logger.getLogger(Test.class);

                void method() {
                    logger.fatal("uh oh");
                }
            }
        """,
        after = """
            import org.slf4j.Logger;
            import org.slf4j.LoggerFactory;

            class Test {
                Logger logger = LoggerFactory.getLogger(Test.class);

                void method() {
                    logger.error("uh oh");
                }
            }
        """
    )

    @Test
    fun objectParametersToString() = assertChanged(
        before = """
            import org.apache.log4j.Logger;

            class Test {
                Logger logger = Logger.getLogger(Test.class);

                void method(Test test) {
                    logger.info(test);
                    logger.info(new Object());
                }
            }
        """,
        after = """
            import org.slf4j.Logger;
            import org.slf4j.LoggerFactory;

            class Test {
                Logger logger = LoggerFactory.getLogger(Test.class);

                void method(Test test) {
                    logger.info(test.toString());
                    logger.info(new Object().toString());
                }
            }
        """
    )

    @Test
    fun usesParameterizedLogging() = assertChanged(
        before = """
            import org.apache.log4j.Logger;

            class Test {
                Logger logger = Logger.getLogger(Test.class);

                void method() {
                    String name = "Jon";
                    logger.info("Hello " + name + ", nice to meet you " + name);
                }
            }
        """,
        after = """
            import org.slf4j.Logger;
            import org.slf4j.LoggerFactory;

            class Test {
                Logger logger = LoggerFactory.getLogger(Test.class);

                void method() {
                    String name = "Jon";
                    logger.info("Hello {}, nice to meet you {}", name, name);
                }
            }
        """
    )

}