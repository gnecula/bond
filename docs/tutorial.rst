.. _tutorial:

==========================
Bond Tutorial
==========================

Bond was designed to simplify the **development and maintenance** of automated tests. There are two main uses
of Bond: spying and mocking. These use cases are all supported by a total of four Bond functions. We'll discuss spying first.

If you haven't done so already, you should read the :ref:`Getting Started guide <gettingstarted>`.

Part 1: Spying with Bond
----------------------------------

Spying inside your test code
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Note: All of the code for this part of the tutorial can be found within the ``tutorials/binary_search_tree``
directory (``tutorials/src/{main,test}/java/tutorial/binarysearchtree`` for Java)
of the `Bond sources <http://github.com/necula01/bond>`_.

Spying with Bond is meant to replace writting the common equality assertion calls in your tests, i.e., the validation
that some state variable has some expected value. These assertions are tedious to write, and even more tedious to
update when your code or your test fixture changes and you need to update the test. For this reason, people
tend to write fewer assertions than they should.

Consider, for example, that you have just implemented binary-search-trees (BST) and want to write tests.
You may write the following testing code:

.. container:: tab-section-group

   .. container:: tab-section-python

        .. code-block:: python
            :emphasize-lines: 11-15

            def test_bst(self):
                tree = BST()
                tree.insert(8)
                tree.insert(12)
                tree.insert(3)
                tree.insert(4)
                tree.insert(6)

                # WITHOUT BOND: Add self.assertEquals here to verify the position in the tree
                # of all the data points, in the order in which they were inserted
                self.assertEquals(8, tree.data)
                self.assertEquals(12, tree.right.data)
                self.assertEquals(3, tree.left.data)
                self.assertEquals(4, tree.left.right.data)
                self.assertEquals(6, tree.left.right.right.data)

   .. container:: tab-section-ruby

        .. code-block:: ruby
            :emphasize-lines: 12-16

            # Using RSpec
            describe BST do

                it 'should insert correctly' do
                    tree = BST()
                    tree.insert(8)
                    tree.insert(12)
                    tree.insert(3)
                    tree.insert(4)
                    tree.insert(6)

                    expect(tree.data).to eq(8)
                    expect(tree.right.data).to eq(12)
                    expect(tree.left.data).to eq(3)
                    expect(tree.left.right.data).to eq(4)
                    expect(tree.left.right.right.data).to eq(6)
                end
            end

   .. container:: tab-section-java

       .. code-block:: java
           :emphasize-lines: 10-16

           // Using JUnit
           @Test
           public void testAdd() {
             Node<Integer> tree = new Node<>(8);
             tree.insert(12);
             tree.insert(3);
             tree.insert(4);
             tree.insert(6);

             assertEquals(8, tree.data.intValue());
             assertEquals(12, tree.right.data.intValue());
             assertNull(tree.right.left);
             assertNull(tree.right.right);
             assertEquals(3, tree.left.data.intValue());
             assertEquals(4, tree.left.right.data.intValue());
             assertEquals(6, tree.left.right.right.data.intValue());
           }

That is a lot of asserting, and in fact, it is not even a complete test, because you'd have to
test, e.g., that 6 is a leaf node.

The alternative with Bond is as follows:

.. container:: tab-section-group

   .. container:: tab-section-python

      .. code-block:: python
            :emphasize-lines: 2, 13

            def test_bst(self):
                bond.start_test(self)              # Initialize Bond for this test

                tree = BST()
                tree.insert(8)
                tree.insert(12)
                tree.insert(3)
                tree.insert(4)
                tree.insert(6)

                # WITH BOND: record the value of the tree variable, and compare it
                # with previous recordings.
                bond.spy(tree=tree)  # Spy the whole tree

   .. container:: tab-section-ruby

        .. code-block:: ruby
            :emphasize-lines: 7, 19

            # Necessary to get the bond context
            require 'bond/spec_helper'

            # Using RSpec
            describe BST do
                # Automatically initializes Bond
                include_context :bond

                it 'should insert correctly' do
                    tree = BST()
                    tree.insert(8)
                    tree.insert(12)
                    tree.insert(3)
                    tree.insert(4)
                    tree.insert(6)

                    # WITH BOND: record the value of the tree variable, and compare it
                    # with previous recordings.
                    bond.spy(tree: tree)  # Spy the whole tree
                end
            end

   .. container:: tab-section-java

       .. code-block:: java
            :emphasize-lines: 2-3, 14

            // Automatically initializes Bond
            @Rule
            public BondTestRule btr = new BondTestRule()

            // Using JUnit
            @Test
            public void testAdd() {
              Node<Integer> tree = new Node<>(8);
              tree.insert(12);
              tree.insert(3);
              tree.insert(4);
              tree.insert(6);

              Bond.obs("tree", tree).spy("testAdd");
            }



