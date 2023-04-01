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
package org.openrewrite.java.migrate.guava;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

public class NoGuavaAtomicsNewReference extends Recipe {
    private static final MethodMatcher NEW_ATOMIC_REFERENCE = new MethodMatcher("com.google.common.util.concurrent.Atomics newReference(..)");

    @Override
    public String getDisplayName() {
        return "Prefer `new AtomicReference<>()`";
    }

    @Override
    public String getDescription() {
        return "Prefer the Java standard library over third-party usage of Guava in simple cases like this.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("guava");
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getApplicableTest() {
        return new UsesMethod<>(NEW_ATOMIC_REFERENCE);
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            private final JavaTemplate newAtomicReference = JavaTemplate.builder(this::getCursor, "new AtomicReference<>()")
                    .imports("java.util.concurrent.atomic.AtomicReference")
                    .build();

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                if (NEW_ATOMIC_REFERENCE.matches(method)) {
                    maybeRemoveImport("com.google.common.util.concurrent.Atomics");
                    maybeAddImport("java.util.concurrent.atomic.AtomicReference");
                    return ((J.NewClass) method.withTemplate(newAtomicReference, method.getCoordinates().replace()))
                            .withArguments(method.getArguments());
                }
                return super.visitMethodInvocation(method, ctx);
            }
        };
    }
}
