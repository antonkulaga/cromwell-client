#!/usr/bin/env bash
docker stack deploy -c services/pipelines.yml cromwell
