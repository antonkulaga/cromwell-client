FROM broadinstitute/cromwell:dev

ENV NB_USER=cromwell

RUN apt update &&  apt upgrade -y
RUN apt install -y apt-transport-https ca-certificates curl software-properties-common gnupg2 git apt-utils
RUN curl -fsSL https://download.docker.com/linux/debian/gpg | apt-key add -
RUN apt install docker-compose -y
#RUN add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/debian $(lsb_release -cs) stable"

RUN git clone https://github.com/vishnubob/wait-for-it.git
RUN mkdir data
WORKDIR data

#COPY ./app-config /app-config

#22-29

RUN useradd -r -u 1004 $NB_USER
RUN usermod -aG docker $NB_USER
ENV EUID=1004
EXPOSE 8000

ENTRYPOINT ["/bin/sh", "-c"]
