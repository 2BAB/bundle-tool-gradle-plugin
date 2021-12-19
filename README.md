<img src="./banner.png" alt="bundle tool gradle plugin" width="771px">

[![Apache 2](https://img.shields.io/badge/License-Apache%202-brightgreen.svg)](https://www.apache.org/licenses/LICENSE-2.0)

A Gradle Plugin for Android BundleTool.

## Usage

**0x01. Add the plugin to classpath:**

``` kotlin
buildscript {
    repositories {
        ...
        mavenCentral()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:7.0.4'
        classpath 'me.2bab:bundle-tool-plugin:1.0.0'
    }
}
```

**0x02. Apply Plugin:**

``` kotlin
// For you application module
plugins {
    id("me.2bab.bundletool")
}
```

**0x03. Advanced Configurations**

``` kotlin
bundleTool {
    enableByVariant { variant -> variant.name.contains("debug", true) }

    buildApks {
        create("universal") {
            buildMode.set(ApkBuildMode.UNIVERSAL.name)
        }
        create("pixel4a") {
            deviceSpec.set(file("./pixel4a.json"))
        }
    }

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
./gradlew TransformApksFromBundleForStagingDebug
```


## Compatible

bundle-tool-gradle-plugin is only supported & tested on LATEST 2 Minor versions of Android Gradle Plugin.

| AGP   | BundleTool | bundle-tool-gradle-plugin |
|-------|------------|---------------------------|
| 7.0.x | 1.6.0      | 1.0.0                     |

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