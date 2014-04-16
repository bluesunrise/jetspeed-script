jetspeed-script
===============

Jetspeed API Scripting Tool
============================

Build Instructions:
-------------------

1. download Oracle database driver: ojdbc6-11.2.0.4.jar

2. install into local Maven repo:

    ```sh
    > mvn install:install-file -Dfile=/home/rwatler/installs/oracle/ojdbc6-11.2.0.4.jar -DgroupId=com.oracle -DartifactId=ojdbc6 -Dversion=11.2.0.4 -Dpackaging=jar
    ```

3. run standard Maven build, (requires JDK 1.6+):

    ```sh
    > mvn clean install
    ```

Running Instructions:
---------------------

1. run executable jar, (optionally takes a Jetspeed spring filter key argument, defaults to 'portal.dbPageManager'):

    ```sh
    > java -jar target/jetspeed-script-1.0.jar
    ```

2. configure jetspeed-script.properties template, (supports MySQL, Oracle, and PostgreSQL):

    ```
    org.apache.jetspeed.database.driver=com.mysql.jdbc.Driver
    org.apache.jetspeed.database.url=jdbc:mysql://localhost:3306/j2test?characterEncoding=utf-8&amp;useUnicode=true
    org.apache.jetspeed.database.user=j2
    org.apache.jetspeed.database.password=j2
    ```

3. restart executable jar:

    ```sh
    > java -jar target/jetspeed-script-1.0.jar
    ```

4. enter Javascript commands at 'jetspeed>' prompt:

    ```sh
    jetspeed> println(RoleManagerImpl.roleExists('user'))
    true
    ```

    ```sh
    jetspeed> for (role in Iterator(RoleManager.getRoleNames(""))) println(role)
    admin
    dev
    devmgr
    ...
    ```

    ```sh
    jetspeed> for (user in Iterator(UserManager.getUsers(""))) println(user.name)
    admin
    devmgr
    guest
    ...
    ```

5. utilities are available to facilitate Java List and Map usage:

    ```sh
    jetspeed> javaToJS(UserManager.getUsers('')).forEach(function(user){println(user.name)})
    guest
    ```

    ```sh
    jetspeed> javaToJS(UserManager.getUser('guest').getInfoMap())['user.name.family']
    Guest
    ```

6. a Jetspeed security user/subject can be set using a global property: 'jetspeedUserName'. This can
also be set using the -jetspeedUserName argument:

    ```sh
    > java -jar target/jetspeed-script-1.0.jar -jetspeedUserName=guest
    jetspeed> jetspeedUserName
    guest
    jetspeed> jetspeedUserName='admin'
    admin
    ```

7. Example of adding roles to a user

     ```sh
    jetspeed> RoleManager.addRoleToUser('tomcat', 'dev')
    jetspeed> javaToJS(RoleManager.getRolesForUser('tomcat')).forEach(function(role){println(role.name)})
    dev
    jetspeed> RoleManager.addRoleToUser('tomcat', 'devmgr')
    jetspeed> javaToJS(RoleManager.getRolesForUser('tomcat')).forEach(function(role){println(role.name)})
    dev
    devMgr
    ```

8. bash-like command history available using up/down arrows and javascript symbol completion available using the Tab key.

9. run scripts without entering interactive mode:

    ```sh
    > java -jar target/jetspeed-script-1.0.jar < roles.js
    ```

10. enter 'exit' or 'quit' at 'jetspeed>' prompt to exit:

    ```sh
    jetspeed> exit
    ```

Project To Do:
--------------

1. implement configuration option for PSML file path.
