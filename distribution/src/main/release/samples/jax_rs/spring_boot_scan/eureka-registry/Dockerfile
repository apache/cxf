FROM openjdk:8-jdk-alpine
VOLUME /tmp
ADD target/spring-boot-sample-rs-scan-eureka.jar spring-boot-sample-rs-scan-eureka.jar
EXPOSE 8761
ENV JAVA_OPTS=""
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /spring-boot-sample-rs-scan-eureka.jar" ]
