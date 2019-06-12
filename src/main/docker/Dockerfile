FROM java:8
VOLUME /tmp
MAINTAINER ruili
ADD *.jar app.jar
EXPOSE 9099

ENTRYPOINT ["java","-Xmx2048m","-Duser.timezone=GMT+08","-jar","/app.jar"]
