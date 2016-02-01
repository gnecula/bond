.. _buses_tutorial:

=====================================
Practice Testing with Spies and Mocks
=====================================

In this tutorial you will write tests with mocking for a simple application that we provide.
This will teach you a little about spying and mocking in general, and you should also get 
comfortable with using Bond in a real project. 
The application finds out what are the closest buses to a particular stop on a bus route, e.g.,
the 51B route (Alameda County Transit Authority). The app first prompts the user to select
the stop among the list of stops on the route.

.. image:: _static/bus_stop.jpg
    :height: 200px       
           
The app works by finding out the geographic coordinates of the desired bus stop, and the live locations
of the buses on that route. Using the geographic coordinates the app will estimate the (straight line)
distance from each bus to the bus stop and will compute a list of running buses sorted on the increasing
distance to the bus stop.

Disclaimer: There are certainly approximations in this app, perhaps even bugs.

Acknowledgements
-------------------------------------------

The idea for this particular application came from Jonathan Indig and Davis Shepherd.

The data for bus stops and running buses is obtained from `NextBus <http://nextbus.cubic.com/About/Vision>`_. 
For details on the API please see https://www.nextbus.com/xmlFeedDocs/NextBusXMLFeed.pdf 
(although you do not really need to look at that document to complete this tutorial).


Important Request
------------------------------------------

The project is provided as is to anybody who wants to learn mocking. However, we ask
that you do not post the solutions in any location that is searchable. Please do not push these solutions
to a public GitHub repository. We would like everybody who wants to do this project in the future
to have the same pristine experience that you are having now.
   
Task 1: Get Started with the Provided Files
-------------------------------------------

To help you start we have provided some boilerplate files that you will have to
modify to implement the project.

- This tutorial has been developed on Mac OS X and Linux. You can probably use Windows with 
  an installation of ``python`` / ``ruby`` / ``mvn`` or ``gradle`` (for Java), but we have not tested this.
  
