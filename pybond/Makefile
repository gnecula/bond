UNAME_S := $(shell uname -s)
ifeq ($(UNAME_S),Darwin)
    SED_OPTS=-i \"\"  # Syntax on OSX is a little different from Linux
else
    SED_OPTS=-i
endif


##
## Running tests
##
BOND_RECONCILE ?= console

# You can specify PYTHON_TEST_ARGS=tests.bond_test.BondTest.test_result
PYTHON_TEST_ARGS ?= discover -s tests -p '*_test.py'
run_tests:
	BOND_RECONCILE=$(BOND_RECONCILE) python -m unittest $(PYTHON_TEST_ARGS)

##
## Deploying to PyPi

VALIDATE_RST=rst2html.py --strict

SERVER?=pypi

# Create the sdist
pypi_sdist:
# Validate the rst files
	$(VALIDATE_RST) AUTHORS.rst >/dev/null
	$(VALIDATE_RST) README.rst >/dev/null
	rm -rf dist
	python setup.py check
	python setup.py sdist
	tar tvfz dist/*.tar.gz

# Create and test the sdist
pypi_test: pypi_sdist
	rm -rf bond_venv bond_venv_tests
	virtualenv bond_venv
	bond_venv/bin/pip install dist/bond-*.tar.gz
	@echo "** Check that bond is installed"
	bond_venv/bin/pip show -f bond
	@echo "** Check the bond version"
	bond_venv/bin/python -c 'import bond; print bond.__version__'
	@echo
	@echo
	@echo "** We will now run the self-tests."
	@echo "** You should see an observation change (from 'a little fun' to 'a ton of fun'). This is on purpose!"
	@echo "** Press a key to continue with tests..." && read x_input
	@echo "** Run the tests"
	mkdir -p bond_venv_tests
	cp -rf tests/*.py tests/test_observations bond_venv_tests
	@echo "** Wipe the setup_paths_test, so we use the just-installed bond"
	echo "">bond_venv_tests/setup_paths_test.py
	@echo "** Fake a change in observations"
	sed $(SED_OPTS) -e 's/ton/little/' bond_venv_tests/test_observations/BondTest/test_result.json
	@echo "** Change directory to ensure that we will only reach the installed bond"
	cd bond_venv_tests && BOND_RECONCILE=console BOND_OBSERVATION_DIR=test_observations ../bond_venv/bin/python -m unittest discover -s . -p '*_test.py'

# Upload to PyPi
pypi_upload: pypi_sdist
	python setup.py register -r $(SERVER)
	python setup.py sdist upload -r $(SERVER)	

