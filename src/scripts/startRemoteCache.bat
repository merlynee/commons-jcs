@rem echo off

call prep.bat
  
:run
rem set DBUGPARM=-classic -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,address=5000,suspend=n
%JAVA_HOME%\bin\java %DBUGPARM% -verbosegc -ms10m -mx200m -classpath %CLASSPATH% "-Djava.security.policy=%CURDIR%\src\conf\cache.policy" org.apache.jcs.auxiliary.remote.server.RemoteCacheServerFactory /remote.cache%1.ccf


