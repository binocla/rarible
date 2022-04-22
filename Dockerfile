FROM python:3.8-slim

USER root

RUN apt-get update && \
    apt-get install -y apt-transport-https build-essential cmake curl gcc g++ git python3-numpy tree sudo unzip wget htop && \
    python -m venv venv && \
    /usr/local/bin/python -m pip install --upgrade pip && \
    rm -rf /var/lib/apt/lists/* && \
    apt-get purge   --auto-remove && \
    apt-get clean

COPY ./requirements.txt /tmp/requirements.txt
RUN pip install --ignore-installed --no-cache-dir -r /tmp/requirements.txt

COPY . /app
WORKDIR /app

ENTRYPOINT [ "bash", "api.sh" ]
