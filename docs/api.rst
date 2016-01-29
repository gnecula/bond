===========================
Bond API Documentation
===========================


Python
---------------

Python Spying API
^^^^^^^^^^^^^^^^^^^^^^^

.. _api_start_test:

For spying you need at the very minimum to call :py:func:`bond.start_test` (typically in your ``setUp`` code)
and then :py:func:`bond.spy` to actually spy values.

----

.. automodule:: bond
  :members: start_test

----

.. _api_spy:

To actually collect observations (spy) you need to call :py:func:`bond.spy`, with a name of a spy point, and
your choice of values to spy. The values to spy are named with key parameters and are saved in a JSON format
to the observation file.

----

.. automodule:: bond
  :members: spy

----

One more API function that comes handy occasionally is :py:func:`bond.settings` that you can use in the body of
your test to override some Bond parameters that were set by :py:func:`bond.start_test`. It takes
similar arguments as :py:func:`bond.start_test`.

----

.. automodule:: bond
  :members: settings

----

.. _api_spy_point:

You can place spy points in your production code as well, if you want to be able to spy on intermediate computations
in your code. Bond provides the :py:func:`bond.spy_point` function and method decorator that you can use to spy
the arguments and the result of a function or a method.

----

.. automodule:: bond
  :members: spy_point

----

If you want to modify your production code to fine tune your mocking, you may need to
know when Bond is active. You can use the function :py:func:`bond.active` for this purpose.


----

.. automodule:: bond
  :members: active

----

Python Mocking API
^^^^^^^^^^^^^^^^^^^^^^^

.. _api_deploy_agent:

Once you have deployed spy point annotations in your production code it is possible to use the same points for
mocking the result of those functions by deploying agents for specific spy points. An agent can:

* further decide on which invocations of the spy point they activate, based on various filters on the function arguments.
* spy the values of the arguments, and optionally the result also.
* control which arguments are spied and how are the observations formatted.
* execute additional test code on each call.
* bypass the actual body of the method and return a result prepared by the testing code, or throw an exception when the call is reached.

.. automodule:: bond
  :members: deploy_agent


Ruby
------------------

The Ruby API follows closely the Python API described above. The Ruby
API is implemented using the following classes:

- `Bond class <rbond/Bond.html>`_
- `BondTargetable class <rbond/BondTargetable.html>`_


Java
------------------

Due to the strongly typed nature of Java, the `Java API <jbond/index.html>`_ 
has significant differences from the Python API described above. You'll want
to start with:

- `Bond class <jbond/bond/Bond.html>`_
- `Observation class <jbond/bond/Observation.html>`_
- `BondTestRule JUnit Rule <jbond/bond/BondTestRule.html>`_
- `SpyAgent class <jbond/bond/SpyAgent.html>`_
- `SpyPoint annotation <jbond/bond/spypoint/SpyPoint.html>`_
- `BondMockPolcy (setup for mocking) <jbond/bond/spypoint/BondMockPolicy.html>`_
 
