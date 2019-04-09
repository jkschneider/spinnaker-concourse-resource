#!/usr/bin/env sh

./gradlew distInstall
docker build --tag=spinnaker-concourse-resource .