What is happening there is that we call the ``spy`` function to tell Bond to record the value of the
``tree`` variable. There could be multiple calls to ``spy`` during a test.
The spied values (observations) are recorded in a file saved by default in a subdirectory called ``test_observations``
(except for Java, which does not support a default). This file should be checked in your repository along with
your sources. Next time Bond runs the same test it will compare the current observation with the reference one.
If there are differences, before concluding that the test has failed, you will get the opportunity to interact
with Bond to select what you want to be the new reference.

Here is the test observation spied by the test case we wrote above:

.. container:: tab-section-group

    .. container:: tab-section-python

        .. code-block:: javascript

            [
            {
                "__spy_point__": "testAdd1",
                "tree": {
                    "data": 8,
                    "left": {
                        "data": 3,
                        "right": {
                            "data": 4,
                            "right": {
                                "data": 6
                            }
                        }
                    },
                    "right": {
                        "data": 12
                    }
                }
            }
            ]

    .. container:: tab-section-ruby

        .. code-block:: javascript

            [
            {
                "__spy_point__": "testAdd1",
                "tree": {
                    "data": 8,
                    "left": {
                        "data": 3,
                        "right": {
                            "data": 4,
                            "right": {
                                "data": 6
                            }
                        }
                    },
                    "right": {
                        "data": 12
                    }
                }
            }
            ]

    .. container:: tab-section-java

        .. code-block:: javascript

            [
            {
              "__spy_point__": "testAdd",
              "tree": {
                "data": 8,
                "left": {
                  "data": 3,
                  "left": null,
                  "right": {
                    "data": 4,
                    "left": null,
                    "right": {
                      "data": 6
                      "left": null,
                      "right": null
                    }
                  }
                },
                "right": {
                  "data": 12
                  "left": null,
                  "right": null
                }
              }
            }
            ]


Note that this observation acts implicitly as 15 equality assertions (5 for the data values, and 10 more for
the ``left`` and ``right`` pointers on the nodes).
Furthermore, the assertions are presented in a structure that is much easier
to read that a sequence of equality assertions. Finally, with Bond your test code contains only the names
of the variables you want to assert on; the values they are equal to are saved separated from your test.
This will turn out to be crucial next.

If you need to make a change in the code, or in the testing setup, it is very tedious to fix the assertions.
Let's say that you decide that you get a better test coverage with a different tree where instead of 4 you want to
insert 7 in the tree. If you run the traditional test, you will see the familiar test failure:


.. container:: tab-section-group

    .. container:: tab-section-python

        .. code-block:: diff

            ======================================================================
            FAIL: testAdd1 (bst_tests.NodeTest)
            ----------------------------------------------------------------------
            Traceback (most recent call last):
              File "bond/pybond/tutorials/binary_search_tree/bst_tests.py", line 49, in test_bst
                self.assertEquals(4, tree.left.right.data)
            AssertionError: 4 != 7

    .. container:: tab-section-ruby

        .. code-block:: diff

            Failures:

              1) Node should add nodes to the BST correctly, testing without Bond
                 Failure/Error: expect(tree.left.right.data).to eq(4)

                   expected: 4
                        got: 7

                   (compared using ==)
                 # ./bst_spec.rb:20:in `block (2 levels) in <top (required)>'

    .. container:: tab-section-java

        .. code-block:: diff

            tutorial.binarysearchtree.BinarySearchTreeTest > testAdd FAILED
                java.lang.AssertionError: expected:<4> but was:<7>
                    at org.junit.Assert.fail(Assert.java:88)
                    at org.junit.Assert.failNotEquals(Assert.java:834)
                    at org.junit.Assert.assertEquals(Assert.java:645)
                    at org.junit.Assert.assertEquals(Assert.java:631)
                    at tutorial.binarysearchtree.BinarySearchTreeTest.testAdd(BinarySearchTreeTest.java:37)


