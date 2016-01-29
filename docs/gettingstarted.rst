.. _gettingstarted:

===========================
Getting Started with Bond
===========================

Setting Up Dependencies
-----------------------

.. container:: tab-section-group

    .. container:: tab-section-python

        You will need Python 2.7+ installed and available in your path. 

    .. container:: tab-section-ruby
 
        You will need Ruby 2.1+ installed and available in your path, as well as ``gem``. 
        Simply run ``gem install bond-spy`` to install Bond.

    .. container:: tab-section-java

        You will need Java 1.7+ installed.

        You will also need to include Bond as a dependency in your project:

        - Gradle
            .. code-block:: groovy
     
                dependencies {
                    compile 'org.necula.bond:bond:0.1.0'
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


        If you are using Gradle, you will probably also want to set the observation directory 
        programmatically within your ``build.gradle`` (this example assumes your project has a standard 
        Maven project structure, but you can customize it to your needs). You'll learn more about this
        later, but basically it defines the root directory to store the output of your tests in for use 
        later. You want this near your tests:

        .. code-block:: groovy

            import java.nio.file.Paths

            test { // or any custom test task
                useJUnit() { // or useTestNG()
                    environment 'BOND_OBSERVATION_DIR', Paths.get(file("$projectDir").toString(),
                            'src', 'test', 'resources', 'test_observations').toAbsolutePath()
                }
            }

        This can be accomplished in Maven using the Surfire plugin (>2.4):

        .. code-block:: xml

            <plugin>
              <groupId>org.apache.maven.plugins</groupId>
              <artifactId>maven-surefire-plugin</artifactId>
              <version>2.4</version>
              <configuration>
                <environmentVariables>
                  <BOND_OBSERVATION_DIR>${basedir}/src/test/resources/test_observations</BOND_OBSERVATION_DIR>
                </environmentVariables>
              </configuration>
            </plugin>

After you've finished here, you should head over to the :ref:`tutorial` to learn how to use Bond!
