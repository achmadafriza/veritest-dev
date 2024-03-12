# the first stage of our build will use a gradle 6.9.0 parent image
FROM gradle:8.6.0-jdk21-graal AS GRADLE_BUILD

# copy the pom and src code to the container
COPY . /home
WORKDIR /home

USER root
RUN chown -R gradle /home && gradle clean assemble

# the second stage of our build will use open jdk 8 on alpine 3.9
FROM ubuntu:22.04 AS VERIOPT

WORKDIR /home
RUN apt update && \
    apt install -y wget unzip && \
    wget --no-check-certificate -O veriopt.zip https://github.com/uqcyber/veriopt-releases/archive/main.zip && \
    unzip veriopt.zip && \
    rm veriopt.zip && \
    apt remove -y wget unzip && \
    apt autoremove -y && \
    rm -rf /var/lib/apt/lists/* /var/cache/apt/archives/*

FROM makarius/isabelle:Isabelle2023 AS ISABELLE

# copy only the artifacts we need from the first stage and discard the rest
COPY --from=GRADLE_BUILD /home/build/libs/*.jar /home/isabelle/app/app.jar
COPY --from=GRADLE_BUILD /home/cmd /home/isabelle/cmd

ENV JAVA_HOME=/opt/java/graalvm
ENV PATH "${JAVA_HOME}/bin:${PATH}"
COPY --from=GRADLE_BUILD /opt/java/graalvm $JAVA_HOME

COPY --from=VERIOPT /home/veriopt-releases-main /home/isabelle/source

WORKDIR /home/isabelle/cmd

# set the startup command to execute the jar
CMD ["main.sh"]