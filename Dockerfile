# Linux tests and checks for Fluxo

# https://github.com/CircleCI-Public/cimg-android
# https://hub.docker.com/r/cimg/android/tags
FROM cimg/android:2022.06-browsers
LABEL maintainer="Artyom Shendrik <artyom.shendrik@gmail.com>"
LABEL name=fluxo-test

RUN java -version && gradle -v && ruby -v && node -v

# Remove invalid toolchain (to prevent Gradle problems & warnings)
RUN sudo rm -rfv /usr/lib/jvm/openjdk-11

# Prepare project and preload dependencies + konan caches (Kotlin Native)
COPY --chown=circleci:circleci . .
RUN ./gradlew commonize --no-daemon --no-watch-fs --continue --stacktrace \
    && ./gradlew resolveDependencies --no-daemon --no-watch-fs --continue --stacktrace \
    && ./gradlew -i --stop

# Run in container
# ./gradlew build check --continue --stacktrace --scan