Not only does your test abort on the first assertion, but it turns out that you have to fix
several of the assertions because the tree structure has changed. This is a common scenario when
your tests are aggressive about validating the data, and your test scenario or the underlying code
inevitably evolves.

With Bond, there is absolutely no change to the test code, precisely because the actual expected
tree shape is not part of your code! Instead, the test notices a discrepancy in the
observations, and tries to reconcile the observations.

.. container:: tab-section-group

   .. container:: tab-section-python

       You can read more about ``bond.start_test`` and ``bond.spy`` in the :ref:`API documentation <api_spy>`.

   .. container:: tab-section-ruby

       You can read more about the ``:bond`` context and `bond#spy <rbond/Bond.html#spy-instance_method>`_
       in the `API documentation <rbond/Bond.html>`__.

   .. container:: tab-section-java

       You can read more about the `BondTestRule <jbond/bond/BondTestRule.html>`_ and
       `Bond.spy <jbond/bond/Bond.html#spy-->`__ in the `API documentation <jbond/index.html>`__.


Reconciling Bond observations
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Following along the previous example, when a test run finishes it compares the set of
spied observations with the saved reference ones. If there are no differences,
testing proceeds as before. If there are differences, there are multiple possible
reconciliation methods. By default, you will be presented with a diff of the changes
and a small reconciliation menu, as shown below:

