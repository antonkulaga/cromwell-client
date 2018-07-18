FROM broadinstitute/cromwell:develop

RUN apt-get update && apt-get -y install apt-transport-https curl dirmngr
RUN echo 'deb https://apt.dockerproject.org/repo debian-stretch main' >> /etc/apt/sources.list
RUN apt-key adv --keyserver hkp://p80.pool.sks-keyservers.net:80 --recv-keys F76221572C52609D && apt-get update
RUN apt-get -y install docker-engine

RUN git clone https://github.com/vishnubob/wait-for-it.git
RUN mkdir cromwell-working-dir
WORKDIR cromwell-working-dir

COPY ./app-config /app-config

ENTRYPOINT ["/bin/sh", "-c"]
