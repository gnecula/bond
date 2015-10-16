===========================
Bond API Documentation
===========================


Python
---------------

Python Spying API
^^^^^^^^^^^^^^^^^^^^^^^

For spying you need at the very minimum to call :py:func:`bond.start_test` (typically in your ``setUp`` code)
and then :py:func:`bond.spy` to actually spy values.

At the very minimum, you need to call ``start_test`` with a single argument referencing the current TestCase.


----

.. automodule:: bond
  :members: start_test

----

To actually collect observations (spy) you need to call :py:func:`bond.spy`, with a name of a spy point, and
your choice of values to spy. The values to spy are named with key parameters and are saved in a JSON format
to the observation file.

----

.. automodule:: bond
  :members: spy

----

One more API function that comes handy occasionally is :py:func:`bond.settings` that you can use in the body of
your test to override some Bond parameters that were set by :py:func:`bond.start_test`:

----

.. automodule:: bond
  :members: settings

----

You can place spy points in your production code as well, if you want to be able to spy on intermediate computations
in your code. Bond provides the :py:func:`bond.spy_point` function and method decorator that you can use to spy
the arguments and the result of a function or a method.

----

.. automodule:: bond
  :members: spy_point

----

Python Mocking API
^^^^^^^^^^^^^^^^^^^^^^^



.. automodule:: bond
  :members: deploy_agent


Ruby
------------------