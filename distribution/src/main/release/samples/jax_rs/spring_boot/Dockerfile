FROM openjdk:8-jdk-alpine
VOLUME /tmp
ADD target/spring-boot-sample-rs.jar spring-boot-sample-rs.jar
ENV JAVA_OPTS=""
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /spring-boot-sample-rs.jar" ]
