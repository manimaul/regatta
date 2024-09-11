FROM debian:stable
RUN apt update && \
    apt upgrade -y && \
    apt install -y openjdk-17-jre-headless

COPY server/build/install/server /opt/regatta

ENV JAVA_OPTS="-Djdbcurl=jdbc:postgresql://localhost:5432/regatta -Duser=regatta_admin -Dpassword=mysecretpassword"

CMD ["/opt/regatta/bin/server"]
