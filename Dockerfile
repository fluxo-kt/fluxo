# Linux tests and checks for Fluxo

FROM cimg/android:2022.09-browsers
LABEL maintainer="Artyom Shendrik <artyom.shendrik@gmail.com>"
LABEL name=fluxo-test

RUN java -version && gradle -v && ruby -v && node -v

# Remove invalid toolchain (to prevent Gradle problems & warnings)
RUN sudo rm -rfv /usr/lib/jvm/openjdk-11

COPY --chown=circleci:circleci . .
RUN ./gradlew -v

# Run in container
# ./gradlew build check --continue --stacktrace --scan
