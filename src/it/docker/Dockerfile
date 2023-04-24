FROM openjdk:8u342-slim-buster

RUN apt-get update
RUN apt-get -y upgrade libssl1.0 && \
    apt-get -y install wget ca-certificates bash
RUN wget -O /tmp/kairosdb.tar.gz \
    https://github.com/kairosdb/kairosdb/releases/download/v1.3.0/kairosdb-1.3.0-1.tar.gz

RUN mkdir -p /opt/ && \
    cd /opt/ && \
    tar -xvf /tmp/kairosdb*

#COPY kairosdb.properties /opt/kairosdb/conf/kairosdb.properties
#COPY logback.xml /opt/kairosdb/conf/logging/logback.xml

EXPOSE 8080
ENTRYPOINT [ "/opt/kairosdb/bin/kairosdb.sh"]
CMD [ "run" ]
