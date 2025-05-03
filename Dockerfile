FROM openjdk:21
EXPOSE 7355
ADD target/AssetManagement.jar AssetManagement.jar
ENTRYPOINT ["java","-jar","/AssetManagement.jar"]

