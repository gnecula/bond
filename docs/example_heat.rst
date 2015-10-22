.. _examples:

=======================================
An Example of Bond Usage
=======================================

We discuss here how you could use Bond to test a hypothetical example of a
program that monitors the changes of temperature over time. The code will
read the temperature and the current time, will compute the rate of change,
and depending on the rate of change will transition between several states:

- "Critical" (temperature is raising faster than 2 degrees a minute; next
  temperature reading in 10 seconds);
- "Warning" (temperature is raising slower than 2 degrees a minute, but more
  than 1 degree a minute; next temperature reading in 10 seconds);
- "Ok" (temperature is decreasing or constant, or is increasing slower than 1
  degree a minute; next temperature reading in 60 seconds)

The program sends alerts when there are state changes, and every 10 minutes
when it is in the Warning or Critical state.

This logic is hard to test without mocks, because:

- Tests would be slow if they were to take a sample every 10 seconds, or elss
- We may not have access to the sensor for reading the temperature;
  furthermore, it is impractical to simulate complex temperature changing
  scenarios using real sensors.
- We do not want to send actual alerts when testing


We are showing here how to use Bond to observe (spy) what the code is doing,
and to use mocks to create interesting scenarios. In real life, in addition to
these tests you will probably want to run a system test where you test the
system also with the actual sensors and alerters, but the corner cases would
be tested with mocks. Also in real life, you would probably refactor this code
so that it is even more easily testable.


How to use this code
------------------------------------

You can copy and paste this code into your computer, or you can find it in the
``tutorials/heat_watcher`` directory of the
`Bond sources <http://github.com/necula01/bond>`_. 

You can run the tests, and you can observe in your debugger how the different
functions are called.

.. container:: code-examples

    .. container:: code-language-python

        .. code-block:: bash

           python heat_watchers_test.py


    .. container:: code-language-ruby

        .. code-block:: bash

           rspec heat_watcher_spec.rb           
                        

Finally, to fully experience the power of the Bond observations, make a change
to the logic to compute the alerts and re-run the tests. You will see how the
observations change and you will have a chance to reconcile visually the old
and the new observations.  

The code to be tested
-----------------------------

.. container:: code-examples

   .. literalinclude:: ../pybond/tutorials/heat_watcher/heat_watcher.py
      :language: python

   .. literalinclude:: ../rbond/tutorials/heat_watcher/heat_watcher.rb
      :language: ruby


The tests using Bond
-----------------------------

.. container:: code-examples

   .. literalinclude:: ../pybond/tutorials/heat_watcher/heat_watcher_test.py
      :language: python

   .. literalinclude:: ../rbond/tutorials/heat_watcher/heat_watcher_spec.rb
      :language: ruby
