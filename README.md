Credit Updater
==============
This is a single-purpose program I use for working with GCD (Grand Comics Database - http://www.comics.org) data dumps.
It will prepare an initial database dump by shrinking the database to only the data that is used by Infinite Longbox, an
Android app front-end for the modified GCD. It will also extract characters, character appearances, and creator credits
that are stored in text fields in the GCD. It can also extract characters and credits from an updated version of the
database for new or updated records and transfer any new or updated records to the primary database.

Before you start
----------------

* You will need to have a MySQL database server installed and running. You can download MySQL from
  https://dev.mysql.com/downloads/mysql/.
* You will need to have a GCD data dump. You can download the latest data dump from
  https://www.comics.org/download/.
* You will need to have a Java JDK installed. You can download the latest JDK from
  https://www.oracle.com/java/technologies/javase-downloads.html.
* You will need to have Gradle installed. You can download the latest Gradle from https://gradle.org/install/.
* You will need to have Git installed. You can download the latest Git from https://git-scm.com/downloads.

Usage
-----

* Use the data dump to create a MySQL database. The easiest way to do this is to use the command line:
  ```
  mysql -u root -p
  create database gcd;
  exit
  mysql -u root -p gcd < dump_file.sql
  ```
  where **dump_file.sql** is the name of the data dump file and **gcd** is the name you choose for your database schema.


* The **runjar** script will build the project and run the Credit Updater. To run the Credit Updater, follow these
  steps:

    * Make sure the **runjar** script is executable. If not, run the following command: **chmod +x runjar**
    * Run the **runjar** script: **./runjar**

  This will start the interactive mode of the Credit Updater. You will be prompted to choose between initializing or
  migrating the database. You can also configure other settings such as the database name, username, password, and
  starting story ID.

* You can also create a jar file using **./gradlew jar**, or **./gradlew shadowJar** to bundle the resource files into
  the jar file. The jar file will be created in the build/libs directory. You can then run the jar file using the
  following command:
  ```
  java -jar credit-updater.jar [options]
  ```
  Using the **-h** option will display the available options.
* If you don't provide command line options, Credit Updater will look for a file called **cu_config.json** in the root
  directory of the project. The file should contain the following
  JSON:
  ```json
  {
    "username": "username",
    "password": "password",
    "test_database": "test_database",
    "update_database": "update_database",
    "primary_database": "primary_database",
    "incoming_database": "incoming_database",
    "character_starting_story_id": 0,
    "credits_starting_story_id": 0
  }
  ```
  fill in the values for **username**, **password**, **test_database**, **update_database**, **primary_database**,
  and **incoming_database**. The **character_starting_story_id** and **credits_starting_story_id** values should
  normally
  be set to **0**, but if the program has been interrupted, they can be set to the last story ID that was processed.

License
-------
Credit Updater is open source and available under the Apache 2 License. See the LICENSE file for more info.

Contact
-------
If you have any questions or suggestions regarding the Credit Updater, please contact us at wesley@benica.dev.
That's it! In the unlikely event that you find this program useful, I'd like to hear from you! If you have any further
questions, feel free to reach out to me. Happy coding!

Please note that this README assumes that the project structure and dependencies are already set up correctly. If there
are any issues or missing information, please let me know, and I'll be happy to assist you further.