<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**  *generated with [DocToc](https://github.com/thlorenz/doctoc)*

- [Helidon Example: quickstart-se](#helidon-example-quickstart-se)
  - [Prerequisites](#prerequisites)
  - [Build](#build)
  - [Start the application](#start-the-application)
  - [Exercise the application](#exercise-the-application)
  - [Build the Docker Image](#build-the-docker-image)
  - [Start the application with Docker](#start-the-application-with-docker)
  - [Deploy the application to Kubernetes](#deploy-the-application-to-kubernetes)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->


# Helidon Example: quickstart-se

This example implements a simple Hello World REST service.

## Prerequisites

1. Maven 3.5 or newer
2. Java SE 8 or newer
3. Docker 17 or newer to build and run docker images
4. Kubernetes minikube v0.24 or newer to deploy to Kubernetes (or access to a K8s 1.7.4 or newer cluster)
5. Kubectl 1.7.4 or newer to deploy to Kubernetes

Verify prerequisites
```
java --version
mvn --version
docker --version
minikube version
kubectl version --short
```

## Build

```
mvn package
```

## Start the application

```
java -jar target/quickstart-se.jar
```

## Exercise the application

```
curl -X GET http://localhost:8080/greet
{"message":"Hello World!"}

curl -X GET http://localhost:8080/greet/Joe
{"message":"Hello Joe!"}

curl -X PUT http://localhost:8080/greet/greeting/Hola
{"gretting":"Hola"}

curl -X GET http://localhost:8080/greet/Jose
{"message":"Hola Jose!"}
```

## Build the Docker Image

```
docker build -t quickstart-se target
```

## Start the application with Docker

```
docker run --rm -p 8080:8080 quickstart-se:latest
```

Exercise the application as described above

## Deploy the application to Kubernetes

```
kubectl cluster-info                # Verify which cluster
kubectl get pods                    # Verify connectivity to cluster
kubectl create -f target/app.yaml   # Deply application
kubectl get service quickstart-se  # Get service info
```
