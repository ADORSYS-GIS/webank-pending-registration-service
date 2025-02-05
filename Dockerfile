# Run stage
FROM openjdk:17-jdk-slim

WORKDIR /webank-prs

# py the JAR file from the webank prs  module
COPY ./prs/prs-rest-server/target/prs-rest-server-0.0.1-SNAPSHOT.jar /webank-prs/prs-rest-server-0.0.1-SNAPSHOT.jar
# Expose the port your app runs on

ENV TWILIO_ACCOUNT_SID=${TWILIO_ACCOUNT_SID}
ENV TWILIO_AUTH_TOKEN=${TWILIO_AUTH_TOKEN}
ENV TWILIO_PHONE_NUMBER=${TWILIO_PHONE_NUMBER}
ENV OTP_SALT=${OTP_SALT}
ENV SERVER_PRIVATE_KEY_JSON=${SERVER_PRIVATE_KEY_JSON}
ENV SERVER_PUBLIC_KEY_JSON=${SERVER_PUBLIC_KEY_JSON}
ENV JWT_ISSUER=${JWT_ISSUER}
ENV JWT_EXPIRATION_TIME_MS=${JWT_EXPIRATION_TIME_MS}

EXPOSE 8080

# Run the application
CMD ["java", "-jar", "/webank-prs/prs-rest-server-0.0.1-SNAPSHOT.jar"]