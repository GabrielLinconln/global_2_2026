FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/classes target/classes
COPY lib lib
COPY sql sql
COPY web web
ENV PORT=8080 TZ=America/Sao_Paulo
EXPOSE 8080
CMD ["sh", "-c", "java -Duser.timezone=America/Sao_Paulo -Dporta=${PORT} -Ddb.url='jdbc:h2:/tmp/plantao;MODE=Oracle' -cp 'target/classes:lib/*' br.com.fiap.plantao.App"]
