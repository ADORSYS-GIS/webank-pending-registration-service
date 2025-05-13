#!/bin/bash

# Default to localhost if no URL is provided
TARGET_URL=${TARGET_URL:-"http://localhost:8080"}
ENV=${ENV:-"local"}

echo "Running load tests against: $TARGET_URL"
echo "Environment: $ENV"

# Install dependencies if needed
if [ ! -d "node_modules" ]; then
    echo "Installing dependencies..."
    npm install
fi

# Run the tests
if [ "$ENV" == "dev" ]; then
    npm run test:dev
elif [ "$ENV" == "local" ]; then
    TARGET_URL=$TARGET_URL npm run test
else
    echo "Invalid environment. Use 'local' or 'dev'"
    exit 1
fi 