FROM ubuntu:14.04

# SSH installation and generation & configuration of RSA public/private key
RUN \
	apt-get install -y openssh-client && \
	mkdir /root/.ssh && \
	ssh-keygen -t rsa -N '' -f /root/.ssh/id_rsa && \
	cat /root/.ssh/id_rsa.pub >> /root/.ssh/authorized_keys && \
	chmod 700 /root/.ssh && \
	chmod 600 /root/.ssh/authorized_keys


# Install Percona Server
RUN \
	apt-key adv --keyserver keys.gnupg.net --recv-keys 1C4CBDCDCD2EFD2A && \
	echo "deb http://repo.percona.com/apt `lsb_release -cs` main" > /etc/apt/sources.list.d/percona.list && \
	apt-get update && \
	apt-get install -y percona-server-server-5.6 && \
	rm -rf /var/lib/apt/lists/* && \
	sed -i 's/^\(bind-address\s.*\)/# \1/' /etc/mysql/my.cnf && \
	sed -i 's/^\(log_error\s.*\)/# \1/' /etc/mysql/my.cnf && \
	echo "mysqld_safe &" > /tmp/config && \
	echo "mysqladmin --silent --wait=30 ping || exit 1" >> /tmp/config && \
	echo "mysql -e 'GRANT ALL PRIVILEGES ON *.* TO \"root\"@\"%\" WITH GRANT OPTION;'" >> /tmp/config && \
	bash /tmp/config && \
	rm -f /tmp/config && \
	mkdir -p /etc/mysqlbackup && \
	apt-get update && apt-get install -y percona-xtrabackup


# Adding the script files for data backup
COPY daily_backup_full.sh /etc/mysql/daily_backup_full.sh
COPY incremental_backup.sh /etc/mysql/incremental_backup.sh
COPY run.sh /bin/run.sh

# Setting exec permissions and making cron job entries
RUN \
        mkdir -p /etc/mysqlbackup/current && \
        mkdir -p /etc/mysqlbackup/inc1 && \
        mkdir -p /etc/mysqlbackup/daily && \
        mkdir -p /etc/mysqlbackup/incremental && \
        chmod 777 /etc/mysql/daily_backup_full.sh && \
        chmod 777 /etc/mysql/incremental_backup.sh && \
        chmod 777 /bin/run.sh && \
        ( crontab -l ; echo "59 23 * * * /etc/mysql/daily_backup_full.sh" ) | crontab - && \
        ( crontab -l ; echo "45 */1 * * * /etc/mysql/incremental_backup.sh" ) | crontab -


# Defining FTP location and its user
ENV FTP_DEST_IP 149.210.207.78
ENV FTP_DEST_USER root

# Define mountable directories
VOLUME ["/etc/mysql", "/var/lib/mysql", "/etc/mysqlbackup"]

# Define working directory
WORKDIR /data

# Expose ports
EXPOSE 3306

# Define default command
CMD ["/bin/run.sh"]

