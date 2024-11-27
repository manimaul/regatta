#!/usr/bin/env bash


function k8s_ghcr() {
  kubectl create namespace regatta
  kubectl delete secret --namespace regatta ghreg
  kubectl create secret --namespace regatta docker-registry ghreg \
    --docker-server=ghcr.io --docker-username=$GH_USER --docker-password=$GH_TOKEN --docker-email=$GH_EMAIL
}

function print_token() {
  echo "user = $GH_USER"
  echo "token = $GH_TOKEN"
  echo "email = $GH_EMAIL"
}

function docker_login() {
  echo "$GH_TOKEN" | docker login ghcr.io -u manimaul --password-stdin
}

help() {
   echo "Login to the GitHub Container Registry"
   echo
   echo "arguments:"
   echo "print_token  Print the token"
   echo "docker_login Login to docker"
   echo "k8s_ghcr     Add k8s container pull credential secret to the regatta namespace"
   echo "help         Print this Help."
   echo
}

eval "$1"

