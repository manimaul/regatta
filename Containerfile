# Runtime-only image: the JVM build runs on the host (`./gradlew :regattaServer:makeImg`), and
# `installJvmDist`'s lib/ is copied in. Distroless has no shell, so the Gradle start script is not
# used and java is exec'd directly. Pass JVM options (the -Djdbcurl/-Duser/-Dpassword the server
# reads) through JDK_JAVA_OPTIONS, which the java launcher itself expands; JAVA_OPTS is only
# understood by the start script.
FROM gcr.io/distroless/java21-debian13

COPY server/build/install/regatta-jvm/lib /opt/regatta/lib

ENV JDK_JAVA_OPTIONS="-Djdbcurl=jdbc:postgresql://localhost:5432/regatta -Duser=regatta_admin -Dpassword=mysecretpassword"

EXPOSE 8888

ENTRYPOINT ["/usr/bin/java", "-cp", "/opt/regatta/lib/*", "com.mxmariner.regatta.ApplicationKt"]
