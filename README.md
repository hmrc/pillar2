# pillar2

This service provides the users with means to ensure the large multinational businesses pay a minimum
level of corporate income tax (15%) on the profits.

## Running the service locally

The below command ensures the compilation is successful without any errors

`sbt clean update compile`

#### To check code coverage:

`sbt scalafmt test:scalafmt it:test::scalafmt coverage test it:test coverageReport`

#### Integration and unit tests

To run the unit tests within the project:

`sbt test`

#### Starting the server in local
`sbt run`

By default, the service runs locally on port **10051**

To use test-only route locally, run the below:

`sbt 'run -Dplay.http.router=testOnlyDoNotUseInAppConf.Routes 10051'`

### Using Service Manager

You can use service manager to provide necessary assets to the pillar2 backend. 
**PILLAR2_ALL** service is responsible for starting up all the services required by the tax credits service project.

This can be started by running the below in a new terminal:

    sm2 --start PILLAR2_ALL

The logs can be viewed by running the below command in a new terminal window

    sm2 --logs PILLAR_2

#### Using sbt 

For local development, use `sbt run` but if it is already running in sm2, execute below command to stop the
service before running sbt commands.

    sm2 --stop PILLAR_2

This is an authenticated service, so users first need to be authenticated via GG in order to use the service.

Navigate to http://localhost:9949/auth-login-stub/gg-sign-in which redirects to auth-login-stub page

Make sure to fill in the fields as below:

***Redirect URL: http://localhost:10050/report-pillar2-top-up-taxes***

***Affinity Group: Organisation***
<br><br><br>

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").