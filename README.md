
# pillar2


This service provides a means for users to ensure that large multinational businesses pay a minimum
level of corporate income tax (15%) on the profits.

## Using Service Manager

You can use service manager to provide assets to the piller2 backend. the PILLAR2_ALL service is responsible for starting up all services required by the tax credits service project.
This can be started by running:

    sm --start PILLAR2_ALL

##  To run the locally:

    Run 'sbt run' from within the project and it will run at port 10051

## To check code coverage:

    sbt scalafmt test:scalafmt it:test::scalafmt coverage test it:test coverageReport

## Integration and unit tests

To run the unit tests:

    Run 'sbt test' from within the project

To use testonly route locally .

    sbt 'run -Dplay.http.router=testOnlyDoNotUseInAppConf.Routes 10051' 



### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
### License
