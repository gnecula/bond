import os
import shutil
import unittest
import re

import setup_paths_test
from bond import bond

from bond import bond_reconcile
from bond.bond_helpers import format_at

from bond_test import setup_bond_self_test, teardown_bond_self_test

class FormatterTest(unittest.TestCase):

    @bond.spy_point()
    def annotated_standard_method(self, arg1, arg2):
        return 'return value'

    def setUp(self):
        self.testing_observation_dir = '/tmp/bondObservations Dir'  # Intentionally put a space in the path
        self.reference_file = os.path.join(self.testing_observation_dir, 'reference.json')
        self.current_file = os.path.join(self.testing_observation_dir, 'reference_now.json')

        self.reference_file_content = ""



