# CSE 344 Lab 2

## Setup

You will need several tools for this assignment:

- Azure
- Azure SQL Server
- Maven

These instructions assume you are on the Linux lab machines or attu.

## Testing

To test your solutions, type `mvn test` inside the `lab2` folder.
To run your solutions in an interactive mode, type `mvn clean compile assembly:single` then `java -jar target/lab2-1.0-jar-with-dependencies.jar` inside the `lab2` folder.
Our solutions, following all instructions, ran for less than 2 minutes on a database with 10 DTUs allocated.