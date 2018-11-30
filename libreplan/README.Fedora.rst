Configuring LibrePlan in Fedora for first time
==============================================

This section explains how to set up LibrePlan in Fedora after installing the
package.

It assumes that PostgreSQL is already running and configured to use ``md5`` as
the authentication method.

If this is not the case, go to section "Configuring PostgreSQL to use ``md5``
authentication method", at the end of this document.


1) Create libreplan database
----------------------------

# psql -h localhost -U postgres < /usr/share/libreplan/pgsql/create_db.sql


2) Create libreplan user
------------------------

# psql -h localhost -U postgres < /usr/share/libreplan/pgsql/create_user_postgresql.sql


3) Install LibrePlan database
-----------------------------

# psql -h localhost -U libreplan -W libreplan < /usr/share/libreplan/pgsql/install.sql


4) Link LibrePlan configuration file in Tomcat6 configuration directory
-----------------------------------------------------------------------

# ln -s /usr/share/libreplan/conf/libreplan.xml /etc/tomcat6/Catalina/localhost/


5) Link Java JDBC driver for PostgreSQL in Tomcat6 libraries directory
----------------------------------------------------------------------

# ln -s /usr/share/java/postgresql-jdbc3.jar /usr/share/tomcat6/lib/


6) Link LibrePlan WAR file in Tomcat6 we applications directory
---------------------------------------------------------------

# ln -s /usr/share/libreplan/webapps/libreplan.war /var/lib/tomcat6/webapps/


7) Restart Tomcat6
------------------

# service tomcat6 restart


LibrePlan should be running at http://localhost:8080/libreplan


Review INSTALL file for more information.


Upgrading LibrePlan a.b.c to LibrePlan x.y.z
============================================

This section explains how to upgrade LibrePlan from version a.b.c to version x.y.z.


1) Run upgrade scripts
----------------------

# psql -h localhost -U libreplan -W libreplan < /usr/share/libreplan/pgsql/upgrade_x.y.z.sql

*VERY IMPORTANT*: If there are other versions between a.b.c and x.y.z, we need to execute those scripts in order, so the upgrade is done correctly.

*WARNING*: If you are using PostgreSQL version 8 you will have to execute the next command over LibrePlan database in order to use the upgrade script for version 1.3.0:

# su postgres -c "createlang -d libreplan plpgsql"

2) Stop Tomcat6
---------------

# service tomcat6 stop


3) Remove current deployed aplication
-------------------------------------

# rm -rf /var/lib/tomcat6/webapps/libreplan/


4) Start Tomcat6
----------------

# service tomcat6 start


LibrePlan should be running at http://localhost:8080/libreplan



Configuring PostgreSQL to use ``md5`` authentication method
===========================================================

We assume that PostgreSQL is using ``md5`` as the authentication method, instead of default ``ident``.

These are the steps to change it.


1) Add a password to 'postgres' user (for instance, let's use 'postgres' as password')
--------------------------------------------------------------------------------------

# su postgres -c psql

postgres=# ALTER USER postgres WITH PASSWORD 'postgres';
postgres=# \q


2) Edit '/var/lib/pgsql/data/pg_hba.conf' and replace ``ident`` by ``md5``
--------------------------------------------------------------------------

# sed -i "/^host/s/ident/md5/g" /var/lib/pgsql/data/pg_hba.conf


3) Restart PostgreSQL
---------------------

# service postgresql restart
