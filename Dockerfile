FROM debian:stable
RUN apt update && \
    apt upgrade -y && \
    apt install -y openjdk-17-jre-headless

COPY build/install /opt

ENV JAVA_OPTS="-Djdbcurl=jdbc:postgresql://postgres:5432/regatta -Duser=admin -Dpassword=mysecretpassword"

CMD ["/opt/regatta/bin/regatta"]
