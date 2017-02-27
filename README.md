### Details on the CompassReports Grails SSB
* Grails sdk 2.5
* JDK 1.7
* Browsers tested: Chrome, FF on Mac OSX, Windows 10 and Ubuntu 16
* Tech stack mix of JS libraries and Grails plugins:
    * application.js
    * download.js
    * Grails reverse engineering plugin
    * Grails datatables plugin
    * Apache Commons POI/HSSF and TempDir API
* Heavy use of JS libraries and JQuery ajax functions to call Grails controller actions rendering servlet response text
most cases
* TODO - should look into to upgrading to Grails 3.x to take advantages of Spring Boot features and the new fat jar
capabilities