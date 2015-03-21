#
# MariaDB Dockerfile
#
# https://github.com/dockerfile/mariadb
#

# Pull base image.
FROM ubuntu:14.04

# Install MariaDB.
RUN \
  apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 0xcbcb082a1bb943db && \
  echo "deb http://mariadb.mirror.iweb.com/repo/10.0/ubuntu `lsb_release -cs` main" > /etc/apt/sources.list.d/mariadb.list && \
  apt-get update && \
  DEBIAN_FRONTEND=noninteractive apt-get install -y mariadb-server && \
  rm -rf /var/lib/apt/lists/* && \
  sed -i 's/^\(bind-address\s.*\)/# \1/' /etc/mysql/my.cnf && \
  echo "mysqld_safe &" > /tmp/config && \
  echo "mysqladmin --silent --wait=30 ping || exit 1" >> /tmp/config && \
  echo "mysql -e 'GRANT ALL PRIVILEGES ON *.* TO \"root\"@\"%\" WITH GRANT OPTION;'" >> /tmp/config && \
  bash /tmp/config && \
  rm -f /tmp/config

# Install Percona Xtrabackup
RUN \
  apt-key adv --keyserver keys.gnupg.net --recv-keys 1C4CBDCDCD2EFD2A && \
  echo "deb http://repo.percona.com/apt trusty main" >> /etc/apt/sources.list.d/percona.list && \
  echo "deb-src http://repo.percona.com/apt trusty main" >> /etc/apt/sources.list.d/percona.list && \
  mkdir -p /etc/mysqlbackup && \
  apt-get update && apt-get install -y percona-xtrabackup

# Install qpress
RUN \
	apt-get install -y wget && \
	wget http://www.quicklz.com/qpress-11-linux-x64.tar && \
	tar -xf qpress-11-linux-x64.tar -C /usr/local/bin

# Install aws cli
RUN \
	apt-get install -y python && \
	apt-get install -y unzip && \
	apt-get install -y groff && \
	mkdir -p /mnt/software && \
	cd /mnt/software && \
	wget https://s3.amazonaws.com/aws-cli/awscli-bundle.zip && \
	unzip awscli-bundle.zip && \
	./awscli-bundle/install -i /usr/local/aws -b /usr/local/bin/aws


#Create directory for scripts
RUN \
	mkdir -p /mnt/scripts

#Set AWS CLI credentials
RUN \
	echo 'export AWS_ACCESS_KEY_ID=<YOUR KEY>' >> /root/.bashrc && \
	echo 'export AWS_SECRET_ACCESS_KEY=<YOUR KEY>' >> /root/.bashrc

# Define mountable directories.
VOLUME ["/etc/mysql", "/var/lib/mysql", "/etc/mysqlbackup"]

# Define working directory.
WORKDIR /data

# Define default command.
#CMD ["mysqld_safe"]

# Expose ports.
EXPOSE 3306

# Define default command
ENTRYPOINT ["/bin/bash", "/mnt/scripts/run.sh"]
