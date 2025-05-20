/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

rootProject.name = "ktfmt-parent"

include(
    ":ktfmt",
    ":lambda",
    ":idea_plugin",
)

project(":ktfmt").projectDir = file("core")

project(":lambda").projectDir = file("online_formatter")

project(":idea_plugin").projectDir = file("ktfmt_idea_plugin")

dependencyResolutionManagement {
  versionCatalogs {
    create("libs") {
      val ktfmtVersion = providers.gradleProperty("ktfmt.version").get()
      version("ktfmt", ktfmtVersion)
    }
  }
}
