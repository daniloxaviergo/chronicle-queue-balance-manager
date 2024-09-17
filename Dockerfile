FROM maven:3.6.3-jdk-8

WORKDIR /app

COPY . .

RUN mvn clean package

CMD ["mvn", "install", "exec:java@TCPServer"]
