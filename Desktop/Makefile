OBJECTS = Screen.class Model.class ViewInterface.class

assign3: ${OBJECTS}
run: assign3
	java -cp json-simple-1.1.1.jar:. Screen
clean:
	rm -rf *.class

Screen.class: Screen.java
	javac -cp .:json-simple-1.1.1.jar Screen.java Model.java ViewInterface.java
