===========================================
Bond - Testing with Spies and Mocks
===========================================

Bond is a small library that can be used to spy values and mock functions during tests.
Spying is a replacement for writing
the ``assertEquals`` in your test, which are tedious to write and even more tedious to
update whem your test setup or code inevitably changes.
With Bond, you separate what is being verified, e.g., the variable named ``output``, from
what value it should have. This way you can quickly spy several variables, even have structured
values such as lists or dictionaries, and these values are saved into an observation log that is saved
for future reference. If the test observations are different you have the option to interact with a
console or visual tool to see what has changed, and whether the reference set of observations need to
be updated.

For more examples please look at:

- `The main project page <http://necula01.github.io/bond/>`_
- `A tutorial <http://necula01.github.io/bond/tutorial.html>`_
- `The API documentation <http://necula01.github.io/bond/tutorial.html>`_