.. container:: tab-section-group

    .. container:: tab-section-python

        .. code-block:: diff

            --- reference
            +++ current
            @@ -6,8 +6,8 @@
                     "left": {
                         "data": 3,
                         "right": {
            -                "data": 4,
            -                "right": {
            +                "data": 7,
            +                "left": {
                                 "data": 6
                             }
                         }

            There were differences in observations for NodeTest.test_bst:
            Do you want to accept the changes (NodeTest.test_bst) ? ( [y]es | [k]diff3 | [n]o):

    .. container:: tab-section-ruby

        .. code-block:: diff

            --- reference
            +++ current
            @@ -6,8 +6,8 @@
                 "left": {
                   "data": 3,
                   "right": {
            -        "data": 4,
            -        "right": {
            +        "data": 7,
            +        "left": {
                       "data": 6
                     }
                   }

            There were differences in observations for bst_spec.Node_should_add_nodes_to_the_BST_correctly__testing_with_Bond:
            Do you want to accept the changes (bst_spec.Node_should_add_nodes_to_the_BST_correctly__testing_with_Bond) ? ( [y]es | [k]diff3 | [n]o):


    .. container:: tab-section-JAVA

        .. code-block:: diff

            There were differences in observations for tutorial.binarysearchtree.BinarySearchTreeTest.testAdd
            --- reference
            +++ current
            @@ -6,15 +6,15 @@
                 "left": {
                   "data": 3,
                   "left": null,
                   "right": {
            -        "data": 4,
            -        "left": null,
            -        "right": {
            +        "data": 7,
            +        "left": {
                       "data": 6,
                       "left": null,
                       "right": null
            -        }
            +        },
            +        "right": null
                   }
                 },
                 "right": {
                   "data": 12,
            There were differences in observations for tutorial.binarysearchtree.BinarySearchTreeTest.testAdd

            Do you want to accept the changes (tutorial.binarysearchtree.BinarySearchTreeTest.testAdd)?


At this  point you can click "y" to accept the new changes (they will be saved as the new reference
and the test will pass), or "n" to abort the test. Furthermore, if you click "k" at the above prompt,
Bond will invoke a visual merging tool (in this case ``kdiff3``),
that allows you to navigate all differences, see the context in which they appeared by
inspecting nearby observations, select easily for each difference, or for all, whether the
new observed behavior is correct. If all differences are accepted, Bond will save the new observation file as
future reference. Voila! You have just updated the expected values with a click of a button. Bond gives you
deep assertions about your test while keeping the assertion maintenance cost low.

.. image:: _static/kdiff3_bst1.png


You can control the reconciliation method using a parameter to ``bond.start_test`` in Python / a parameter to ``:include_context :bond`` in Ruby / the `withReconciliationMethod() <jbond/bond/BondTestRule.html#withReconciliationMethod-bond.reconcile.ReconcileType->`_ method on `BondTestRule <jbond/bond/BondTestRule.html>`_ in Java, or with the environment
variable ``BOND_RECONCILE``, with possible values:

* ``accept`` : accept the new observations and change the reference
* ``abort`` : abort the test
* ``console`` : show the above console interaction menu, falling back to a dialog box if a console is not available
* ``dialog`` : ask for user input via a popup dialog box
* ``kdiff3``: invoke the ``kdiff3`` merging tool (must have it installed and available in your ``PATH``)

If the test fails, then you will still be shown the differences in the observations, but you will not have
the choice to accept them as the new reference observations.

The following is the UML sequence diagram for the interaction between the
test, the system-under-test (e.g., the binary-search tree example code from above),
and the Bond library:

.. uml::

   @startuml

   participant Test
   participant SUT as "System-under-test"
   participant Bond
   actor Diff as "Interactive merging tool"

   activate Test
   [-> Test
   group "prepare test"
       activate Bond
       Test -> Bond : start_test*()
       Bond -> Test
   end
   Test -> SUT : insert()
   SUT -> Bond : spy('intermediate data')
   SUT -> Test
   Test -> Bond : spy('data')
   Test -> Bond : spy('more data')

   group "finish test"
       Test -> Bond: end_test*()
       Bond -> Bond : save\nobservations

       alt observations different from reference
          Bond -> Diff: interactive reconcile
          Diff -> Bond
       end

       Bond -> Test
   end
   deactivate Bond
   deactivate Test
   @enduml

Once ``start_test()`` has been called, any subsequent call to
``bond.spy`` will record the observations, which are saved at the end
of the test. Both the test and the system-under-test can spy values.
If the saved observations are different from the
reference ones, an interactive merging session is initiated to decide
whether the current observations should be the new reference ones.

Note that the ``start_test()`` method is explicit in Python, but is
implicit in Ruby, if you add ``include_context :bond`` to the RSpec
test, and in Java, if you add the JUnit @Rule `BondTestRule <jbond/bond/BondTestRule.html>`_. The ``end_test()`` method call is automatic at the end of the
test in all languages.

Spying inside your production code while testing
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Sometimes you want to validate the behavior of your code during testing, beyond just
checking the state at the end of the test. For this purpose you
can use ``bond.spy`` in your production code. This function has effect only if you
called ``bond.start_test`` first.

In the next section we will see another Bond function for spying, and mocking, inside
your production code.

For a pattern to use when including Bond in production code, see :ref:`pattern_bond_import`.


Part 2: Mocking with Bond
--------------------------------

.. container:: tab-section-group

    .. container:: tab-section-python

        Sometimes you want not only to spy values from your production code,
        but also to replace some of those values. Spying and mocking together
        can be achieved if you place the ``bond.spy_point`` annotation on a
        function or a method in your production code.  Let's assume that the
        code to be tested (system under test) is expected to invoke a
        collaborator method called ``make_request``, whose
        purpose is to make HTTP requests to other services. You may want to
        spy how many times this method is called in your tests, and with what
        arguments, and possibly what it returns for each call. You also want
        your tests to be able to bypass the actual HTTP request and provide
        mock results for this function.  This can be achieved with the
        ``bond.spy_point`` function annotation, as shown below:

        .. code-block:: python
            :emphasize-lines: 1

            @bond.spy_point()
            # Among other things, has the effect of injecting a call to
            #
            #     bond.spy(spy_point_name='module.make_request', url=url, data=data)
            #
            # where `module` is the name of the module containing make_request.
            # If make_request was contained within a class, the default spy point
            # name would be `Class.make_request`.
            def make_request(url, data=None):
                "HTTP request (GET, or POST if the data is provided)"
                resp = urllib2.urlopen(url, data)
                return (resp.getcode(), resp.read())

        Just like ``bond.spy``, this annotation has effect only if ``bond.start_test`` has been called,
        meaning that this is a test run. One of the effects of this annotation is to inject a call
        to ``bond.spy`` with the method name as the spy point and the arguments as the observation,
        as shown in the code example above.

        You can read more about ``bond.spy_point`` in the :ref:`API documentation <api_spy_point>`.
        Read on to find out what else you can do with spy point annotations.

        A spy point annotation on a method is also able to inject code to execute on every call to the
        method. This code can do multiple things, and can be controlled from the test code:

        * further decide on which invocations of the spy point they activate, based on various
          filters on the function arguments.
        * spy the values of the arguments, and optionally the result also.
        * control which arguments are spied and how the observations are formatted.
        * execute additional test code on each call.
        * bypass the actual body of the method and return a result prepared by the testing code,
          or throw an exception when the call is reached.

    .. container:: tab-section-ruby

        Sometimes you want not only to spy values from your production code,
        but also to replace some of those values. Spying and mocking together
        can be achieved if you place the ``bond.spy_point`` annotation on a
        function or a method in your production code.  Let's assume that the
        code to be tested (system under test) is expected to invoke a
        collaborator method called ``make_request``, whose
        purpose is to make HTTP requests to other services. You may want to
        spy how many times this method is called in your tests, and with what
        arguments, and possibly what it returns for each call. You also want
        your tests to be able to bypass the actual HTTP request and provide
        mock results for this function.  This can be achieved with the
        ``bond.spy_point`` function annotation, as shown below (note that
        any class or module which you wish to spy on must ``extend BondTargetable``):

        .. code-block:: ruby
            :emphasize-lines: 3,5

            class MyClass
                # Denotes this class as being able to be targetted by Bond
                extend BondTargetable

                bond.spy_point
                # Among other things, has the effect of injecting a call to
                #
                #     bond.spy('MyClass#make_request', url: url, data: data)
                #
                # If make_request was a class method, `MyClass.make_request`
                # would have been used instead.
                def make_request(url, data=nil)
                    uri = URI(url)
                    if data.nil?
                        resp = Net::HTTP.get_response(uri)
                    else
                        resp = Net::HTTP.post_form(uri, data)
                    end
                    return [resp.code, resp.message]
                end
            end

        Just like ``bond.spy``, this annotation has effect only if ``bond.start_test`` has been called,
        meaning that this is a test run. One of the effects of this annotation is to inject a call
        to ``bond.spy`` with the method name as the spy point and the arguments as the observation,
        as shown in the code example above.

        You can read more about ``bond.spy_point`` in the
        `API documentation <rbond/BondTargetable.html#spy_point-instance_method>`__.
        Read on to find out what else you can do with spy point annotations.

        A spy point annotation on a method is also able to inject code to execute on every call to the
        method. This code can do multiple things, and can be controlled from the test code:

        * further decide on which invocations of the spy point they activate, based on various
          filters on the function arguments.
        * spy the values of the arguments, and optionally the result also.
        * control which arguments are spied and how the observations are formatted.
        * execute additional test code on each call.
        * bypass the actual body of the method and return a result prepared by the testing code,
          or throw an exception when the call is reached.

    .. container:: tab-section-java

        Sometimes you want not only to spy values from your production code,
        but also to replace some of those values. Let's assume that the
        code to be tested (system under test) is expected to invoke a
        collaborator method called ``makeRequest``, whose
        purpose is to make HTTP requests to other services. You may want to
        spy how many times this method is called in your tests, and with what
        arguments, and possibly what it returns for each call. You also want
        your tests to be able to bypass the actual HTTP request and provide
        mock results for this function. This can be achieved with the
        ``SpyPoint`` method annotation, as shown below:

        .. code-block:: java
            :emphasize-lines: 3

            public class MyClass {

              @SpyPoint
              // Among other things, has the effect of injecting a call to
              //
              //   Bond.obs("arg0[String]", url).spy("MyClass.makeRequest")
              //
              // In Java 7, names of parameters are not available, so they are
              // spied as arg0, arg1, etc., along with their type. In Java 8,
              // if you compile with the `-parameters` flag, their names will
              // used instead.
              public String makeRequest(String url) {
                // The actual production code for making a GET request
              }

        Just like ``Bond.spy``, this annotation has effect only if ``Bond.startTest`` has been called
        (or ``BondTestRule`` has been used), meaning that this is a test run. Unfortunately, there are
        some additional restrictions: you must also run your JUnit test using PowerMock with one of its
        test runners. See `BondMockPolicy <jbond/bond/spypoint/BondMockPolicy.html>`_ for more details
        and specifics of how to set up your test for mocking, and
        `SpyPoint <jbond/bond/spypoint/SpyPoint.html>`_ for the options you can use on a ``@SpyPoint``.

        One of the effects of this annotation is to inject a call to ``Bond.spy`` with the method name
        as the spy point and the arguments as the observation, as shown in the code example above.
        A spy point annotation on a method is also able to inject code to execute on every call to the
        method. This code can do multiple things, and can be controlled from the test code:

        * further decide on which invocations of the spy point they activate, based on various
          filters on the function arguments.
        * spy the values of the arguments, and optionally the result also.
        * control which arguments are spied and how the observations are formatted.
        * execute additional test code on each call.
        * bypass the actual body of the method and return a result prepared by the testing code,
          or throw an exception when the call is reached.

        If you don't want to use PowerMock to run your tests, you can achieve a similar effect
        by calling ``Bond.spy`` at the start of the method you would like to mock, and if a result
        was returned (which is guaranteed to be false if you are not currently testing), using that
        result instead of the production code. Note that you can also use
        `Bond.isActive() <jbond/bond/Bond.html#isActive-->`_ to achieve similar results
        (see the `inline spy point example <patterns.html#inline-spy-and-mock-points>`_).
        For example:

        .. code-block:: java
            :emphasize-lines: 3-7

            public class MyClass {
              public String makeRequest(String url) {
                // Spy and check if mocked
                SpyResult<String> result = Bond.obs("url", url)
                                               .spy("MyClass.makeRequest", String.class);
                if (result.isPresent()) {
                  return result.get();
                }
                // The actual production code for making a GET request
              }
            }

        Using ``spy`` to return a value will return a `SpyResult <jbond/bond/SpyResult.html>`_, on which
        ``isPresent()`` will be true if a value was available for the given spy point name. You can then
        use ``get`` to retrieve that value.


The behavior of spy points can be controlled with agents that are deployed from the
test code, as shown in the following example, where the test is deploying two
agents for the ``make_request`` spy point that we have instrumented earlier.

.. container:: tab-section-group

   .. container:: tab-section-python

      .. code-block:: python
           :emphasize-lines: 2-8

           def test_with_mocking(self):
               bond.start_test()
               bond.deploy_agent('module.make_request',
                                 url__endswith='/books',
                                 result=(200, json.dumps(mock_books_response)))
               bond.deploy_agent('module.make_request',
                                 url__contains='/books/100',
                                 result=(404, 'Book not found'))

               call_my_code_that_will_make_request()


   .. container:: tab-section-ruby

       .. code-block:: ruby
           :emphasize-lines: 2-7

           it 'should be able to call out to mock services' do
                bond.deploy_agent('MyClass#make_request',
                                  url__endswith: '/books',
                                  result: [200, mock_books_response.to_json])
                bond.deploy_agent('MyClass#make_request',
                                  url__contains: '/books/100',
                                  result: [404, 'Book not found'])

                call_my_code_that_will_make_request()
           end

   .. container:: tab-section-java

        .. code-block:: java
           :emphasize-lines: 3-13

           @Test
           public void testWithMocking() {
              // Deploy an agent to intercept the /books request
              SpyAgent makeRequestBooksAgent = new SpyAgent()
                     .withFilterKeyEndsWith("arg0[String]", "/books")
                     .withResult(mockBookResponse);
              Bond.deployAgent("MyClass.makeRequest", makeRequestBooksAgent);

              // Deploy another agent to simulate error for a given book
              SpyAgent makeRequestBookMissingAgent = new SpyAgent()
                     .withFilterKeyContains("arg0[String]", "/books/100")
                     .withException(new HttpException(404));
              Bond.deployAgent("MyClass.makeRequest", makeRequestBookMissingAgent);

              callMyCodeThatWillMakeRequest();

In the above example the first agent will instruct the ``make_request`` spy point to
skip the actual body of the method and return immediately a respose with status code
200 (in Python/Ruby) and the body being some mocked data structure. The value provided
as ``result`` by the agent is used directly in place of the normal return of the method.
The second agent simulates a 404 error when a particular url is encountered.

The later deployed spy agents override previously deployed ones. This is useful when you want to
deploy a default agent, e.g., return success on every HTTP request, and then for specific tests,
or during a test, you want to deploy a more specific agent that has another behavior.

.. container:: tab-section-group

    .. container:: tab-section-python

        You can read more about ``bond.deploy_agent`` in the :ref:`API documentation <api_deploy_agent>`.

    .. container:: tab-section-ruby

        You can read more about ``bond.deploy_agent`` in the
        `API documentation <rbond/Bond.html#deploy_agent-instance_method>`__.

    .. container:: tab-section-java

        You can read more about ``Bond.deployAgent()`` in the
        `API documentation <jbond/bond/Bond.html#deployAgent-java.lang.String-bond.SpyAgent->`__.

The following is the UML sequence diagram for using Bond for mocking:

.. uml::

   @startuml

   participant Test
   participant SUT as "System-under-test"
   participant DOC as "Collaborator\nlibrary"
   participant Bond
   actor Diff as "Interactive merging tool"

   activate Test
   [-> Test
   activate Bond
   group "prepare test"
       Test -> Bond : start_test*()
       Bond -> Test
       Test -> Bond : deploy_agent(...)
       Test -> Bond : deploy_agent(...)
   end

   Test -> SUT : call_my_code()
   SUT -> DOC : make_request(url)
   DOC -> Bond : spy('make_request', url=url)
   Bond -> Bond : find and use\nactive agent
   Bond -> SUT : mock response
   SUT -> Test

   group "finish test"
      Test -> Bond: end_test*()
      Bond -> Bond : save\nobservations

      alt observations different from reference
         Bond -> Diff: interactive reconcile
         Diff -> Bond
      end

      Bond -> Test
   end
   deactivate Bond
   @enduml

In the above diagrams we see that the test would deploy a number of
agents for specific spy points that would be reached during the
execution, before the test invokes the system under test. When the
system under test invokes the collaborator method on which a spy point
has been declared, Bond is going to look for an active deployed agent
for that spy point and use the mock result provided by the agent.

To learn more about general usage patterns visit :ref:`patterns`.

Part 3: Record-Replay Style Mocking
------------------------------------

.. container:: tab-section-group

    .. container:: tab-section-python

        Coming soon! This feature is currently only supported in Ruby.

    .. container:: tab-section-ruby

        In some cases, it may be cumbersome to explicitly specify what you
        want a mock to return. For instance, in the ``make_request`` example above,
        we used the mock to return the ``mock_books_response`` object in JSON form,
        but this object may be very large and complex. It would preferable not to have
        to manually specify this object, and what will often happen in practice is to
        run the request in a real manner once, view the response, and copy-paste this
        into your mocking code. But, just as we saw that Bond can reduce the amount of
        code you need to write by replacing assertions with calls to ``spy``, we can
        leverage the record-replay feature of Bond to take care of this for you
        automatically. Essentially, Bond goes through the process of observing the
        request and response, asks you if they are as you expect, and if so, saves them for
        mocking later -- no pasting or complex object creation necessary.

        We will continue from the example in the previous section. No change to the
        ``make_request`` method is necessary; the ``bond.spy_point`` annotation is
        applied in the same manner that it is during normal spy point mocking.
        Previously, we used this code to deploy agents which would provide mocking:

        .. code-block:: ruby

            bond.deploy_agent('MyClass#make_request',
                              url__endswith: '/books',
                              result: [200, mock_books_response.to_json])
            bond.deploy_agent('MyClass#make_request',
                              url__contains: '/books/100',
                              result: [404, 'Book not found'])

        Using replay-record functionality, we would instead have simply:

        .. code-block:: ruby

            bond.deploy_record_replay_agent('MyClass#make_request')

        The first time ``make_request`` is called during this test, no recorded
        value is available. What happens next depends on the value of ``BOND_RECONCILE``;
        if it is 'accept', the request will be recorded. If it is 'abort', an
        error will be thrown. For 'console'/'dialog', you will be prompted
        for which action to take. This can be overridden by specifying
        ``record_mode: true`` when the record-replay agent is deployed, which
        will force the agent to record regardless of the reconcile type.

        When a value is recorded, Bond allows the live method to perform its
        normal function, but observes the return value. This value is then displayed
        to you, along with the arguments to the function which resulted in the value.
        You then have the option to edit the return value before saving it to be
        used in future test runs.

        Whenever a record-replay agent is encountered for which a saved value is
        available and which does not explicitly have ``record_mode: true``, the
        saved value is simply replayed instead of calling the live method.
        This achieves the same behavior as our previous mocking, but we didn't
        have to write any expectations about what parameters the method would
        be called with, or write a result to return. Bond has taken care of all
        of this for us!

        Note that in the standard mocking example we returned two different values
        for the request depending on the arguments ``make_request`` was called with;
        during record-replay, each saved response is matched with a specific set of
        call arguments, so we can again have two different responses. We can also
        optionally differentiate multiple sequential calls with the same arguments,
        treating them as different based on their call order and thus returning
        distinct results.

        We demonstrate here record-replay in the context of HTTP requests, which
        is certainly a common use case, but record-replay can be used for any
        type of mocking to avoid having to write the expected mock behavior
        manually.

        The following is the UML sequence diagram for using Bond for record-replay mocking:

        .. uml::

           @startuml

           participant Test
           participant SUT as "System-under-test"
           participant DOC as "Collaborator\nlibrary"
           participant Bond
           actor User as "User Input"

           activate Test
           [-> Test
           activate Bond
           group "prepare test"
               Test -> Bond : start_test*()
               Bond -> Test
               Test -> Bond : deploy_replay_record_agent(...)
           end

           group "first test run; record"
               Test -> SUT : call_my_code()
               SUT -> DOC : make_request(url)
               DOC -> Bond : spy('make_request', url=url)
               Bond -> Bond : begin recording
               DOC -> Bond : spy('make_request.result',\nresult=result)
               Bond -> User : edit and accept\nrecorded result
               Bond -> SUT : return result
               SUT -> Test

               Test -> Bond: end_test*()
               Bond -> Bond : save\nobservations

               Bond -> User: interactive reconcile
               User -> Bond
           end

           group "subsequent test run; replay"
               Test -> SUT : call_my_code()
               SUT -> DOC : make_request(url)
               DOC -> Bond : spy('make_request', url=url)
               Bond -> SUT : return saved result
               SUT -> Test

               Test -> Bond: end_test*()
               Bond -> Bond : no changes\ndo nothing
           end

           deactivate Bond
           @enduml

        The test deploys a record-replay agent, and on the first call to the spy
        point Bond simply observes the behavior of the real method, then requests
        user input to confirm that the arguments and response are as expected.
        On subsequent runs, Bond simply looks up the saved value and returns it.

        You can find some more information about specifics of behavior in the
        `API documentation <rbond/Bond.html#deploy_record_replay_agent-instance_method>`__.

    .. container:: tab-section-java

        Coming soon! This feature is currently only supported in Ruby.



.. include:: example_heat.rst
