FROM hirokimatsumoto/alpine-openjdk-11
COPY ./build/libs/IBL-bank-file-present-validation-1.0-SNAPSHOT.jar /app/IBL-bank-file-present-validation.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app/IBL-bank-file-present-validation.jar"]