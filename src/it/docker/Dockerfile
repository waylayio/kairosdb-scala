FROM java:8-alpine

RUN apk upgrade libssl1.0 --update-cache && \
    apk add wget ca-certificates bash
RUN wget -O /tmp/kairosdb.tar.gz \
#    https://github.com/kairosdb/kairosdb/releases/download/v1.1.3/kairosdb-1.1.3-1.tar.gz
    https://github.com/kairosdb/kairosdb/releases/download/v1.2.0/kairosdb-1.2.0-1.tar.gz

RUN mkdir -p /opt/ && \
    cd /opt/ && \
    tar -xvf /tmp/kairosdb*

#COPY kairosdb.properties /opt/kairosdb/conf/kairosdb.properties
#COPY logback.xml /opt/kairosdb/conf/logging/logback.xml

EXPOSE 8080
ENTRYPOINT [ "/opt/kairosdb/bin/kairosdb.sh"]
CMD [ "run" ]
