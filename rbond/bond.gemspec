require_relative 'lib/bond/version'

Gem::Specification.new do |spec|
  spec.name          = 'bond-spy'
  spec.version       = Bond::VERSION
  spec.date          = '2016-01-28'

  spec.authors       = ['George Necula', 'Erik Krogen']
  spec.email         = ['necula@cs.berkeley.edu', 'erikkrogen@gmail.com']
  spec.license       = 'BSD-2-Clause-FreeBSD'

  spec.summary       = 'A spy-based testing framework'
  spec.homepage      = 'http://github.com/necula01/bond'
  spec.description   = <<-EOF
    Bond is a small library that can be used to spy values and mock functions
    during tests. Spying is a replacement for writing the assertEquals in your
    test, which are tedious to write and even more tedious to update when your
    test setup or code inevitably changes. With Bond, you separate what is
    being verified, e.g., the variable named output, from what value it should
    have. This way you can quickly spy several variables, even have structured
    values such as lists or dictionaries, and these values are saved into an
    observation log that is saved for future reference. If the test observations
    are different you have the option to interact with a console or visual tool
    to see what has changed, and whether the reference set of observations need
    to be updated.
  EOF

  spec.files         = `git ls-files -z`.split("\x0").reject { |f| f.match(%r{^spec/}) }
  spec.test_files    = `git ls-files -z -- spec`.split("\x0")
  spec.bindir        = 'bin'

  spec.required_ruby_version = '>= 2.1'
  spec.add_runtime_dependency 'neatjson', '~> 0.6'
  spec.add_development_dependency 'bundler', '~> 1.10'
  spec.add_development_dependency 'rake', '~> 10.0'
  spec.add_development_dependency 'rspec', '~> 3.0'
end
