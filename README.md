# pillar2

This service provides the users with means to ensure the large multinational businesses pay a minimum
level of corporate income tax (15%) on the profits.

## Running the service locally
`sbt clean update compile`

The above command ensures the compilation is successful without any errors.

`sbt run`

By default, the service runs locally on port **10051**

To use test-only route locally, run the below:

`sbt 'run -Dplay.http.router=testOnlyDoNotUseInAppConf.Routes 10051'`

#### Using Service Manager

You can use service manager to provide necessary assets to the pillar2 backend. 
**PILLAR2_ALL** service is responsible for starting up all the services required by the tax credits service project.

This can be started by running the below in a new terminal:

    sm2 --start PILLAR2_ALL

#### Using sbt 

For local development, use `sbt run` but if its already running in sm2, execute below command to stop the
service before running sbt commands.

    sm2 --stop PILLAR_2

#### To check code coverage:

    sbt scalafmt test:scalafmt it:test::scalafmt coverage test it:test coverageReport

#### Integration and unit tests

To run the unit tests within the project:

    `sbt test`

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").