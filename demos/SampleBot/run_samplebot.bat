@echo Off
set jar_files=
FOR %%f in (.\lib\*.jar) do (
    set jar_files=%%f;%jar_files%
)
@echo on

java -classpath "%jar_files%" samplebot.exe