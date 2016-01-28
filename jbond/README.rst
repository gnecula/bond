Bond
.......................

For more about Bond and usage instructions, see the main `documentation page <http://necula01.github.io/bond/>`_.

Installation
-----------------------

Include Bond as a dependency in your project:

    - Gradle
        .. code-block:: groovy
     
            dependencies {
                compile: 'org.necula.bond:bond:0.1.0'
            }

    - Maven
        .. code-block:: xml

            <dependency>
                <groupId>org.necula.bond</groupId>
                <artifactId>bond</artifactId>
                <version>0.1.0</version>
            </dependency>                

    - Ivy
        .. code-block:: xml

            <dependency org="org.necula.bond" name="bond" rev="0.1.0" />
    
Please note that the minimum version of Java that Bond supports is 7.

Development
-----------------------

After checking out the repo, run ``./gradlew test`` to run the tests. 

API documentation is built using Javadocs; run ``./gradlew javadoc`` to generate the documentation.
However, for the full suite of documentation including examples, you should build from the 
project main directory (one level up), which will generate and integrate Javadoc documentation.
To do this, simply run ``make docs`` from the main directory. 

.. rst_newVersionInstructionsStart



To release a new version, update the version number in ``build.gradle``,
commit your changes, and tag the commit with ``jbond-vX.X.X``. Please include '[jbond]' as the
first part of any commit message related to the Java version of Bond to differentiate it from
commits related to other parts of Bond. Push your commit up (including tags), and upload the
new jar file to the Maven central repository using ``./gradlew uploadArchives``. You will need
to have an account with `OSSRH <http://central.sonatype.org/pages/ossrh-guide.html>`_ and have
the credentials for this account, as well as PGP signature information, available in your
``gradle.properties`` file (should be located at ``~/.gradle/gradle.properties``) as such
(see the `GnuPG documentation <https://www.gnupg.org/documentation/howtos.html>`_ 
for information on generating a PGP key using GnuPG):

.. code::

    signing.keyId=8-digit_hexadecimal_key_id
    signing.password=password_to_your_pgp_key
    signing.secretKeyRingFile=/home/user/.gnupg/secring.gpg

    ossrhUsername=yourUsername
    ossrhPassword=yourPassword

This will put the artifact on OSSRH for staging; you can then promote the release manually
on the `OSSRH <https://oss.sonatype.org/>`_ or use the command ``./gradlew closeAndPromoteRepository``
to automate the process through Gradle. 

.. code:: bash

    $ git commit -am "[jbond] Release version 1.0.2"
    $ git tag jbond-v1.0.2
    $ git push origin --tags
    $ git push origin
    $ ./gradlew test
    $ ./gradlew uploadArchives
    $ ./gradlew closeAndPromoteRepository

.. rst_newVersionInstructionsEnd

Contributing
-----------------------

Bug reports and pull requests are welcome on `GitHub <https://github.com/necula01/bond>`_.

