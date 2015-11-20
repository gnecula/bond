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

        You will also need to include Bond's dependencies in your project (as well as Bond, of course):

        - Gradle
            .. code-block:: groovy
                :emphasize-lines: 2
     
                dependencies {
                    compile: 'bond:bond:0.1'
                    test 'com.google.guava:guava:18.0'
                    test 'com.google.code.gson:gson:2.4'
                    test 'com.googlecode.java-diff-utils:diffutils:1.2.1'
                    test 'junit:junit:4.12'
                }

        - Maven
            .. code-block:: xml
                :emphasize-lines: 1-5

                <dependency>
                    <groupId>bond</groupId>
                    <artifactId>bond</artifactId>
                    <version>0.1</version>
                </dependency>                
                <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>18.0</version>
                </dependency>
                <dependency>
                    <groupId>com.google.code.gson</groupId>
                    <artifactId>gson</artifactId>
                    <version>2.4</version>
                </dependency>
                <dependency>
                    <groupId>com.googlecode.java-diff-utils</groupId>
                    <artifactId>diffutils</artifactId>
                    <version>1.2.1</version>
                </dependency>
                <dependency>
                    <groupId>junit</groupId>
                    <artifactId>junit</artifactId>
                    <version>4.12</version>
                </dependency>

        - Ivy
            .. code-block:: xml
                :emphasize-lines: 1

                <dependency org="bond" name="bond" rev="0.1" />
                <dependency org="com.google.guava" name="guava" rev="18.0" />
                <dependency org="com.google.code.gson" name="gson" rev="2.4" />
                <dependency org="com.googlecode.java-diff-utils" name="diffutils" rev="1.2.1" />
                <dependency org="junit" name="junit" rev="4.12" />

        If you are using Gradle, you will probably also want to set the observation directory 
        programmatically within your ``build.gradle`` (this example assumes your project has a standard 
        Maven project structure, but you can customize it to your needs). You'll learn more about this
        later, but basically it defines the root directory to store the output of your tests in for use 
        later. You want this near your tests:

        .. code-block:: groovy

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
