================================
Bond Usage Patterns
================================


.. _patern_nondet:

Dealing with Non-determinism in Observations
-------------------------------------------------

Bond shines when you need to assert the equality of some state values in your
test, with some expected values that are the same every time the particular
test runs. This is not enough when your test tries to observe values that are
inherently non-deterministic, e.g., random numbers, timestamps, user names,
full directory paths, process ids, etc.


.. _pattern_random:

Dealing with Random Numbers
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

You should seed the random number generator with a fixed value, when starting
each test. To be continued ...

Alternatively, I sometimes prefer to mock the entire random number generator
to make it return consecutive recognizable numbers. To be continued ...


.. _patern_time:
Dealing with Timestamps
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

You should mock time. To be continued ...


.. _pattern_bond_import:
Including Bond in Production Code
--------------------------------------------------

Safe Import of Bond
^^^^^^^^^^^^^^^^^^^^^^^^

If you put spy point annotations in your production code, you will have to either distribute
Bond with your code, which is safe as long as you do not call ``bond.start_test``,
or else fake the Bond API functions using something like this in your file:

.. container:: code-examples

    .. code-block:: python
        :emphasize-lines: 2, 4-

        # Import, or fake, bond
        try:
            from utils import bond
        except ImportError:
            class bond:
                @staticmethod
                def active()
                    return False
                @staticmethod
                def spy(*args, **kw):
                    return None
                @staticmethod
                def spy_point(**kw):
                    return lambda f: f

    .. code-block:: ruby
        :emphasize-lines: 1, 3-9

        begin
            require 'bond'
        rescue LoadError
            module BondTargetable
                DUMMY_BOND = Class.new { def method_missing(meth, *args); end }.new
                def self.extended(base); base.include(BondTargetable); end
                def bond; DUMMY_BOND; end
            end
        end


The code shown above defines all the API functions that may be used in your production code. Put this in
a module of your production code, and import ``bond`` from that module everywhere you need it in production files.

Inline Spy and Mock Points
^^^^^^^^^^^^^^^^^^^^^^^^^^^

The recommended way to use mocking with Bond in the production code is to use
the ``bond.spy_point`` annotation, which allows you to observe and mock at
function boundaries. Occasionally, you may want to inject a mock in the middle
of a function. This pattern gives you full flexibilty for how you weave
the mock injection point into your production code, while still delegating
the mock functionality to the agent, and thus to the testing code. 

.. container:: code-examples

    .. code-block:: python

        # Inline spying
        bond.spy(what=x, msg='Spying has effect only if you called bond.start_test')
        ...
        if not bond.active():
           value = compute_production_value ()
        else
           # This is true only if you called bond.start_test
           value = bond.spy('my_spy_point', what=x)
           if value == bond.AGENT_RESULT_NONE:
               assert False, "When testing, you must mock 'my_spy_point'"
               
               

    .. code-block:: ruby

        
    .. code-block:: ruby

         at some point do { |x| add code }
