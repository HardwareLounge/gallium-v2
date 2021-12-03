#!/bin/bash
DATA_DIR=$(pwd)
docker build -t gallium:v2 .
docker run -d --restart=always --name gallium -v $DATA_DIR:/usr/local/lib/data gallium:v2
