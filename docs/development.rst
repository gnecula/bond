===========================
Bond Development
===========================


Generating documentation
---------------------------

You must install some tools:

.. code::
   
   pip install -U Sphinx
   gem install yard

Then you can process the documentation

.. code::

    make docs

To push it to GitHub Pages:

.. code::

   make github_pages
   

Running tests
-----------------

The best way to run tests is from PyCharm. Just right-click in the body of a test, and you can select "Debug test..."

To run all the tests from the command line:

.. code::

   bond> [BOND_RECONCILE=...] make run_tests

   
 
    
