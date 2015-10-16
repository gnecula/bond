
PYTHON_TEST_ARGS ?= tests

BOND_MERGE ?= console

run_tests:
	BOND_MERGE=$(BOND_MERGE) python -m unittest discover -p '*_tests.py'

TMP_GP=/tmp/bond_github

# Invoke make ... GITHUB_USER=you
GITHUB_USER?=necula01
GITHUB_REPO=https://$(GITHUB_USER)@github.com/necula01/bond.git

# Push documentation to GitHub pages
# Create a new REPO in $(TMP_GP) and push from there
github_pages:
	$(MAKE) -C docs clean html
	if test -d $(TMP_GP) ;then rm -rf $(TMP_GP); fi
	mkdir $(TMP_GP)
	cd $(TMP_GP) && git init && git remote add upstream $(GITHUB_REPO)
	rsync -arv docs/_build/html/* $(TMP_GP)
	echo "Disable Jekyll to allow _static" >$(TMP_GP)/.nojekyll
	cd $(TMP_GP) && git add * .nojekyll && git commit -am"Bond documentation"
	cd $(TMP_GP) && git push -f upstream HEAD:gh-pages



