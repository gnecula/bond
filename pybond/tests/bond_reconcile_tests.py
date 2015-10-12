import os
import shutil
import unittest

import setup_paths_test
from bond import bond

from bond import bond_reconcile
from bond_tests import BondTest

class ReconcileTest(unittest.TestCase):

    def setUp(self):
        self.testing_observation_dir = '/tmp/bondObservationsDir'
        self.reference_file = os.path.join(self.testing_observation_dir, 'reference.json')
        self.current_file = self.reference_file.replace('reference', 'reference_now')

        self.reference_file_content = """
[
{
   "__spy_point_name" : "point 1",
   val" : 12345
}
]
"""
        BondTest.setupUpBondSelfTests(self)


    def prepareObservations(self,
                            reference_file_content=None,
                            current_file_content=None):
        """
        Prepare reference and current files
        :param reference_file_content:
        :param current_file_content:
        :return:
        """
        if os.path.isdir(self.testing_observation_dir):
            shutil.rmtree(self.testing_observation_dir)
        os.makedirs(self.testing_observation_dir)

        if reference_file_content:
            with open(self.reference_file, 'w') as f:
                f.write(reference_file_content)

        if current_file_content:
            with open(self.current_file, 'w') as f:
                f.write(current_file_content)


    def invoke_top_reconcile(self, merge=None):
        bond_reconcile.reconcile_observations(dict(merge=merge),
                                              'test 1',
                                              self.reference_file,
                                              self.current_file)

    def testNoReference(self):
        self.prepareObservations(reference_file_content=None,
                                 current_file_content=self.reference_file_content)
        self.invoke_top_reconcile(merge='console')

