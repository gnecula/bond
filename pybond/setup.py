from distutils.core import setup

import os
here_dir = os.path.abspath(os.path.dirname(__file__))

def readme():
    with open(os.path.join(here_dir, 'README.rst')) as f:
        return f.read()

setup(
    name='bond',
    packages=['bond', 'bond.bond_helpers'],
    version='1.0.0',
    description='Testing with Spies and Mocks',
    long_description=readme(),
    summary='Testing with Spies and Mocks',
    author='George Necula, Erik Krogen',
    author_email='necula@cs.berkeley.edu',
    url='http://necula01.github.io/bond/',
    license='BSD',
    keywords=['testing', 'mocking'],
    package_dir={
        'bond' : 'bond'
    },
    package_data={
        'bond' : [ 'AUTHORS.rst', 'LICENSE', 'README.rst']
    },
    classifiers=[
        "Development Status :: 4 - Beta",
        "Intended Audience :: Developers",
        "Natural Language :: English",
        "License :: OSI Approved :: BSD License",
        "Operating System :: OS Independent",
        "Programming Language :: Python",
        "Programming Language :: Python :: 2",
        "Programming Language :: Python :: 2.6",
        "Programming Language :: Python :: 2.7",
        "Topic :: Software Development :: Libraries :: Python Modules",
        'Topic :: Software Development :: Testing'
    ]
)
