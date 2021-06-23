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
package org.openrewrite.java.logging.slf4j;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @see <a href="http://www.slf4j.org/migrator.html">SLF4J Migrator</a>
 */
public class Log4jToSlf4jStringifyLoggerParameters extends Recipe {
    @Override
    public String getDisplayName() {
        return "Stringify logger parameters";
    }

    @Override
    public String getDescription() {
        return "Ensure non-String object parameters passed to the logger use `toString()`.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext executionContext) {
                doAfterVisit(new UsesType<>("org.apache.log4j.Logger"));
                doAfterVisit(new UsesType<>("org.apache.log4j.Category"));
                return cu;
            }
        };
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new Log4jToSlf4jStringifyLoggerParametersVisitor();
    }

    private static class Log4jToSlf4jStringifyLoggerParametersVisitor extends JavaIsoVisitor<ExecutionContext> {
        private final List<MethodMatcher> logLevelMatchers = Stream.of("trace", "debug", "info", "warn", "error", "fatal")
                .map(level -> "org.apache.log4j." + (level.equals("trace") ? "Logger" : "Category") +
                        " " + level + "(..)")
                .map(MethodMatcher::new)
                .collect(Collectors.toList());

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

            for (MethodMatcher matcher : logLevelMatchers) {
                if (matcher.matches(m)) {
                    List<Expression> args = method.getArguments();
                    if (!args.isEmpty()) {
                        Expression message = args.iterator().next();
                        if (!TypeUtils.isString(message.getType())) {
                            if (message.getType() instanceof JavaType.Class) {
                                m = m.withTemplate(
                                        template("#{any(java.lang.Object)}.toString()").build(),
                                        m.getCoordinates().replaceArguments(),
                                        message
                                );
                            }
                        }
                    }
                }

            }
            return m;

        }

    }

}
