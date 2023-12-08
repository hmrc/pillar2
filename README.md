# pillar2

The Pillar2 backend service provide the users with means to ensure the large multinational businesses pay a minimum
level of corporate income tax (15%) on their profits.

## Running the service locally

#### To compile the project:
The below command ensures the project is compiled without any errors

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

#### Using sbt

For local development, use `sbt run` but if it is already running in sm2, execute below command to stop the
service before running sbt commands.

    sm2 --stop PILLAR_2

This is an authenticated service, so users first need to be authenticated via GG in order to use the service.

Navigate to http://localhost:9949/auth-login-stub/gg-sign-in which redirects to auth-login-stub page

Make sure to fill in the fields as below:

***Redirect URL: http://localhost:10050/report-pillar2-top-up-taxes***

***Affinity Group: Organisation***

## Testing Endpoints

This backend service provides a few test-only endpoints, exposed via the **GET** HTTP method in order to be operated by the browser.

---------------------

```GET /get-all```

Gets all the records from the Mongo DB in the backend

> Response status: 200

---------------------

```GET /clear-all```

Clears all the record from the Mongo DB in the backend

> Response status: 200

---------------------

```GET /clear-current/:id```

Clears the current records with a specific ID from the Mongo DB in the backend

> Response status: 200

---------------------

```GET /registration-data/:id```

Gets the registration data of a specific ID from the Mongo DB in the backend

> Response status: 200

---------------------
<br><br>

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").