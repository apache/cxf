FROM openjdk:8-jdk-alpine
VOLUME /tmp
ADD target/spring-boot-sample-rs-cxf.jar app.jar
ENV JAVA_OPTS=""
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app.jar" ]
