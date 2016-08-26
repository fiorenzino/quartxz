FROM java:8u92-jre-alpine

ENV VERTICLE_FILE quartxz-1.0-SNAPSHOT-fat.jar

# Set the location of the verticles
ENV VERTICLE_HOME /usr/verticles

EXPOSE 8080

# Copy your fat jar to the container
COPY target/$VERTICLE_FILE $VERTICLE_HOME/


# Launch the verticle
WORKDIR $VERTICLE_HOME

#COPY docker/logging.properties $VERTICLE_HOME/
#ENV VERTX_JUL_CONFIG $VERTICLE_HOME/logging.properties


ENTRYPOINT ["sh", "-c"]
CMD ["java -jar $VERTICLE_FILE"]