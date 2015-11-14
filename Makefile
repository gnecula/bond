
PYTHON_TEST_ARGS ?= tests

BOND_RECONCILE ?= console

##
## Running tests
##
run_tests: run_tests_py run_tests_rb run_tests_java

run_tests_py:
	$(MAKE) -C pybond run_tests BOND_RECONCILE=$(BOND_RECONCILE) 

run_tests_rb:
	cd rbond && BOND_RECONCILE=$(BOND_RECONCILE) rspec

run_tests_java:
	cd jbond && BOND_RECONCILE=$(BOND_RECONCILE) ./gradlew test

##
## Documentation
##
.PHONY: docs run_tests_py run_tests_rb run_tests_java run_tests
docs:
	$(MAKE) -C docs clean html
	cd rbond && yardoc
	mkdir -p docs/_build/html/rbond
	rsync -ar rbond/doc/* docs/_build/html/rbond
	cd jbond && ./gradlew javadoc
	mkdir -p docs/_build/html/jbond
	rsync -ar jbond/build/docs/javadoc/* docs/_build/html/jbond

##
## Publishing documentation
##
TMP_GP=/tmp/bond_github

# Invoke make ... GITHUB_USER=you
GITHUB_USER?=necula01
GITHUB_REPO=https://$(GITHUB_USER)@github.com/necula01/bond.git

# Push documentation to GitHub pages
# Create a new REPO in $(TMP_GP) and push from there
github_pages: docs
	if test -d $(TMP_GP) ;then rm -rf $(TMP_GP); fi
	mkdir $(TMP_GP)
	cd $(TMP_GP) && git init && git remote add upstream $(GITHUB_REPO)
	rsync -arv docs/_build/html/* $(TMP_GP)
	echo "Disable Jekyll to allow _static" >$(TMP_GP)/.nojekyll
	cd $(TMP_GP) && git add * .nojekyll && git commit -am"Updated Bond documentation"
	cd $(TMP_GP) && git push -f upstream HEAD:gh-pages



