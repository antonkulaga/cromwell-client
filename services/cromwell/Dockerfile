FROM broadinstitute/cromwell:develop

RUN apt update &&  apt -y upgrade
RUN apt -y install apt-transport-https ca-certificates curl software-properties-common gnupg2 git
RUN curl -fsSL https://download.docker.com/linux/debian/gpg | apt-key add -
RUN add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/debian $(lsb_release -cs) stable"
RUN apt update && apt -y install docker-ce

RUN git clone https://github.com/vishnubob/wait-for-it.git
RUN mkdir data
WORKDIR data

COPY ./app-config /app-config

ENTRYPOINT ["/bin/sh", "-c"]
