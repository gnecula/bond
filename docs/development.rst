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
   brew install gradle

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

The best way to run tests is from PyCharm / RubyMine / IntelliJ / your favorite IDE. Just right-click in the body of a test, and you can select "Debug test..."

To run all the tests from the command line:

.. code::

   bond> [BOND_RECONCILE=...] make run_tests

To execute only tests for a single language, ``make`` using tasks ``run_tests_py``, ``run_tests_rb``, or ``run_tests_java``.. 

To execute only a subset of the Python tests:

.. code::

   bond> PYTHON_TEST_ARGS=tests.bond_test[.BondTest[.test_result]]

    
Deploying a new version
--------------------------

For Python

   - Increment the version number (``pybond/setup.py`` and ``pybond/bond/__init__.py``)
   - Update docs/changelog.rst
   - Test the submission to PyPi

      - It is best to create an account on `https://testpypi.python.org/pypi`_
      - Create the ``~/.pypirc`` as described at https://wiki.python.org/moin/TestPyPI

        .. code::

           make pypi_test     # Create the package and test it
           make pypi_upload   # Upload to Test PyPi


         
         
