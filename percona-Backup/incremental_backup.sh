# Cron com
# 45 */1 * * * /etc/mysql/incremental_backup_1hr.sh

mkdir /data/backups/inc1

echo "Starting incremental backup..."
thisincrementalbackup=`date +%Y-%m-%d-%H-%M-%Sincremental`

rm -rf /etc/mysqlbackup/inc1/*

echo "Starting incremental backup $thisincrementalbackup"
/usr/bin/xtrabackup --backup --target-dir=/etc/mysqlbackup/inc1 --incremental-basedir=/etc/mysqlbackup/current --datadir=/var/lib/mysql/

echo "Starting xtra prepare $thisincrementalbackup"
/usr/bin/xtrabackup --prepare --apply-log-only --target-dir=/etc/mysqlbackup/current --incremental-dir=/etc/mysqlbackup/inc1

tar -cvf /etc/mysqlbackup/incremental/$thisincrementalbackup /etc/mysqlbackup/current
#scp -r /etc/mysqlbackup/incremental/$thisincrementalbackup support@a.b.c.d:~/incremental/
rsync -va /etc/mysqlbackup/incremental/ support@a.b.c.d:~/incremental/

