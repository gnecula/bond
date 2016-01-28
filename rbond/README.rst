Bond
.......................

For more about Bond and usage instructions, see the main `documentation page <http://necula01.github.io/bond/>`_.

Installation
-----------------------

Add this line to your application's Gemfile:

.. code::

    gem 'bond-spy'

And then execute:

.. code:: bash

    $ bundle

Or install it yourself as:

.. code:: bash

    $ gem install bond-spy
    
Please note that the minimum version of Ruby that Bond supports is 2.1.

Development
-----------------------

After checking out the repo, run ``bin/setup`` to install dependencies. Then, run ``rake spec`` 
to run the tests. 

API documentation is built using YARD; simply running ``yard`` will generate the documentation. 
However, for the full suite of documentation including examples, you should build from the 
project main directory (one level up), which will generate and integrate YARD documentation.
To do this, simply run ``make docs`` from the main directory. 

.. rst_newVersionInstructionsStart

To install this gem onto your local machine, run ``bundle exec rake install``. To release a 
new version, update the version number in ``bond/version.rb`` and the date in ``bond.gemspec``, 
commit your changes, and tag the commit with ``rbond-vX.X.X``. Please include '[rbond]' as the
first part of any commit message related to the Ruby version of Bond to differentiate it from
commits related to other parts of Bond. Push your commit up (including tags), build the gem 
using ``gem build bond.gemspec``, and run ``gem push bond-spy-X.X.X.gem`` to release the .gem 
file to `rubygems.org <https://rubygems.org>`_. For example:

.. code:: bash

    $ git commit -am "[rbond] Release version 1.0.2"
    $ git tag rbond-v1.0.2
    $ git push origin --tags
    $ git push origin
    $ gem build bond.gemspec
    $ gem push bond-spy-1.0.2.gem

.. rst_newVersionInstructionsEnd

Contributing
-----------------------

Bug reports and pull requests are welcome on `GitHub <https://github.com/necula01/bond>`_.

