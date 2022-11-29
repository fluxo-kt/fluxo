# Linux tests and checks for Fluxo

FROM cimg/android:2022.09-browsers
LABEL maintainer="Artyom Shendrik <artyom.shendrik@gmail.com>"
LABEL name=fluxo-test

RUN java -version && gradle -v && ruby -v && node -v

# Remove invalid toolchain (to prevent Gradle problems & warnings)
RUN sudo rm -rfv /usr/lib/jvm/openjdk-11

# Prepare project and preload dependencies + konan caches (Kotlin Native)
COPY --chown=circleci:circleci . .
RUN ./gradlew resolveDependencies commonize -i --no-daemon --no-watch-fs --continue --stacktrace --console=plain --scan \
    && ./gradlew -i --stop

# Run in container
# ./gradlew build check --continue --stacktrace --scan
