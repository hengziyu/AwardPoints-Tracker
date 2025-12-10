#!/bin/bash

# Get the directory where the script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

# Set JAVA_HOME to our bundled JDK
export JAVA_HOME="$SCRIPT_DIR/jdk"

# Add the bundled JDK's bin to the PATH
export PATH="$JAVA_HOME/bin:$PATH"

JAR_FILE="$SCRIPT_DIR/target/untitled-1.0-SNAPSHOT-jar-with-dependencies.jar"

# Check if the JAR file exists
if [ ! -f "$JAR_FILE" ]; then
    echo ""
    echo "Jar file not found in target directory."
    echo "Please build the project first by running the command:"
    echo "mvn clean package"
    echo ""
    exit 1
fi

# Run the application
echo "Starting the application..."
"$JAVA_HOME/bin/java" -jar "$JAR_FILE"

