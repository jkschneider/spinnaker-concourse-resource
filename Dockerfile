FROM openjdk:8-jre-alpine

COPY ./build/install/spinnaker-concourse-resource /resource
COPY ./assets/check.sh /opt/resource/check
COPY ./assets/in.sh /opt/resource/in
COPY ./assets/out.sh /opt/resource/out

RUN chmod 755 /opt/resource/*
