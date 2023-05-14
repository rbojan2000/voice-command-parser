# Voice Command Parser: Stateless Processing in Kafka Streams

## Our Application
An application that will receive Voice Commands and will convert them into text commands, if it's able to recognize them, and will put the unrecognized commands in a different topic.

The audio will be in FLAC format and is prepared to be used by the Speech To Text API in Google Cloud Platform. For the purpose of testing, we are going to be using a mock service.

The commands that are not in English will be translated to English to be further processed by a third hypothetical service. The translation service will also be mocked for testing purposes.
## Application Diagram
![Application Diagram](docs/appDiagram.png)

