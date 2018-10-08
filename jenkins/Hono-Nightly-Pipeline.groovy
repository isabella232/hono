#!/usr/bin/env groovy

/*******************************************************************************
 * Copyright (c) 2016, 2018 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

/**
 * Jenkins pipeline script for nightly build (every night between 2 and 3 AM) of Hono master.
 *
 */


node {
    def utils = evaluate readTrusted("jenkins/Hono-PipelineUtils.groovy")
    properties([buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '1', daysToKeepStr: '', numToKeepStr: '3')),
                pipelineTriggers([cron('TZ=Europe/Berlin \n # every night between 2 and 3 AM \n H 2 * * *')])])
    try {
        utils.checkOutHonoRepoWithCredentials("master", "bebef3c5-da22-425c-8554-f62d0b3a9608", "ssh://git@github.com/eclipse/hono.git")
        nightlyBuild()
        utils.aggregateJunitResults()
        utils.captureCodeCoverageReport()
        utils.publishJavaDoc()
        utils.archiveArtifacts("deploy/target/eclipse-hono-deploy-*.tar.gz,cli/target/hono-cli-*-exec.jar")
        currentBuild.result = 'SUCCESS'
    } catch (err) {
        currentBuild.result = 'FAILURE'
        echo "Error: ${err}"
    }
    finally {
        echo "Build status: ${currentBuild.result}"
        utils.notifyBuildStatus()
    }
}

/**
 * Nightly build with maven (with jdk1.8.0-latest and apache-maven-latest as configured in 'Global Tool Configuration' in Jenkins).
 *
 */
def nightlyBuild() {
    stage('Build') {
        withMaven(maven: 'apache-maven-latest', jdk: 'jdk1.8.0-latest', options: [jacocoPublisher(disabled: true), artifactsPublisher(disabled: true)]) {
            sh 'mvn clean package javadoc:aggregate'
            sh 'mvn --projects :hono-service-auth,:hono-service-messaging,:hono-service-device-registry,:hono-adapter-http-vertx,:hono-adapter-mqtt-vertx,:hono-adapter-kura,:hono-adapter-amqp-vertx,:hono-adapter-coap-vertx,:hono-example -am deploy -DcreateJavadoc=true -DenableEclipseJarSigner=true'
        }
    }
}
