===========================
Bond Development
===========================


Generating documentation
---------------------------

You must install Sphinx and dependencies:

.. code::
   
   pip install -U Sphinx
   brew install graphviz
   pip install sphinxcontrib-plantuml
   gem install yard

Then you can process the documentation

.. code::

   pip install -u Sphinx
   pip install sphinxcontrib-plantuml

Then
   
.. code::

   make docs           # makes all the docs
   make -C docs html   # makes the RST docs (subset of above)

To push it to GitHub Pages:

.. code::

   make github_pages
   

Running tests
-----------------

The best way to run tests is from PyCharm. Just right-click in the body of a test, and you can select "Debug test..."

To run all the tests from the command line:

.. code::

   bond> [BOND_RECONCILE=...] make run_tests

To execute only tests for Python or Ruby, ``make`` using tasks ``run_tests_py`` or ``run_tests_rb``. 
 
    
