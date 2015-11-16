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

   make docs           # makes all the docs
   make -C docs html   # makes the RST docs (subset of above)

To push it to GitHub Pages:

.. code::

   GITHUB_USER=your_user_name make github_pages

   
Dependencies
-----------------

.. container:: tab-section-group

    .. container:: tab-section-python

        You will need Python 2.7+ installed and available in your path. 

    .. container:: tab-section-ruby
 
        You will need Ruby 2.1+ installed and available in your path. ``rspec`` and ``gem`` need to be available as well. 

    .. container:: tab-section-java

        You will need Java 1.7+ installed and have ``JAVA_HOME`` set for gradle to use. 
        gradle will be downloaded automatically through the use of gradle wrapper - all 
        gradle commands should be run as ``./gradlew`` rather than ``gradle``. 


Running tests
-----------------

The best way to run tests is from PyCharm / RubyMine / IntelliJ / your favorite IDE. Just right-click in the body of a test, and you can select "Debug test..."

To run all the tests from the command line:

.. code::

   bond> [BOND_RECONCILE=...] make run_tests

To execute only tests for a single language, ``make`` using tasks ``run_tests_py``, ``run_tests_rb``, or ``run_tests_java``.

.. container:: tab-section-group

    .. container:: tab-section-python

        To execute only a subset of the Python tests:

        .. code::

            bond> PYTHON_TEST_ARGS=tests.bond_test[.BondTest[.test_result]] make run_tests_py

    .. container:: tab-section-ruby

        To execute only a subset of the Ruby tests:

        .. code::

            bond> cd rbond; rspec spec/test_file_spec.rb

    .. container:: tab-section-java

        To execute only a subset of the Java tests:

        .. code::
    
            bond> cd jbond; ./gradlew test -Dtest.single=YourClassTest


Deploying a new version
--------------------------

.. container:: tab-section-group

    .. container:: tab-section-python

        - Increment the version number (``pybond/setup.py`` and ``pybond/bond/__init__.py``)
        - Update docs/changelog.rst
        - Test the submission to PyPi

        - It is best to create an account on `https://testpypi.python.org/pypi`_
        - Create the ``~/.pypirc`` as described at https://wiki.python.org/moin/TestPyPI

        .. code::

           make pypi_test     # Create the package and test it
           make pypi_upload   # Upload to Test PyPi

    .. container:: tab-section-ruby

        .. include:: ../rbond/README.rst
            :start-after: rst_newVersionInstructionsStart
            :end-before: rst_newVersionInstructionsEnd

    .. container:: tab-section-java

        Coming soon!
         
         
