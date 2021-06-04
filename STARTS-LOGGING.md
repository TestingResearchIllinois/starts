
# Logging

## Intro

Logging in STARTS is a customized (read: simpler) version of [java.util.logging (JUL)](https://docs.oracle.com/javase/8/docs/api/java/util/logging/package-summary.html).

The code for logging is located in starts-core/src/main/java/edu/illinois/starts/util/Logger.java


For any piece of starts that you'd like to add logging to, begin by adding two import statements at the top:

- ``import edu.illinois.starts.util.Logger;``
- ``import java.util.logging.Level;``

_note: you may need to make minor changes to the positioning of these two import statements to ensure checkstyle does not break. Place ``edu.illinois.starts.util.Logger`` directly underneath the other imports with the same package name, and do the same with ``java.util.logging.Level``. Additionally, ensure you have a newline separating different package names_


Next, instantiate your Logger as a class variable:
- ``protected static final Logger logger = Logger.getGlobal();``

## Levels
There are 7 logging levels (excluding OFF and ALL), just like JUL:
- SEVERE (highest value)
- WARNING
- __INFO (default)__
- CONFIG
- FINE
- FINER
- FINEST (lowest value)

To set the logging level of your log, use the 

``setLoggingLevel(Level level)`` 

method.
i.e.,

``logger.setLoggingLevel(Level.CONFIG);``

To check the logging level, use the ``getLoggingLevel()`` method, which will return an object of type [Level](https://docs.oracle.com/javase/8/docs/api/java/util/logging/Level.html).
i.e.,

``Level currentLevel = logger.getLoggingLevel();``

## Writing messages
There are two methods you can use to log that differ only in the number of arguments you pass in.

###### ``public void log(Level lev, String msg, Throwable thr)``
should be used when you want to have a custom message AND an exception message


###### ``public void log(Level lev, String msg)``
should be used when you only want to have a custom message

i.e.,

``logger.log(Level.SEVERE, "houston we have a problem");``

In both cases above, the provided message will only be logged if the specified logging Level is equal to or higher in severity than the Level the logger is set to.
For example, if ``logger.setLoggingLevel(Level.SEVERE);``, then only ``logger.log()`` messages with Level.SEVERE will be spit out.
Similarly, if ``logger.setLoggingLevel(Level.CONFIG);``, then ``logger.log()`` with Level.INFO will be output, but not Level.FINER.

## Where will messages be output? 
Standard Output (System.out)

## Artifact storage
The logging granularities serve a dual purpose - both to control which log messages in code are sent to standard output, AND to control which artifacts are stored between runs.

The default __Level.INFO__ will store:
- _dependency file/checksum (.starts/deps.zlc)_

__Level.FINER__ will store:
- _dependency file/checksum (.starts/deps.zlc)_
- _list of all tests_
- _list of impacted tests_

__Level.FINEST__ will store:
- _dependency file/checksum (.starts/deps.zlc)_
- _list of all tests_
- _list of impacted tests_
- _list of non-impacted tests_
- _list of dependencies computed by jdeps_
- _classpath that STARTS used_
- _yasgl graph that STARTS constructed_
- _set of changed types_

To set the log level at runtime, call starts like this: 

``mvn starts:starts -DstartsLogging=<Level>``

i.e., 

``mvn starts:starts -DstartsLogging=FINEST``
