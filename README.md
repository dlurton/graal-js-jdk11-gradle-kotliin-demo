

# Running GraalJS on stock JDK11 with Kotlin and Gradle

This repository is just like [graalvm/graal-js-jdk11-maven-demo](https://github.com/graalvm/graal-js-jdk11-maven-demo) but in Kotlin and Gradle 4.10.

It was intended to teach me a little about what it takes to get Graal/Truffle up and running under stock OpenJDK 11.

I'm throwing this up in GitHub for future reference.  Primary learning: given how difficult this was to set up, it is better just to use GraalVM.

# Building 

```
export JAVA_HOME=<path to jdk 11>
./gradle build
```

Make sure your gradle build is successful before attempting to build in an IDE.

Add this to your VM options in the run configuration of your IDE to get the tests to run in the IDE.

-`-ea -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI --module-path=build/compiler --upgrade-module-path=build/compiler/compiler-1.0.0-rc12.jar`

