/*
 * Copyright 2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.integtests

import org.gradle.integtests.fixtures.TargetVersions

@TargetVersions("8.0.2")
class PropertyUpgradesBinaryCompatibilityCrossVersionSpec extends AbstractPropertyUpgradesBinaryCompatibilityCrossVersionSpec {

    def "can use upgraded Checkstyle in a Groovy plugin compiled with a previous Gradle version"() {
        given:
        prepareGroovyPluginTest """
            project.tasks.register("myCheckstyle", Checkstyle) {
                maxErrors = 1
                int currentMaxErrors = maxErrors
                assert currentMaxErrors == 1
            }
        """

        expect:
        succeedsWithPluginCompiledWithPreviousVersion()
    }

    def "can use upgraded Checkstyle in a Java plugin compiled with a previous Gradle version"() {
        given:
        prepareJavaPluginTest """
            project.getPlugins().apply(ApplicationPlugin.class);

            project.getTasks().register("myCheckstyle", Checkstyle.class, it -> {
                it.setMaxErrors(1);
                int currentMaxErrors = it.getMaxErrors();
                assert currentMaxErrors == 1;
            });

            {
                JavaApplication application = project.getExtensions().getByType(JavaApplication.class);
                application.setApplicationName("myapp");
                String applicationName = application.getApplicationName();
                assert "myapp".equals(applicationName);
                application.setExecutableDir("mydir");
                String executableDir = application.getExecutableDir();
                assert "mydir".equals(executableDir);
            }

            project.getTasks().register("createStartScripts", org.gradle.jvm.application.tasks.CreateStartScripts.class, it -> {
                it.setApplicationName("myapp");
                String applicationName = it.getApplicationName();
                assert "myapp".equals(applicationName);

                it.setExecutableDir("mydir");
                String executableDir = it.getExecutableDir();
                assert "mydir".equals(executableDir);

                it.setOptsEnvironmentVar("myops");
                String optsEnvironmentVar = it.getOptsEnvironmentVar();
                assert "myops".equals(optsEnvironmentVar);

                it.setExitEnvironmentVar("myenvvar");
                String exitEnvironmentVar = it.getExitEnvironmentVar();
                assert "myenvvar".equals(exitEnvironmentVar);
            });
        """

        expect:
        succeedsWithPluginCompiledWithPreviousVersion()
    }

    def "can use upgraded Checkstyle in a Kotlin plugin compiled with a previous Gradle version"() {
        given:
        prepareKotlinPluginTest """
            project.tasks.register("myCheckstyle", Checkstyle::class.java) {
                maxErrors = 1
                val currentMaxErrors = maxErrors
                assert(currentMaxErrors == 1)
            }
        """

        expect:
        succeedsWithPluginCompiledWithPreviousVersion()
    }

    def succeedsWithPluginCompiledWithPreviousVersion() {
        version previous withTasks 'assemble' inDirectory(file("producer")) run()
        version current withTasks 'tasks' withStacktraceEnabled() requireDaemon() requireIsolatedDaemons() run()
    }
}

