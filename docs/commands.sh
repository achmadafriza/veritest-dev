docker run --rm -it -p 8080:8080 -v ./:/app ubuntu:22.04 /bin/bash

docker run --rm -it --entrypoint "/bin/bash" makarius/isabelle:Isabelle2023

-v ~/.isabelle/Isabelle2023/servers.db:/home/isabelle/.isabelle/servers.db

docker run --rm -it \
  -v /mnt/c/Programming/Thesis/veriopt-dev/isabelle:/home/isabelle/source \
  -p 8082:8081 makarius/isabelle:Isabelle2023 server -p 8081

docker run --rm -it \
  -v /mnt/c/Programming/Thesis/veriopt-dev/isabelle:/home/isabelle/source \
  -v ./tmp:/tmp \
  -v ~/.isabelle/Isabelle2023:/home/isabelle/.isabelle \
  -p 8081:8081 makarius/isabelle:Isabelle2023 client -p 8081

/bin/bash -c apt-get -y update && apt-get install -y curl less libfontconfig1 libgomp1 openssh-client perl pwgen rlwrap && apt-get clean

/bin/bash -c tar xzf Isabelle.tar.gz && \
  mv Isabelle2023 Isabelle && \
  perl -pi -e 's,ISABELLE_HOME_USER=.*,ISABELLE_HOME_USER="\$USER_HOME/.isabelle",g;' Isabelle/etc/settings && \
  perl -pi -e 's,ISABELLE_LOGIC=.*,ISABELLE_LOGIC=HOL,g;' Isabelle/etc/settings \
  && Isabelle/bin/isabelle build -o system_heaps -b HOL \
  && rm Isabelle.tar.gz

apt update
apt install wget lualatex curl less libfontconfig1 libgomp1 openssh-client pwgen rlwrap
wget https://isabelle.in.tum.de/dist/Isabelle2023_linux.tar.gz

docker build . -t veritest
docker run --rm -it -p 8080:8080 veritest

docker tag veritest achmadafriza/veritest:0.1.3
docker push achmadafriza/veritest:0.1.3

docker tag veritest achmadafriza/veritest:latest
docker push achmadafriza/veritest:latest