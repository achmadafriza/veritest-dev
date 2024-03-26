FROM gradle:8.6.0-jdk21-graal AS GRADLE_CACHE

RUN mkdir -p /home/gradle/cache_home

ENV GRADLE_USER_HOME /home/gradle/cache_home
COPY build.gradle /home/gradle/java-code/
COPY settings.gradle /home/gradle/java-code/

WORKDIR /home/gradle/java-code
RUN gradle clean build -i --stacktrace

FROM gradle:8.6.0-jdk21-graal AS GRADLE_BUILD

COPY --from=GRADLE_CACHE /home/gradle/cache_home /home/gradle/.gradle
COPY . /usr/src/code
WORKDIR /usr/src/code

USER root
RUN gradle bootJar -i --stacktrace

FROM ubuntu:22.04 AS VERIOPT

WORKDIR /home
RUN apt update && \
    apt install -y wget unzip && \
    wget --no-check-certificate -O veriopt.zip https://github.com/uqcyber/veriopt-releases/archive/main.zip && \
    unzip veriopt.zip && \
    rm veriopt.zip && \
    sed -i 's/document\s*=\s*.*\s*,/document = false,/g' /home/veriopt-releases-main/ROOT && \
    apt remove -y wget unzip && \
    apt autoremove -y && \
    rm -rf /var/lib/apt/lists/* /var/cache/apt/archives/*

FROM makarius/isabelle:Isabelle2023 AS ISABELLE_BUILD

COPY --chown=isabelle:isabelle --from=VERIOPT /home/veriopt-releases-main /home/isabelle/source

WORKDIR /home/isabelle/source
RUN /home/isabelle/Isabelle/bin/isabelle build -v -o system_heaps -b HOL-Library && \
    /home/isabelle/Isabelle/bin/isabelle build -R -v -d . -o system_heaps -o show_question_marks=false -o document=false -o quick_and_dirty -b Canonicalizations && \
    /home/isabelle/Isabelle/bin/isabelle build -v -d . -o system_heaps -o show_question_marks=false -o document=false -o quick_and_dirty -b Canonicalizations

FROM ghcr.io/graalvm/graalvm-community:21 AS VERITEST

COPY --from=GRADLE_BUILD /usr/src/code/build/libs/*.jar /root/app/app.jar
COPY --from=GRADLE_BUILD /usr/src/code/cmd /root/cmd

#ENV JAVA_HOME=/opt/java/graalvm
#ENV PATH "${JAVA_HOME}/bin:${PATH}"
#COPY --from=GRADLE_BUILD /opt/java/graalvm $JAVA_HOME

COPY --from=ISABELLE_BUILD /home/isabelle/Isabelle /root/Isabelle

COPY --from=VERIOPT /home/veriopt-releases-main /root/source

WORKDIR /root/cmd

CMD ["./start.sh"]