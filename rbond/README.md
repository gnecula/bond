# Bond

For more about Bond and usage instructions, see the main [documentation page](http://necula01.github.io/bond/).

## Installation

Add this line to your application's Gemfile:

```ruby
gem 'bond-spy'
```

And then execute:

    $ bundle

Or install it yourself as:

    $ gem install bond-spy
    
Please note that the minimum version of Ruby that Bond supports is 2.1.

## Development

After checking out the repo, run `bin/setup` to install dependencies. Then, run `rake spec` 
to run the tests. 

API documentation is built using YARD; simply running `yard` will generate the documentation. 
However, for the full suite of documentation including examples, you should build from the 
project main directory (one level up), which will generate and integrate YARD documentation. 

To install this gem onto your local machine, run `bundle exec rake install`. To release a 
new version, update the version number in `bond/version.rb` and the date in `bond.gemspec`, 
commit your changes, and tag the commit with `rbond-vX.X.X`. Please include '\[rbond\]' as the
first part of any commit message related to the Ruby version of Bond to differentiate it from
commits related to other parts of Bond. Push your commit up (including tags), build the gem 
using `gem build bond.gemspec`, and run `gem push bond-spy-X.X.X.gem` to release the .gem 
file to [rubygems.org](https://rubygems.org). For example:

    $ git commit -am "[rbond] Release version 1.0.2"
    $ git tag rbond-v1.0.2
    $ git push origin --tags
    $ gem build bond.gemspec
    $ gem push bond-spy-1.0.2.gem

## Contributing

Bug reports and pull requests are welcome on GitHub at https://github.com/necula01/bond.

