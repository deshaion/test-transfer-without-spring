# The task
Design and implement a RESTful API (including data model and the backing implementation) for money transfers between accounts.
Explicit requirements:
1. keep it simple and to the point (e.g. no need to implement any authentication, assume the APi is invoked by another internal system/service)
2. use whatever frameworks/libraries you like (except Spring, sorry!) but don't forget about the requirement #1
3. the datastore should run in-memory for the sake of this test
4. the final result should be executable as a standalone program (should not require a pre-installed container/server)
5. demonstrate with tests that the API works as expected

Implicit requirements:
1. the code produced by you is expected to be of high quality.
2. there are no detailed requirements, use common sense.

# Solution
* Why vert.x? Usually, I use spring framework. So I didn't want to do the test without a framework. And It was interesting to use the async framework. I didn't use it before
* I like DI, so I have added dagger framework.
* Why I separate Account and AccountBalance tables? In this task, it is maybe extra work, but I think that in a real project I would create two tables. If I change the balance, so then I don't change another account's columns and indexes
* Want to add additional unit tests. But I think I already spent a lot of time on this test and that is why I am sending this email.

# The result
* It doesn't compile (problem with lombok dependency) **[FIXED]**
* Lack of tests: only one positive scenario test over a single endpoint (on a total of 8)
* Lack of DB consistency: allows to delete accounts with balances/transfers **[FIXED]** 
* There also might be too much operation over account​ 
