# All test files import this module.
#
# Setup paths. This is important for when we run tests directly from PyCharm
import os
import sys
bond_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
if bond_dir not in sys.path:
    sys.path.append(bond_dir)


