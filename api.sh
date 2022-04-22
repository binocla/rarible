#!/bin/sh

set -o errexit
set -o nounset
set -o pipefail

export $(grep -v '^#' .env | xargs)

# uvicorn app.api:app --host 0.0.0.0 --port 6677
gunicorn app.api:app --workers $WORKERS --timeout $TIMEOUT --preload --worker-class uvicorn.workers.UvicornWorker --bind 0.0.0.0:6677
