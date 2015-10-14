# coding: utf-8
lib = File.expand_path('../lib', __FILE__)
$LOAD_PATH.unshift(lib) unless $LOAD_PATH.include?(lib)

Gem::Specification.new do |spec|
  spec.name          = "bond"
  spec.version       = 0.1
  spec.authors       = ["George Necula", "Erik Krogen"]
  spec.email         = ["necula@cs.berkeley.edu", "erikkrogen@gmail.com"]

  spec.summary       = "A spy-based testing framework"
  spec.homepage      = "http://github.com/necula01/bond/rbond"

  spec.files         = `git ls-files -z`.split("\x0").reject { |f| f.match(%r{^(test|spec|features)/}) }
  spec.bindir        = "exe"
  spec.executables   = spec.files.grep(%r{^exe/}) { |f| File.basename(f) }
  spec.require_paths = ["lib"]

  spec.add_development_dependency "bundler", "~> 1.10"
  spec.add_development_dependency "rake", "~> 10.0"
  spec.add_development_dependency "rspec"
end
