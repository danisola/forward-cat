# Forward Cat
Forward Cat is a service that allows you to quickly create a temporary email address that automatically forwards to your real email address.

## How to build it
In order to build Forward Cat you need:

*   Java 7+
*   Maven 3+

Just head into the root directory and type:

    mvn clean install 


## Structure
The project is divided in three modules: the email server, the web server and some common classes shared between the other two.

### Web Server [forward-cat-server]
To serve the website we use a custom framework built on [Netty](http://netty.io/). It manages the creation of temporary email addresses, which are stored in a [Redis](http://redis.io/) database. 

### Email Server [forward-cat-james]
To send and receive emails, we use [Apache James](http://james.apache.org/). When receiving a new email, we check whether a valid temporary email address exists and we forward the email accordingly.

### Shared Classes [forward-cat-common]
Since the temporary email addresses are stored as JSON values, this module contains the class that maps them. To do so, we us [Jackson](http://jackson.codehaus.org/).
