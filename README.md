# pillar2-frontend
Backend microservice for Pillar 2  project. Pillar 2 refers to the Global Minimum Tax being introduced by the Organisation for Economic Cooperation and Development (OECD).

The Pillar 2 Tax will ensure that global Multinational Enterprises (MNEs) with a turnover of >€750m are subject to a minimum Effective Tax Rate of 15%, i.e. a top-up tax for Medium to Large MNEs.

## Running the service locally

    sbt run
Test-only route:

    sbt 'run -Dplay.http.router=testOnlyDoNotUseInAppConf.Routes 10051'
To run locally:

Navigate to http://localhost:9949/auth-login-stub/gg-sign-in which redirects to auth-login-stub page.


***Redirect URL: http://localhost:10050/report-pillar2-top-up-taxes***

***Affinity Group: Organisation***

## Key Terminologies

### Ultimate Parent Entity (UPE):
An ultimate parent is not a subsidiary of any other company and has a controlling interest in one or more other entities.
### Nominated Filing Member (NFM):
The nominated filing member is responsible for managing the group's tax returns and keeping business records.
### To check test coverage:

`sbt scalafmt test:scalafmt it:test::scalafmt coverage test it/test coverageReport`

### Integration and unit tests

To run unit tests:

    Run 'sbt test' from directory the project is stored in
To run Integration tests:

    sbt it/test

### Using Service Manager

You can use service manage to run all dependent microservices using the command below

    sm2 --start PILLAR2_ALL
To stop services:

    sm2 --stop PILLAR2_ALL


### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").