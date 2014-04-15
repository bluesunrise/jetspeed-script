jetspeed-script
===============

Jetspeed API Scripting Tool
============================

Build Instructions
------------------
1. download Oracle database driver: ojdbc6-11.2.0.4.jar

2. install into local Maven repo:


    > mvn install:install-file -Dfile=/home/rwatler/installs/oracle/ojdbc6-11.2.0.4.jar -DgroupId=com.oracle -DartifactId=ojdbc6 -Dversion=11.2.0.4 -Dpackaging=jar


3. run standard Maven build:


    > mvn clean install


Running Instructions:
---------------------

1. run executable jar:


    > java -jar target/jetspeed-script-1.0.jar


2. configure jetspeed-script.properties template:


    org.apache.jetspeed.database.driver=com.mysql.jdbc.Driver
    org.apache.jetspeed.database.url=jdbc:mysql://localhost:3306/j2test?characterEncoding=utf-8&amp;useUnicode=true
    org.apache.jetspeed.database.user=j2
    org.apache.jetspeed.database.password=j2


3. restart executable jar:


    > java -jar target/jetspeed-script-1.0.jar


4. enter Javascript commands at 'jetspeed>' prompt:


    jetspeed> println(RoleManagerImpl.roleExists('user'))
    true


    jetspeed> for (role in Iterator(RoleManagerImpl.getRoleNames(""))) println(role)
    admin
    dev
    devmgr
    ...

    jetspeed> for (user in Iterator(UserManagerImpl.getUsers(""))) println(user.name)
    admin
    devmgr
    guest
    ...


5. bash-like command history available using up/down arrows and javascript symbol completion available using the Tab key.


6. run scripts without entering interactive mode 


    > java -jar target/jetspeed-script-1.0.jar < roles.js


7. enter 'exit' or 'quit' at 'jetspeed>' prompt to exit:


    jetspeed> exit
