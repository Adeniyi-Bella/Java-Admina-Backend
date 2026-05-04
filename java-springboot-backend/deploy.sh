#!/bin/bash

echo "Building JAR..."
./gradlew clean bootJar --no-daemon -x test

echo "Copying files to server..."
scp -i ~/.ssh/id_rsa build/libs/api-1.0.0-SNAPSHOT.jar root@167.235.54.229:~/admina/

echo "Building Docker image on server..."
ssh -i ~/.ssh/id_rsa root@167.235.54.229 "cd ~/admina && docker compose build --no-cache && docker image prune -f"

echo "Restarting app on server..."
ssh -i ~/.ssh/id_rsa root@167.235.54.229 "systemctl restart admina"

echo "Done! Checking health..."
sleep 30
curl https://api.admina-app.com/actuator/health