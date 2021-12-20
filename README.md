<img src="./banner.png" alt="bundle tool gradle plugin" width="771px">

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/me.2bab/bundle-tool-plugin/badge.svg)](https://search.maven.org/artifact/me.2bab/bundle-tool-plugin)
[![Actions Status](https://github.com/2bab/bundle-tool-gradle-plugin/workflows/CI/badge.svg)](https://github.com/2bab/bundle-tool-gradle-plugin/actions)
[![Apache 2](https://img.shields.io/badge/License-Apache%202-brightgreen.svg)](https://www.apache.org/licenses/LICENSE-2.0)

A Gradle Plugin for Android BundleTool. It supports:

1. Generate **".apks"** artifacts by `build-apks` command (When running in universal build mode, it also extracts universal **".apk"** artifact.)
2. Calculate **".apks"** size by `get-size total` command and export to **".csv"** files.

## Usage

**0x01. Add the plugin to classpath:**

``` kotlin
buildscript {
    repositories {
        ...
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.0.4")
        classpath("me.2bab:bundle-tool-plugin:1.1.0")
    }
}
```

**0x02. Apply Plugin:**

``` kotlin
// For your application module
plugins {
    id("me.2bab.bundletool")
}
```

**0x03. Advanced Configurations**

``` kotlin
import me.xx2bab.bundletool.*

bundleTool {
    // The plugin can be enabled by variant, for instance,
    // BundleToolFeature.GET_SIZE feature is only enabled for "debug" variant.
    enableByVariant { variant, feature ->
        !(variant.name.contains("debug", true) && feature == BundleToolFeature.GET_SIZE)
    }
    
    // Each of them will create a work action with `build-apks` command
    buildApks {
        create("universal") {
            buildMode.set(ApkBuildMode.UNIVERSAL.name)
        }
        create("pixel4a") {
            deviceSpec.set(file("./pixel4a.json"))
        }
    }

    // Each of them will create a work action for above "buildApks" list items' output
    getSize {
        create("all") {
            dimensions.addAll(
                GetSizeDimension.SDK.name,
                GetSizeDimension.ABI.name,
                GetSizeDimension.SCREEN_DENSITY.name,
                GetSizeDimension.LANGUAGE.name)
        }
    }
}
```

**0x04. Build your App and Enjoy!**

```shell
# Please check "enableByVariant" config to ensure you are running the one
# that enabled features you want already.
./gradlew TransformApksFromBundleForProductionRelease
```
![](./transform_result.png)

## Compatible

bundle-tool-gradle-plugin is only supported & tested on LATEST 2 Minor versions of Android Gradle Plugin.

| AGP   | BundleTool | bundle-tool-gradle-plugin |
|-------|------------|---------------------------|
| 7.0.x | 1.6.0      | 1.1.0                     |

## Git Commit Check

Check this [link](https://medium.com/walmartlabs/check-out-these-5-git-tips-before-your-next-commit-c1c7a5ae34d1) to make sure everyone will make a **meaningful** commit message.

So far we haven't added any hook tool, but follow a regex rule like below:

```
(chore|feat|docs|fix|refactor|style|test|hack|release)(:)( )(.{0,80})
```

## License

>
> Copyright 2016-2022 2BAB
>
>Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
>
>   http://www.apache.org/licenses/LICENSE-2.0
>
> Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.