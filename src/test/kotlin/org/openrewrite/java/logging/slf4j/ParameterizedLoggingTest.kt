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

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.Recipe
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

class ParameterizedLoggingTest : JavaRecipeTest {
    override val parser: JavaParser = JavaParser.fromJavaVersion()
        .logCompilationWarningsAndErrors(true)
        .classpath("slf4j")
        .build()

    override val recipe: Recipe
        get() = ParameterizedLogging()

    @Test
    fun basicParameterization() = assertChanged(
        before = """
            import org.slf4j.Logger;
            import org.slf4j.LoggerFactory;

            class Test {
                Logger logger = LoggerFactory.getLogger(Test.class);

                void method() {
                    String name = "Jon";
                    logger.info("Info! Hello " + name + ", nice to meet you " + name);
                    logger.warn("Warn! Hello " + name + ", nice to meet you " + name);
                    logger.debug("Debug! Hello " + name + ", nice to meet you " + name);
                    logger.trace("Trace! Hello " + name + ", nice to meet you " + name);
                    logger.error("Error! Hello " + name + ", nice to meet you " + name);
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
                    logger.info("Info! Hello {}, nice to meet you {}", name, name);
                    logger.warn("Warn! Hello {}, nice to meet you {}", name, name);
                    logger.debug("Debug! Hello {}, nice to meet you {}", name, name);
                    logger.trace("Trace! Hello {}, nice to meet you {}", name, name);
                    logger.error("Error! Hello {}, nice to meet you {}", name, name);
                }
            }
        """
    )

    @Test
    @Disabled
    @Issue("https://github.com/openrewrite/rewrite-logging-frameworks/issues/30")
    fun escapeMessageStrings() = assertChanged(
        before = """
            import org.slf4j.Logger;
            import org.slf4j.LoggerFactory;

            class Test {
                Logger logger = LoggerFactory.getLogger(Test.class);

                void method(String text) {
                    logger.info("See link #" + text);
                }
            }
        """,
        after = """
            import org.slf4j.Logger;
            import org.slf4j.LoggerFactory;

            class Test {
                Logger logger = LoggerFactory.getLogger(Test.class);

                void method(String text) {
                    logger.info("See link #{}", text);
                }
            }
        """
    )

    @Test
    fun exceptionArgumentsAsConcatenatedString() = assertChanged(
        before = """
            import org.slf4j.Logger;
            import org.slf4j.LoggerFactory;

            class Test {
                Logger logger = LoggerFactory.getLogger(Test.class);

                void asInteger(String numberString) {
                    try {
                        Integer i = Integer.valueOf(numberString);
                    } catch (NumberFormatException ex) {
                        logger.debug("some big error: " + ex);
                    }
                }
            }
        """,
        after = """
            import org.slf4j.Logger;
            import org.slf4j.LoggerFactory;

            class Test {
                Logger logger = LoggerFactory.getLogger(Test.class);

                void asInteger(String numberString) {
                    try {
                        Integer i = Integer.valueOf(numberString);
                    } catch (NumberFormatException ex) {
                        logger.debug("some big error: {}", ex);
                    }
                }
            }
        """
    )

    @Test
    fun exceptionArgumentsWithThrowable() = assertChanged(
        before = """
            import org.slf4j.Logger;
            import org.slf4j.LoggerFactory;

            class Test {
                Logger logger = LoggerFactory.getLogger(Test.class);

                void asInteger(String numberString) {
                    try {
                        Integer i = Integer.valueOf(numberString);
                    } catch (NumberFormatException ex) {
                        logger.warn("some big error: " + ex.getMessage(), ex);
                    }
                }
            }
        """,
        after = """
            import org.slf4j.Logger;
            import org.slf4j.LoggerFactory;

            class Test {
                Logger logger = LoggerFactory.getLogger(Test.class);

                void asInteger(String numberString) {
                    try {
                        Integer i = Integer.valueOf(numberString);
                    } catch (NumberFormatException ex) {
                        logger.warn("some big error: {}", ex.getMessage(), ex);
                    }
                }
            }
        """
    )

    @Test
    fun noChangeRequired() = assertUnchanged(
        before = """
            import org.slf4j.Logger;
            import org.slf4j.LoggerFactory;

            class A {
                Logger logger = LoggerFactory.getLogger(A.class);

                void asInteger(String numberString) {
                    try {
                        Integer i = Integer.valueOf(numberString);
                    } catch (NumberFormatException ex) {
                        logger.warn("Invalid parameter: {}", ex.getMessage(), ex);
                    }
                }
            }
        """
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite-logging-frameworks/issues/26")
    fun argumentsContainingBinaryExpressions() = assertChanged(
        before = """
            import org.slf4j.Logger;
            import org.slf4j.LoggerFactory;

            class Test {
                Logger logger = LoggerFactory.getLogger(Test.class);

                void method(String name, double percent) {
                    logger.debug("Process [" + name + "] is at [" + percent * 100 + "%]");
                }
            }
        """,
        after = """
            import org.slf4j.Logger;
            import org.slf4j.LoggerFactory;

            class Test {
                Logger logger = LoggerFactory.getLogger(Test.class);

                void method(String name, double percent) {
                    logger.debug("Process [{}] is at [{}%]", name, percent * 100);
                }
            }
        """
    )

}