- Download the support files

  * Clone the Bond repository (https://github.com/necula01/bond)
  * The ``buses_tutorial`` directory is what we will be interested in for this tutorial

- This tutorial can be done in Python, Ruby, or Java. Once you have cloned the repository
  you should see the following directory structure within ``buses_tutorial``:

  .. list-table::
                  
     * - ``py``
       - Files for the Python version of the tutorial
     * - ``py/buses.py``
       - The buses app 
     * - ``py/buses_tests.py``
       - The tests for buses app 
     * - ``rb``
       - Files for the Ruby version of the tutorial
     * - ``rb/buses.rb``
       - The buses app 
     * - ``rb/buses_spec.py``
       - The RSpec tests for buses app 
     * - ``java``
       - Files for the Java version of the tutorial
     * - ``java/pom.xml``
       - Maven pom file defining the Java project and its dependencies
     * - ``java/build.gradle``
       - Gradle build file defining the Java project and its dependencies
     * - ``java/src/main/java/bond/example/Buses.java``
       - Source file of the Buses app
     * - ``java/src/test/java/bond/example/BusesTest.java``
       - Tests for the Buses app

- Note that for Java we have included both a pom.xml file for Maven and a build.gradle 
  file for Gradle; either build system will work and you don't need both.
        
- For Python and Ruby you will also need to install Bond (in Java this is taken care of 
  automatically by Maven/Gradle). This can be done via ``pip install bond`` (Python) or
  ``gem install bond-spy`` (Ruby). 

- (Optional, but recommended) You should install the tool ``kdiff3`` if you
  have not installed it already. It is a generally good tool for any kind of
  merging, including using for ``git mergetool``. Make sure to add the
  installation directory to the PATH so that you can invoke the tool from the
  terminal.
  
  - Visit the `Kdiff3 web site <http://kdiff3.sourceforge.net/>`_
  - For Mac, this involves downloading and installing the DMG file, and then
    adding to your PATH environment variable ``/Applications/kdiff3.app/Contents/MacOS/kdiff3``.
    If running a Debian flavor of Linux, you should be able to run ``sudo apt-get install kdiff3``.

    .. code::

       kdiff3    #  should open the kdiff3 window

  If you do install ``kdiff3`` you can use it in place of the default
  ``console`` by using ``BOND_RECONCILE=kdiff3`` instead of
  ``BOND_RECONCILE=console`` in the command line (see below).
  

Task 2: Run the buses application
----------------------------------

Try out the application to undestand what it does, and to make sure it works properly:
(these commands should be executed within the directory for the language you will be working in)

.. container:: tab-section-group

   .. container:: tab-section-Python

      .. code-block:: bash

         python buses.py

   .. container:: tab-section-Ruby

      .. code-block:: bash
         
         ruby buses.rb
       
   .. container:: tab-section-Java
          
      .. code-block:: bash
        
         mvn compile exec:java
         # OR
         gradle --console plain run

You should see a list of stops on the 51B bus route through Berkeley, and a
prompt to pick one of the stops. If you enter something that is not a number
in the required range, the interactive loop will repeat. Once you pick the
stop, you should see a list of a few buses that are currently running that
route sorted in increasing order of the distance from the bus stop you picked.

Note: if you try to do this part at night, when the 51B bus is not running,
you may not get any running buses. You can change the code to use the route
851 (an all nighter) in that case.

Take a look at the code for the app, it should be easy to follow.

In the rest of the tutorial you will have to write the tests for this app. 
Ideally, the person who wrote the app would have used test-driven development 
and the app would have tests already - but that would leave nothing for you to do!

Task 3: Run the initial tests
------------------------------

We provided a skeleton test framework for you to use.

.. container:: tab-section-group

   .. container:: tab-section-Python

      The tests are in the file ``buses_tests.py`` and are using the Python ``unittest`` test framework. 

      .. code::

        python buses_tests.py

      You should see one test passing: ``test_distance_computation``.

   .. container:: tab-section-Ruby

      The tests are in the file ``buses_spec.rb`` and are using the Ruby ``rspec`` test framework.
      You may need to install this gem using ``gem install rspec``. 

      .. code::
        
        rspec buses_spec.rb

      You should see one test passing: ``it 'should properly compute distance between points'``

   .. container:: tab-section-Java

      The tests are in the file ``BusesTest.java`` and are using the Java JUnit test framework.

      .. code-block:: bash
        
        mvn test
        # OR
        gradle test
  
      You should see one test passing: ``testDistanceComputation``.  

Task 4: Read the documentation for the Bond spying and mocking library
-------------------------------------------------------------------------

This tutorial assumes you are already familiar with the rest of the Bond documentation;
if you have not already done so, you should read it now:

* `Read the Bond tutorial <http://necula01.github.io/bond/tutorial.html>`_
* `Read the Bond example <http://necula01.github.io/bond/example_heat.html>`_
* `Skim the API documentation  <http://necula01.github.io/bond/api.html>`_ You
  will have to refer back to the API documentation as you are working on this project.
  

Task 5: Use spying for a simple unit test
-----------------------------------------------------------------------

#. Use ``bond.spy`` to replace the assertions in the unit test ``test_distance_computation`` / 
   ``should properly compute distance between points`` / ``testDistanceComputation``. 
   You can use a single call to spy in place of the 3 calls to ``assertEquals`` / ``expect``.

#. .. container:: tab-section-group

      .. container:: tab-section-Python

         You must initialize the bond library by calling ``bond.start_test`` in your test. 

      .. container:: tab-section-Ruby

         - You must install RSpec: ``gem install rspec``
         - You must ``include_context :bond`` in your RSpec ``describe``
           block. We have prepared this for you, all you have is to uncomment the
           line. 

      .. container:: tab-section-Java  

         For Java: You must include the ``BondTestRule`` JUnit ``@Rule`` in your test class
         to enable Bond, and there are a number of other requirements to enable mocking
         (see the `API docs for BondMockPolicy <jbond/bond/spypoint/BondMockPolicy.html>`_ for more detail).
         We have prepared all of this for you, all you have to do is uncomment the ``BondTestRule``
         lines at the top of the class. 
  
#. Now run the tests, this time setting the environment variable
   ``BOND_RECONCILE`` to the value ``console`` (or ``kdiff3`` if you installed it).
   You can use any method you know for setting the environment
   variable. In our examples, we will just set the environment variable
   at the begining of the shell command for running the tests:

   .. container:: tab-section-group

      .. container:: tab-section-Python

         .. code::
    
            BOND_RECONCILE=console python buses_tests.py


      .. container:: tab-section-Ruby

         .. code::
    
            BOND_RECONCILE=console rspec buses_spec.rb

      .. container:: tab-section-Java

         .. code-block:: bash
 
            BOND_RECONCILE=console mvn test
            # OR
            BOND_RECONCILE=console gradle test
 
   For the rest of this assignment this is how you should run the tests. 

#. You will be prompted to accept the new spy observations. Verify that they are
   correct (same values as what you had in ``assertEquals`` / ``expect``), and then 
   accept the changes. Hint: In ``kdiff3`` you can accept change individually by 
   clicking on the red up/down arrows to position to the next difference, and click 
   B to accept the new observations (shown in panel B in the UI). You can accept all 
   changes by going to the ``Merge`` menu.

#. You will now see a subdirectory called ``test_observations``.

Task 6: Write the first mock
--------------------------------------

Next you will write a test for the ``select_stop_interactive`` function. This
function takes a list of bus-stop records with fields like ``lat``, ``lon``, and
``title``, prints a menu of stops, indexed from 1 to however many there are,
prompts the user to enter a valid stop index, and then returns the record
corresponding to that index.

#. Add to the testing module a new test named ``test_select_stop_0`` (Python) / 
   ``it 'should select the first bus when stop 1 is specified'`` (Ruby) /
   ``testSelectStop0`` (Java). In this
   test you will call the ``select_stop_interactive`` method. You should write code
   in the test to construct a list of two bus-stop records and pass it to the function.

#. Add to your test a call to Bond to spy the value returned from the ``select_stop_interactive`` function
   (instead of adding asserts that the call returns a record with the right fields)

#. If you try to run the test at this point, it is going to wait for
   user input. This is not acceptable in automated tests.
   You will need to intercept this operation and
   mock the response to "1" (to select the first stop).

   * The app code has already been refactored to allow
     you to insert a spy point for mocking with Bond the reading from the
     console.
   * The spy point should spy its result (we want to save in the observations
     the returned value).
   * Also, we want to tell the spy point to abort the execution if there is no
     mock result, since we never want to actually wait for user input in
     automated tests.
   * The test code should deploy an agent to provide a mock result for
     reading from the console.  In Bond, an agent must specify the precise spy point
     name. See the `Bond API documentation <http://necula01.github.io/bond/api.html>`_ for
     how spy point names are constructed. You can also see the spy point name
     in the observation dictionary if you run the test.

#. Run the tests, verify and accept the initial observations for the new test.

Task 7: Write a more complex mock for reading input
-----------------------------------------------------------------------

The ``select_stop_interactive`` function contains logic to reject invalid
input and re-prompt the user. Now you will write a test to exercise that
logic.

**Please note that this task is a bit trickier that others. The subsequent tasks
do not depend on it.** 

#. Add to the testing module a new test named ``test_select_stop_retries`` (Python) /
   ``it 'should should retry when an invalid bus selection is given'`` (Ruby) /
   ``testSelectStopRetries`` (Java). In this test you will call the ``select_stop_interactive``, 
   using the same input data as for the previous test.

#. Add to your test a call to Bond to spy the value returned from the function
   (instead of adding asserts that it returns a record with the right fields).

#. You need to deploy a slightly more complex agent for the spy point that you
   used also for Task 6.

   - The first time the user is prompted for input, your mock should return
     the string "a". This will force the logic to retry.
   - On second call, the mock should return "" (empty string). This will
     also force the logic to retry.
   - On third call, the mock should return "3". This will
     also force the logic to retry, because the index is too large. 
   - On third call, the mock should return "0". This will
     also force the logic to retry, because the index is too small. 
   - On fourth call, the mock should return "2". This should be a valid input
     and the code should return the second stop record.
   - Hints:

     - Writing the logic for this mock can be a bit tricky, but the shortest
       solution is just 2 lines.
     - Your agent should use a function as a ``result`` parameter (in Java, 
       use a ``Resulter`` as the argument to the ``withResult()`` method on ``SpyAgent``).
     - The function should be written to return "a" the first time it is
       called, "" the second time, etc. 
   - Note that this test should find a bug in the app code. Fix it, to make
     the test pass.  

#. Run the tests, verify and accept the initial observations for the new test

Task 8: Writing mocks for the HTTP requests
---------------------------------------------------------------------

Now we are going to test the main function, ``get_closest_buses``. There are
no new concepts for this part, it is going to be similar mocking as for the
previous two tasks.

#. Add to the testing module a new test named ``test_integration`` (Python) / 
   ``it 'should integrate correctly'`` (Ruby) / ``testIntegration`` (Java). In this test you will 
   call the ``get_closest_buses``, with no arguments, and you should spy the result.

#. You need to add a spy point to intercept the two HTTP requests. Your spy point
   should:

   - observe the parameters of the request
   - not observe the result of the request (it is big)
   - declare that it needs a mock result, or else the
     execution should fail. We don't want actual HTTP requests during testing. 

#. In your test you will need to deploy two agents, one for each kind of
   request

   - Your agents will have to provide mock results for the two HTTP requests.
   - To get good samples, run the buses app directly, and you will see that it
     prints the URLs that it is accessing. Use a command-line tool (e.g.,
     curl) to make the same request and get the output. (Be careful to quote
     the URL when you use it on the command line, because it contains
     characters such as '&' which are special in the shell.)
   - The ``routeConfig`` output is big, you only need the first 3 stops or so
     for your integration test. But when you extract that fragment be careful
     to preserve the XML file structure.
   - When you copy the XML output in your test, be careful to use multi-line
     strings, e.g., ``"""..."""`` in Python or ``%q(...)`` in Ruby. Java doesn't
     have anything like this, so you'll need to use a series of String concatenations,
     but most IDEs will do this automatically for you when you paste in string blocks. 
   
#. Run the tests, verify and accept the initial observations for the new test.

Task 9: Some code maintenance
------------------------------------------------------------------

You think you are done, but your product manager has just woken up with this
nightmare that he was made to promise at gun-point that the buses application
will be changed to provide the answers in kilometers instead of miles.

.. image:: _static/nightmare.jpeg
       :height: 200px
       :align: right
               
So, he
askes you to change to kilometers. Oh, no! The code change is small but all
these precise tests will have to be changed. No sweat, you call in 007.

#. Change the function ``compute_lat_long_distance`` to replace the variable
   ``earth_radius`` from 3961 (miles) to 6373 (kilometers). That is all you
   need to change in the code.

#. Re-run all your tests, and let Bond prompt you to accept the new
   observations. Inspect them then accept the new reference values. 
                  
