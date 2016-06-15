# Thymes2 Framework

## Introduction

*Thymes2*, or *X2* for short, is a Java framework designed for back-end applications that on one end are backed by persistent storage (a database), possibly communicate with a range of other back-end services, and on the other end expose a REST API. The framework is an alternative to several Java EE technologies, primarily implementations of [JAX-RS](https://jax-rs-spec.java.net/) and [JPA](https://java.net/projects/jpa-spec/). It is modern, practical and lightweight. The framework is open source, free and is available under [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).

There is usually little responsibility for using terms "modern", "practical" and "lightweight" since these terms do not have a precise, official definition and have been used to describe so many different things, that by now they have lost almost any useful meaning. However, the words do have their meaning, so let's explain it and see how it is applicable to X2 Framework.

* **Modern**

  X2 is designed and built from scratch, it does not carry any legacy compatibility requirements and it is created for developing new projects. For example, when the framework was being designed, no attention was paid to make it support exposing a SOAP interface (it actually may, but that is irrelevant). On the other hand, when the persistence API was designed, it was designed to support not only RDBMS, but possibly NoSQL as well. The goal is to give application developers a set of tools for creating new applications that satisfy a set of actual, modern technical requirements.

* **Practical**

  X2 is not an implementation of any beautiful mathematical or philosophical theory or concept. The framework's feature set and the way how it is exposed to the application developers is based on accumulated experience of developing real life projects for a variety of industries. It is also important to mention, that not only software developers' point of view is taken into account, but also the application life cycle after the initial release, including deployment in live production environments, maintenance, monitoring, updates, etc.

* **Lightweight**

  This is probably the most misused and, therefore, blurred term. A few assertions must be made to bring the term's meaning back into focus:

  * Lightweight means minimum dependency on third-party libraries. Compiled binary of an application that uses the framework must be small. Yes, storage is becoming more available and less expensive. However, a deployed application is not exactly just "stored". The size of the application binary starts to matter when we start deploying it, especially if we are talking about multiple instances working in a pool.
  * Lightweight means transparency. When the application makes a call to the framework, the effect of the call must be immediate, predictable and easy to trace and debug. A framework that behind the scenes wraps objects in binary enhanced proxies is not lightweight. A framework that makes decisions on when and in what order to send commands to the database is not lightweight.
  * Lightweight means simplicity of the concept and extensibility. X2 is not attempting to be a universal tool for all imaginable cases. It focuses on a certain type of back-end applications. If your application exposes a REST API, stores data in a database and, potentially, talks to other services&mdash;X2 was designed for you. All components of the framework itself can have multiple implementations and those implementations can be packaged and distributed separately.

From these principles several requirements were formulated for the framework and subsequently implemented.

Application that are built using the framework are packaged as Java web-applications (web-archives) and run under any simple Servlet container, such as [Apache Tomcat](http://tomcat.apache.org/) or [Jetty](http://www.eclipse.org/jetty/). Alternatively, a Servlet container can be embedded to create a self-contained runnable application. The framework does not use any advanced Java EE technologies beyond basic Servlet API and will run under a simplest Servlet container configuration. It is designed against the Servlet API specification version 3.1, but will run under a 3.0 implementation as well. It won't run under earlier versions, however, since it uses asynchronous request processing, which is new in Servlet API specification 3.0.

The only third-party library dependencies for the framework are: an implementation of [JSON Processing API](https://json-processing-spec.java.net/), such as the reference implementation provided by [Glassfish project](https://jsonp.java.net/), an implementation of [Java Bean Validation](http://beanvalidation.org/), such as the one provided by [Hibernate project](http://hibernate.org/validator/), and [Apache Commons Logging](http://commons.apache.org/proper/commons-logging/), which is used for debug logging.

The framework provides its own persistent storage API to the applications. The API is not specific to any particular storage technology. However, out of the box, the framework includes an RDBMS storage implementation based on JDBC, which does not require any additional third-party dependencies (besides the JDBC driver).

The RDBMS-based persistent storage service included in the framework focuses on maximum efficiency (minimum number of queries required to achieve complex results), transparency (commands are sent to the database when the application issues them) and respect for RDBMS-specific features. SQL databases are a very mature technology and if for your application you are choosing to use one, it is probably because you want features like transactions, data constraints, such as foreign keys and unique constraints, reasonably normalized data structure that can be easily queried using SQL.

There are several important differences between X2 persistence API and JPA (Hibernate, etc.). The X2 persistence API is not an ORM. It does not manage your persistent entities' state, it does not operate with notions of persistent and detached states, it does not do lazy loading, it does not handle session entity caching. When you ask the framework to load a persistent record into a Java object, it loads it. When you ask it to save it, it saves it. Although it allows you to send specific queries to the database, it is not a mere facade for JDBC either. In the most cases the framework can build queries for your application automatically. The language of the queries does not have to be SQL if you are not using a SQL database. The framework's analog of JPA's entity beans&mdash;application persistent resources&mdash;do not define relationships between each other in RDBMS terms. A simpler, more business logic oriented way is utilized for that.

The way how relationships are defined between application's persistent resources allows creating quite complex data hierarchies. What is more important though, is that on the other end the framework allows creating REST API calls that can fetch various complex combinations of related data all in a transactional way. Also, efficient calculation of deep "ETag" and "Last-Modified" HTTP response headers is provided and support for conditional HTTP requests is fully implemented and expected to be used.

For REST API caller authentication the framework uses its own HTTP authentication scheme called "AuthToken". Each authenticated HTTP request contains an encrypted token in the "Authorization" header. The token is decrypted on the server side and is used to associate the request with an actor. The response contains a new token in the "Authentication-Info" header that can be used to make next request. The framework does not use Servlet API's HTTP session tracking feature and in a multiple server setup does not require any special configuration for session tracking such as "sticky" sessions. This allows server instances to be added and removed from the pool transparently without worries about breaking any existing sessions.

## Application and Framework Components

Below is a simplified diagram that shows components of an X2-based application.

[[File:Thymes2-components-diagram.svg|center]]

The HTTP server is provided by the Servlet container. The database is used for storing persistent data. And the application may also be connected to other external services. Here is a brief description of each component:

### Dispatcher Servlet and Endpoint Mappings

Dispatcher Servlet is a Servlet provided by the framework that receives all requests to the application's REST API. The main purpose of the Servlet is to find an application provided handler for the request method and URI, create an asynchronous execution context and submit the request for execution by the handler. Before the Dispatcher Servlet can be used, it must be mapped to a URI pattern in the web-application's deployment descriptor "web.xml". Usually, a prefix-based mapping is used. For example:

```xml
...

<servlet>
    <servlet-name>DispatcherServlet</servlet-name>
    <servlet-class>org.bsworks.x2.core.DispatcherServlet</servlet-class>
    <load-on-startup>1</load-on-startup>
    <async-supported>true</async-supported>
</servlet>
<servlet-mapping>
    <servlet-name>DispatcherServlet</servlet-name>
    <url-pattern>/api/*</url-pattern>
</servlet-mapping>

...
```

Note, that the Servlet must be marked as supporting asynchronous request processing. It is also recommended to load the Servlet on startup so that if there are any problems with its configuration it can be reported right away.

The second part of the Dispatcher Servlet configuration is specification of the REST API endpoint mappings. Each mapping associates a certain request URI pattern and HTTP method with a handler, called Endpoint Call Handler, which is an application custom component. The URI pattern is a regular expression and may contain groups. Each group in the URI regular expression is a URI parameter placeholder. Dispatcher Servlet extracts values of the groups and passes them to the handler. These URI parameters may be used similarly to request parameters provided in the request query string.

The endpoint mappings are specified using a web-application context initialization parameter called "x2.app.endpoints". Endpoint handlers and mappings are discussed in detail later, but, as an illustration, here is an example of how the mappings may look:

```xml
...

<context-param>
    <param-name>x2.app.endpoints</param-name>
    <param-value>
        /api/login
            GET org.bsworks.x2.toolbox.handlers.PasswordLoginEndpointCallHandler
        /api/accounts(?:/([1-9][0-9]*))?
            com.example.resources.Account
        /api/accounts/([1-9][0-9]*)/orders(?:/([1-9][0-9]*))?
            com.examples.handlers.OrdersEndpointHandler
    </param-value>
</context-param>

...
```

Note how some of the groups in the regular expressions are non-capturing. Only capturing groups are used as URI parameter placeholders.

When the framework was designed, it was tempting to use Java annotations on the handler classes to map then to request URIs, as many other contemporary frameworks do. But, stemming from the practicality principle, a decision was made that it is beneficial to have all mappings in one place, so that the application's API can be seen as a whole without going through multiple files.

### Actor Authentication Service

Often, application REST API calls must be associated with a certain actor. "Actor" is a term analogous to "user", but a decision was made to use it instead of "user" to reflect the fact that an actor is not necessarily a human individual. It, for example, mat be an external process, an application, etc.

The actor authentication service provides the framework with actor records, that can be looked up by the actor username. By default, a "dummy" actor authentication service implementation is used that does not contain any actor records. It makes it usable only if all requests that the application accepts are unauthenticated. In any other case, a real actor authentication service needs to be configured. The service implementation can be specified using web-application context initialization parameter called "x2.service.auth.provider". Its value is a fully qualified name of the service provider class. The framework includes some generic implementations that can be used directly, extended by the application, or the application can provide its own implementation of the service.

The framework uses an HTTP authentication scheme called "AuthToken". Every actor record provided by the actor authentication service, besides the username, also contains a secret key and credentials. The secret key is used for symmetric encryption of the authentication token, and the credentials are included in the token value along with other information, such as the token creation timestamp. Normally, the secret key is generated by the application and is not known to the actor. Credentials are known to the actor.

Here is an example of an authenticated HTTP request:

```http
GET /api/accounts/123 HTTP/1.1
Authorization: AuthToken username=user@example.com,token=jaoGoxPoXt62MQuDbnA81iS8qtqLOuQAJQhocNeEHm0=
...
```

If authentication is successful, the response will contain token value for the next request:

```http
HTTP/1.1 200 OK
Authentication-Info: nexttoken=+QEvFqtxl1onu6wR5PD+bSS8qtqLOuQAJQhocNeEHm0=
...
```

A token is valid only for a certain time period since its issue. The default is 30 minutes, but it can be configured using web-application context initialization parameter called "x2.auth.tokenTTL", which specifies the token expiration period in milliseconds.

Since the framework does not use any HTTP session information kept in memory, each authenticated call to the application's REST API may lead to an actor record lookup in the actor authentication service. If the service fetches the records from an external system, such as a database or an LDAP directory, it is potentially a costly operation and a significant overhead for each request. To minimize this overhead, X2 uses a simple per-instance actor record caching on top of the actor authentication service. After the service returns an actor record, the record is kept in the cache associated with the actor username and is considered valid for a certain time (default is 5 seconds). If a request is made after this period, the record is still used, so that the request processing is not delayed, but an actor record refresh is submitted for asynchronous execution in the background. However, after a longer period of time (default is 1 minute) the record is considered expired, a new actor record lookup is performed synchronously and only after that the request processing is continued. This logic makes execution of a series of subsequent requests from the same actor possible with minimum actor record lookup overhead.

The actor record cache timeouts can be customized using web-application context initialization parameters: "x2.auth.cache.refreshAfter" specifies number of milliseconds after which an asynchronous cached record refresh is requested, and "x2.auth.cache.discardAfter" specified number of milliseconds after which a cached actor record is considered stale.

The size of the cache is also configurable: "x2.auth.cache.maxSize" web-application context initialization parameter specified its limit. The default is 256 cached actor records.

To establish the initial authentication token, the application may provide an endpoint handler that takes, for example, username and password as request parameters, authenticates the actor and returns the token in "Authentication-Info" HTTP response header. In fact, the framework includes such handler implementation. See `org.bsworks.x2.toolbox.handlers.PasswordLoginEndpointCallHandler` class.

### Application Resources

Application resources represent the data, with which the application operates. It is often the centerpiece of the application. If a back-end application were paralleled with the MVC concept, the application resources would be the models. Application resources is the data that is exchanged between the back-end application and the client via the back-end application's REST API. Using the REST API, the client can query the resources, get various levels of the resource details, submit resources to the back-end.

In X2, application resources are represented by Java beans, a.k.a. POJOs, with annotated properties. A given resource class can contain several types of properties:

* Simple properties, such as strings, numbers, enumerations, Boolean values and dates.
* Nested objects, which are also Java beans and contain resource properties.
* References to other resources, represented by values of `org.bsworks.x2.resource.Ref`. These allow creating complex networks of related resources.
* Dependent resource references, which are also values of `org.bsworks.x2.resource.Ref` that point to resources that contain a reference property pointing back at the parent resource. Such resources are called dependent, because they can exist only in a context of their parent resource (the parent resource reference must point to an existing parent resource instance).

Any property can be single-valued, or it can be a collection, represented by a `java.util.Set`, `java.util.List` or a `java.util.Map` (only simple values and references can be used as map keys).

Application resource properties can also use Java Bean Validation constraints. The framework automatically validates resources when it receives them from the client via the application's REST API before handing them over to the application custom code for processing.

There are two types of resources: transient and persistent. Persistent resource instances are stored in the application's persistent storage (the database). Such stored resource instances are called records. Each record must have a unique identifier. Also, a reference can be created pointing to a persistent resource record. A reference is basically a combination of the persistent resource type and the record id.

Note, that to create relationships between application resources, X2 does not use any type of proxy objects and/or lazy loading. References are defined explicitly by using `org.bsworks.x2.resource.Ref` properties and values. This stems from the lightweightness principle. To work with a reference, the framework does not need to load the target resource record. It only needs its type and record id. When the application needs the target resource record, it can load it explicitly using the reference.

In addition to regular resource properties and record id, persistent resources may include special meta-properties automatically managed by the framework. These include:

* Record version number. It is used for calculating "ETag" values as well modification conflicts detection.
* Record creation timestamp and actor username.
* Record last modification timestamp and actor username. The last modification timestamp is used for calculating "Last-Modified" values.

It is highly recommended to use at least record version numbers, because it enables the HTTP conditional requests, which may dramatically improve application performance.

Transient resources are discovered by the framework at runtime as they are first used by the application. Persistent resources, however, need to be all pre-loaded, which means that the framework needs to be pointed at where to look for them during the application initialization. This is done by defining web-application context initialization parameter called "x2.app.persistentResources.packages", which is a whitespace-separated list of packages that contain persistent resource classes. Persistent resource classes are annotated with `@PersistentResource` annotations and thus discoverable by the framework.

Due to specifics of Java class loading, by default, the framework only looks in the web-application's "/WEB-INF/classes" for the persistent resource packages. Scanning jars for packages is an expensive operation, so to shorten the application start-up time, the jars in "/WEB-INF/lib" are not scanned. To include certain jars in the persistent resource discovery process, a regular expression pattern matching jar file names can be specified as a web-application context initialization parameter called "x2.app.persistentResources.jarsPattern".

### Application Resources Manager

Application resource manager, represented by the `org.bsworks.x2.resource.Resources` interface, is a core framework component responsible for maintaining application resource definitions. This component is not customizable. It is an integral part of the framework.

### Resource Serialization Service

Resource serialization service is used to serialize/deserialize application resources to represent them in HTTP request and response bodies. By default, the framework uses a serializer that represents resources as JSON. To override the default serializer, web-application context initialization parameter called "x2.service.serialization.provider" can be used, which specifies fully qualified name of the service provider class.

### Endpoint Handlers

As was mentioned earlier, endpoint handlers are custom application components responsible for processing the application REST API calls. Normally, this is where the bulk of the custom application logic is located. If an analogy is made with the MVC concept, where application resources are the models, endpoint handlers would be the controllers.

An endpoint call handler, an implementation of the `org.bsworks.x2.EndpointCallHandler` interface, is associated with a certain combination of an HTTP request method ("GET", "POST", "PUT" or "DELETE") and request URI pattern. Dispatcher Servlet and the Endpoint Mappings are responsible for that association. The Dispatcher Servlet is an asynchronous Servlet and it does not use the container's request processing thread to execute the endpoint call handler's logic. Instead, the handler is executed in a thread pool maintained by the framework. One of the two thread pools may be used to execute a handler: regular call processing pool and "long jobs" pool. The regular pool is used for handlers that view an endpoint call as a single transaction spanning from the client submitting the request and getting the response. A handler, however, can declare itself to be a "long job". It is usually used to execute multi-transactional tasks that need to be performed synchronously, that is the response is not returned to the client until the job is completed. Such handlers are executed by a separate thread pool with threads configured to have a notch lower priority than those in the regular pool. The number of threads in the regular pool can be customized using "x2.threads.EndpointCallProcessors" web-application context initialization parameter. The default is 2 threads. For the "long jobs" pool, it's "x2.threads.JobRunners" parameter and the default is 1, that is a single low-priority thread. The handlers also have timeouts used to limit the call processing times. For the regular calls the default timeout is 10 seconds and can be overridden via "x2.regularCallTimeout" context initialization parameter, which specifies the timeout in milliseconds. For the "long job" calls the default timeout is 1 minute and can be overridden via "x2.longJobCallTimeout" context initialization parameter, which also specifies the timeout in milliseconds. If a handler does not finish processing a call within the timeout period, the client receives an HTTP 408 (Request Timeout) response and the framework attempts to abort the handler.

When the framework invokes a handler to process a call, it passes endpoint call context (see `org.bsworks.x2.EndpointCallContext`) to it along with an application resource instance deserialized by the application resource serialization service from the HTTP request entity, if any. The endpoint call context contains all the call-specific information and provides the main point for the handler to communicate with the framework API. Through the endpoint call context, the handler can get access to the application runtime context (see `org.bsworks.x2.RuntimeContext`), which is the framework's API for the custom application that is not specific to any particular endpoint call. The endpoint call context contains may convenience shortcut methods that call the runtime context.

An endpoint handler mapping can be created in several ways. The simplest is to directly associate an HTTP request URI pattern and method with a class implementing `org.bsworks.x2.EndpointCallHandler` interface. This mapping method looks like this in the "x2.app.endpoints" web-application context initialization parameter:

```xml
<context-param>
    <param-name>x2.app.endpoints</param-name>
    <param-value>
        ...
        /api/myresource
            GET com.example.handlers.MyResourceGetEndpointCallHandler
        ...
    </param-value>
</context-param>
```

The framework, however, provides a complete set of default handler implementations for the four HTTP methods when the resource is a persistent resource. These default implementations deal with the generic HTTP logic, including, for example, conditional HTTP requests. To separate HTTP logic from the business logic, an application can use an implementation of `org.bsworks.x2.app.PersistentResourceEndpointHandler`, which encapsulates the resource handling business logic for all the HTTP methods in one place. The resource handler is responsible for providing the framework with endpoint call handlers for the HTTP methods and the framework accepts only those methods, for which the resource handler has provided an endpoint call handler. There is a default implementation, `org.bsworks.x2.app.DefaultPersistentResourceEndpointHandler`, which can be extended by the application instead of creating completely its own implementation of `org.bsworks.x2.app.PersistentResourceEndpointHandler`. The default resource handler provides default endpoint call handlers for all HTTP methods. To map such handler, there is no need to specify the HTTP method. Only the request URI pattern is used:

```xml
<context-param>
    <param-name>x2.app.endpoints</param-name>
    <param-value>
        ...
        /api/myresources(?:/(\d+))?
            com.example.handlers.MyResourceEndpointHandler
        ...
    </param-value>
</context-param>
```

If the application does not need to extend the default persistent resource handler logic, there is an even simpler way to create a mapping for a persistent resource:

```xml
<context-param>
    <param-name>x2.app.endpoints</param-name>
    <param-value>
        ...
        /api/myresources(?:/(\d+))?
            com.example.resources.MyResource
        ...
    </param-value>
</context-param>
```

Where `com.example.resources.MyResource` is the persistent resource class.

### Resource Persistence Service

The persistence service is responsible for loading and storing persistent application resource records from and to the persistent storage (the database). The persistent resource records are stored in *persistent collections*. The exact nature of a persistent collection depends on the underlying database implementation. For example, in the case of an RDBMS, it is a table or, to be more precise, a set of related tables used to store the persistent resource properties with one table being the root persistent resource records table.

By default, a "dummy" persistence service is configured that does not actually persist anything and throws an error if the application attempts to use it. This can be used only for simple applications that do not use any persistent resources. Otherwise, a real persistence service must be configured using "x2.service.persistence.provider" web-application context initialization parameter, which specifies fully qualified name of the service provider class.

The persistence service works with the concept of a transaction. All persistent operations are performed in a context of a transaction and the transaction object provides API to the application custom code for all the persistence operations. Using the transaction, the application can query collections of persistent resource records, load the records in various combinations, create new records, update existing records and delete existing records. Low-level access to the underlying database technology is also made available to the application.

The framework includes a persistence service implementation for RDBMS, which uses JDBC. The service provider is `org.bsworks.x2.services.persistence.impl.jdbc.JDBCPersistenceServiceProvider`. At the moment, [MySQL](http://www.mysql.com/) (including [MariaDB](https://mariadb.org/)) and [PostgreSQL](http://www.postgresql.org/) are supported.

### Persistent Resource Collections Versioning Service

The framework utilizes an efficient way to track changes to persistent resource collections as a whole. For example, when a persistent resource record of a certain type is created, deleted or updated, the whole collection of records of that persistent resource type is considered to be modified. The persistent resource collections versioning service keeps track of these modifications. The information about collection version number and last modification timestamp is used in calculating "ETag" and "Last-Modified" values for the application's REST API responses that include data from those collections not identified by particular record ids. This allows support for conditional HTTP requests without need of querying the actual records data, which is often significantly more costly.

By default, a very simple implementation of the versioning service is configured. It keeps the versioning information in memory and is suitable only for application development or very simple single-instance setups. Most applications need to configure a more sophisticated versioning service implementation using "x2.service.versioning.provider" web-application context initialization parameter, which specifies a fully qualified name of the service provider class. The framework includes an implementation, provided by `org.bsworks.x2.services.versioning.impl.rdbms.RDBMSPersistentResourceVersioningServiceProvider`, that uses an RDMBS table to maintain versioning information for every persistent application resource collection.

### Internal Monitor Service

Internal monitor service performs two tasks: it collects, aggregates and stores statistics about various internal application events, such as REST API calls, persistent storage queries, etc., and also registers all unexpected application errors. Whenever an error happens that leads to sending an HTTP 500 (Internal Server Error) response back to the client, the error is first submitted to the monitor service together with information about the context, in which the error took place. The monitor service then stores the error information for further investigation. By default, a "dummy" monitor service is configured, which does not store any information. The errors are simply logged by the framework in the application debug log. A real monitor service implementation can be configured using "x2.service.monitor.provider" web-application context initialization parameter, which specifies a fully qualified name of the service provider class.

### Application Services

The services are application components that provide specific functionality to the rest of the application. There are several service that are used by the framework itself. These are called *essential services*, marked with `org.bsworks.x2.EssentialService` interface, and include implementations of the following service interfaces:

* `org.bsworks.x2.services.auth.ActorAuthenticationService`
* `org.bsworks.x2.services.monitor.MonitorService`
* `org.bsworks.x2.services.persistence.PersistenceService`
* `org.bsworks.x2.services.versioning.PersistentResourceVersioningService`
* `org.bsworks.x2.services.serialization.ResourceSerializationService`

In addition to these essential services, an application can define a set of its own services. To do that, a `org.bsworks.x2.ServiceProvider` implementation needs to be created and registered using "x2.app.serviceProviders" web-application context initialization parameter, which is a whitespace-separated list of fully qualified service provider implementation class names. Such additional application service is made available to the application via the runtime context, using which the application can get a reference to the service instance by the service interface.

Sometimes, a single service interface may have several service instances, which may be different implementations of the interface, or have different configuration. In that case, each instance can be assigned an id (specified after a colon following the service provider class name in the "x2.app.serviceProviders" context initialization parameter). The service instance then can be requested from the runtime context using the service interface and the service instance id.

## Simple Application Tutorial

As a tutorial, let's develop back-end application for a simple Internet store. We have products that we offer and we have customers who can browse the products, register accounts and place and review orders.

### Project Setup

An X2-based application is a Java web-application running under a Servlet container. The framework itself depends on several libraries that are usually not packaged with simple Servlet containers and therefore must be included in the web-application. These include:

* JSON Processing API (JSR-353) and implementation.
* Java Bean Validation API and implementation.
* Apache Commons Logging.

The easiest way to setup a project is to use [Apache Maven](http://maven.apache.org/). We are going to need the following elements in the "pom.xml":

```xml
...

<repositories>
    <repository>
        <id>boylesoftware-os</id>
        <url>http://www.boylesoftware.com/maven/repo-os</url>
    </repository>
</repositories>

...

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-war-plugin</artifactId>
            <version>2.4</version>
            <configuration>
                <archive>
                    <manifest>
                        <addDefaultImplementationEntries>true</addDefaultImplementationEntries>
                    </manifest>
                </archive>
            </configuration>
        </plugin>
    </plugins>
</build>

...

<dependencies>
    <dependency>
        <groupId>org.bsworks.x2</groupId>
        <artifactId>thymes2</artifactId>
        <version>1.0.0</version>
    </dependency>
    <dependency>
        <groupId>javax.servlet</groupId>
        <artifactId>javax.servlet-api</artifactId>
        <version>3.1.0</version>
        <scope>provided</scope>
    </dependency>
    <dependency>
        <groupId>org.glassfish</groupId>
        <artifactId>javax.json</artifactId>
        <version>1.0.4</version>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>org.hibernate</groupId>
        <artifactId>hibernate-validator</artifactId>
        <version>5.1.2.Final</version>
        <scope>runtime</scope>
    </dependency>
</dependencies>

...
```

We've added a repository for the framework, we've added the framework dependency, the Servlet API and implementations for the JSON Processing and Bean Validation. Another important part is that we tell Maven to generate MANIFEST.MF and include implementation entries in it. The framework uses "Implementation-Version" element in the MANIFEST.MF to include application version in the calculated "ETags", so that if different versions of the application have different resources, stale resources do not stay in the client caches.

Now we must create web-application deployment descriptor "web.xml". In it, we must create a mapping for the framework's Dispatcher Servlet:

```xml
...

<servlet>
    <servlet-name>DispatcherServlet</servlet-name>
    <servlet-class>org.bsworks.x2.core.DispatcherServlet</servlet-class>
    <load-on-startup>1</load-on-startup>
    <async-supported>true</async-supported>
</servlet>
<servlet-mapping>
    <servlet-name>DispatcherServlet</servlet-name>
    <url-pattern>/api/*</url-pattern>
</servlet-mapping>

...
```

During application development it is often useful to see what it going on during request processing. X2 framework writes a detailed debug log for that purpose. You can configure your runtime environment and enable debug log for channel "org.bsworks.x2".

### First Persistent Application Resource

We start our application development from definition of the persistent application resources. For our purpose, we are going to have three such resources: one to represent products, second to represent customer accounts, and third to represent orders. Let's start with the product:

```java
package org.bsworks.x2sample.resources;

import java.math.BigDecimal;

import org.bsworks.x2.resource.IdHandling;
import org.bsworks.x2.resource.MetaPropertyType;
import org.bsworks.x2.resource.annotations.*;


@PersistentResource
public class Product {

    @IdProperty(handling=IdHandling.AUTO_GENERATED)
    private Integer id;

    @MetaProperty(type=MetaPropertyType.VERSION)
    private int version;

    @Property
    private String title;

    @Property
    private BigDecimal price;


    public Integer getId() { return this.id; }
    public void setId(Integer id) { this.id = id; }

    public int getVersion() { return this.version; }
    public void setVersion(int version) { this.version = version; }

    public String getTitle() { return this.title; }
    public void setTitle(String title) { this.title = title; }

    public BigDecimal getPrice() { return this.price; }
    public void setPrice(BigDecimal price) { this.price = price; }
}
```

There are several things to note about this class. It is a POJO. The class is annotated with `@PersistentResource`, so the framework can discover it and register as a persistent application resource. It has a record id property annotated with `@IdProperty`. The id handling attribute tells that the id is going to be automatically generated for new records by the database. The resource also has a record version meta-property annotated with `@MetaProperty`. The framework is going to use it for the "ETags" associated with the product records. The two data properties&mdash;the product title and the price&mdash;are annotated with `@Property`. The framework does not consider a Java bean property as an application resource property if it is not annotated with `@Property`.

For the framework to discover our new persistent application resource, we must point it to the Java package that contains the resources. To do that, we must add a context parameter to our application's "web.xml" file:

```xml
...

<context-param>
    <param-name>x2.app.persistentResources.packages</param-name>
    <param-value>org.bsworks.x2sample.resources</param-value>
</context-param>

...
```

Now we have to take care of the persistent resource storage. Let's use a MySQL database for that. There are several setup steps that we must undertake. First, we must create the database. Let's assume we have it. Now, we need to configure the framework to use a JDBC persistence service. We also need to add the database data source as the JNDI resource. To do these two things, we add to the "web.xml":

```xml
...

<context-param>
    <param-name>x2.service.persistence.provider</param-name>
    <param-value>org.bsworks.x2.services.persistence.impl.jdbc.JDBCPersistenceServiceProvider</param-value>
</context-param>

...

<resource-ref>
    <res-ref-name>jdbc/ds</res-ref-name>
    <res-type>javax.sql.DataSource</res-type>
    <res-auth>Container</res-auth>
    <res-sharing-scope>Shareable</res-sharing-scope>
</resource-ref>

...
```

Naturally, our Servlet contained must define datasource for "java:comp/env/jdbc/ds" JNDI name.

We need to create a table for storing our products:

```sql
CREATE TABLE product (
    id INT PRIMARY KEY AUTO_INCREMENT,
    version INT NOT NULL,
    title VARCHAR(50) NOT NULL UNIQUE,
    price DECIMAL(5,2) NOT NULL
)
```

Now, we need to go back to our product resource and make some adjustments. First, even though the class is marked as a persistent resource, its properties are by default transient unless explicitly marked as persistent. Second, we need to associate the resource with the table and the properties with the table columns. Technically, in our particular case, we don't have to do that, because by default the class name and the property names are automatically used for the table and column names respectively. But to demonstrate how to do it, and in general to recommend it as a good practice, let's explicitly set the names. Our class now will look like this:

```java
...

@PersistentResource(persistentCollection="product")
public class Product {

    @IdProperty(handling=IdHandling.AUTO_GENERATED, persistentField="id")
    private Integer id;

    @MetaProperty(type=MetaPropertyType.VERSION, persistentField="version")
    private int version;

    @Property(persistence=@Persistence(field="title"))
    private String title;

    @Property(persistence=@Persistence(field="price"))
    private BigDecimal price;


    // getters and setters
    ...
}
```

### First Endpoint Mapping

Now that we have a persistent resource, we can create an endpoint mapping for it, start our application and see how it works. To create a basic mapping without any additional handler logic we add the following to the "web.xml":

```xml
...

<context-param>
    <param-name>x2.app.endpoints</param-name>
    <param-value>
        /api/products(?:/([1-9][0-9]*))?
            org.bsworks.x2sample.resources.Product
    </param-value>
</context-param>

...
```

This defines an endpoint URI with one optional URI parameter placeholder at the end for the product record id. An HTTP "GET" method can be used with a URI with or without the id, "POST" can be used without the id, and "PUT" and "DELETE" will require an id in the URI.

Let's also create a product record in the database, so that we have some test data:

```sql
INSERT INTO product (version, title, price) VALUES (1, 'Test', 5.99);
```

Now we can start the application and send some requests to it:

```http
GET /api/products HTTP/1.1
...
```

The meaning of this request is to get the whole products collection. In our case, the response will be:

```http
HTTP/1.1 200 OK
ETag: "20141018122610-SNAPSHOT-1413649570823"
Last-Modified: Sat, 18 Oct 2014 16:31:47 GMT
Content-Type: application/json;charset=UTF-8
...

{
    "records": [
        {
            "id": 1,
            "version": 1,
            "title": "Test",
            "price": 5.99
        }
    ],
    "refs": null,
    "totalCount": -1
}
```

In the response headers we see "ETag" and "Last-Modified". Since the request did not address any particular record in the collection, the values of these two headers are computed solely based on the products collection versioning information provided by the persistent resource collections versioning service (the memory-base one in our case, since we have not configured a different implementation). So, the "Last-Modified" contains the timestamp of the last modification of the products collection as a whole, and the "ETag" is composed of two parts: the application version and the products collection version. Since our application is running in development mode, the application version is based on the instance startup timestamp. If we ran the application in live mode it would be the version value from the MANIFEST.MF file. The collection version also looks like a timestamp and that's because we are not using a real versioning service, but a memory-based one, which indeed uses instance startup time as the base for counting collection updates. All of this works well during development, so that we do not have stale cached responses on the client side. In live mode and with a real versioning service implementation, the "ETag" values are less volatile.

Now, let's look at the response entity. First of all, the entity is in JSON format. This is because we are using a JSON resource serialization service, configured by default. Second, instead of returning a JSON array with the product records, the result is wrapped in a single result JSON object. It includes three fields: "records" is the array with the records, "refs" would contain a map with fetched referred resource records if we requested it (discussed later), and "totalCount" contains the total number of records if the request was ranged (also discussed slightly later), which, since the request is not ranged, is unused and the total count of records is the number of elements in the "records" array.

To make a ranged request, for example for displaying the result in pages, we add a request parameter:

```http
GET /api/products?r=0,10 HTTP/1.1
...
```

We've asked to fetch up to 10 records starting from the beginning of the collection and the response is:

```http
HTTP/1.1 200 OK
ETag: "20141018122610-SNAPSHOT-1413649570823"
Last-Modified: Sat, 18 Oct 2014 16:31:47 GMT
Content-Type: application/json;charset=UTF-8
...

{
    "records": [
        {
            "id": 1,
            "version": 1,
            "title": "Test",
            "price": 5.99
        }
    ],
    "refs": null,
    "totalCount": 1
}
```

It was tempting to use HTTP "Range" header to request ranged collection results. However, since in that case the range specification is not part of the request URL, it introduces some complications in the "ETag" and response caching logic. Besides, the collection result needed to be wrapped in a result object anyway to support fetching referred resource records (the "refs" property) and returning the whole result in a single response, so that it is transactional. The practicality principle kicks in here and a decision is made not to use "Range" headers and include range specification as a request parameter in the URL.

The collection search request demonstrated above supports a number of other request parameters, which allow filtering the result, ordering it, including/excluding certain resource properties, fetching other resource records referred by the resource reference properties, and, as shown above, selecting only a specific range of records. For example, to find all products that have title that starts with "test" (case-insensitive) and price less than 6.00 and sort the result by title, we could send this request:

```http
GET /api/products?f=title^test&f=price<6&o=title HTTP/1.1
...
```

Look in the reference documentation for `org.bsworks.x2.app.DefaultGetPersistentResourceEndpointCallHandler` for the complete information about supported parameters. This is the default endpoint handler for "GET" requests provided by the framework and it is where this logic is implemented.

If we want to get just a single record given its id, the request is:

```http
GET /api/products/1 HTTP/1.1
...
```

And the response is:

```http
HTTP/1.1 200 OK
ETag: "20141018132322-SNAPSHOT-1"
Content-Type: application/json;charset=UTF-8
...

{
    "id": 1,
    "version": 1,
    "title": "Test",
    "price": 5.99
}
```

No response wrapped object in this case and in the "ETag" in addition to the application version the product record version is used. There is no "Last-Modified" header in the response because our persistent resource does not define last modification timestamp meta-property. If it had it, the response would include the header in addition to the "ETag".

What about creating a new product record via our application API? Let's try it:

```http
POST /api/products HTTP/1.1
Content-Type: application/json
...

{
    "title": "Another",
    "price": 10.99
}
```

The response is an error:

```http
HTTP/1.1 401 Unauthorized
...

{
    "errorMessage": "Authentication required.",
    "errorDetails": null
}
```

Our requests so far have been unauthenticated and the framework does not allow any unauthenticated call that modifies the data in the persistent storage. We need to take care of user accounts now.

### User Accounts

For our sample application, user accounts are going to be persistent resource records in the database and we are going to have two types of users: customers and admins. In regard to the products collection, customers will be able to see the products, but will not be allowed to modify them. Admins, on the other hand, will have all the permissions associated with customers plus they will be allowed to create, modify and delete products. Let's start defining our user account persistent application resource.

First of all, we know by now that we are going to have several persistent resources in our application and that they all are going to have something in common&mdash;an auto-generated numeric record id, record version, and other meta-properties. Thus, it makes sense to create an abstract common parent for our persistent application resources:

```java
package org.bsworks.x2sample.resources;

import java.util.Date;

import org.bsworks.x2.resource.IdHandling;
import org.bsworks.x2.resource.MetaPropertyType;
import org.bsworks.x2.resource.annotations.IdProperty;
import org.bsworks.x2.resource.annotations.MetaProperty;


public abstract class AbstractPersistentResource {

    @IdProperty(handling=IdHandling.AUTO_GENERATED, persistentField="id")
    private Integer id;

    @MetaProperty(type=MetaPropertyType.VERSION, persistentField="version")
    private int version;

    @MetaProperty(type=MetaPropertyType.CREATION_TIMESTAMP, persistentField="created_on")
    private Date createdOn;

    @MetaProperty(type=MetaPropertyType.CREATION_ACTOR, persistentField="created_by")
    private String createdBy;

    @MetaProperty(type=MetaPropertyType.MODIFICATION_TIMESTAMP, persistentField="modified_on")
    private Date lastModifiedOn;

    @MetaProperty(type=MetaPropertyType.MODIFICATION_ACTOR, persistentField="modified_by")
    private String lastModifiedBy;


    // getters and setters
    ...
}
```

Now we can subclass our already existing product resource:

```java
...

@PersistentResource(persistentCollection="product")
public class Product extends AbstractPersistentResource {

    @Property(persistence=@Persistence(field="title"))
    private String title;

    @Property(persistence=@Persistence(field="price"))
    private BigDecimal price;


    // getters and setters
    ...
}
```

And add the rest of the meta-properties to the product table in the database:

```sql
ALTER TABLE product ADD (
    created_on TIMESTAMP NOT NULL,
    created_by VARCHAR(50) NOT NULL,
    modified_on TIMESTAMP NOT NULL,
    modified_by VARCHAR(50) NOT NULL
);

UPDATE product SET created_on = CURRENT_TIMESTAMP, modified_on = CURRENT_TIMESTAMP,
    created_by = 'admin', modified_by = 'admin';
```

Now, the user account persistent resource that uses user e-mail address as the username:

```java
package org.bsworks.x2sample.resources;

import org.bsworks.x2.resource.annotations.*;


@PersistentResource(persistentCollection="account")
public class Account extends AbstractPersistentResource {

    @Property(persistence=@Persistence(field="email"))
    private String email;

    @Property(persistence=@Persistence(field="is_admin"))
    private boolean admin;

    @Property(persistence=@Persistence(field="fname"))
    private String firstName;

    @Property(persistence=@Persistence(field="lname"))
    private String lastName;


    // getters and setters
    ...
}
```

This is a very simple resource definition. Unfortunately, it lacks some important features. First, we are going to use this resource with the actor authentication service&mdash;the framework provided `org.bsworks.x2.services.auth.impl.prsrc.PersistentResourceActorAuthenticationService` in our case&mdash;and that means our account persistent resource must implement `org.bsworks.x2.Actor` interface. Second, at this point we have to start thinking about security and associate specific permissions with certain properties. Let's add all these features to our account resource and discuss it:

```java
...

@PersistentResource(persistentCollection="account")
public class Account extends AbstractPersistentResource implements Actor {

    @Property(persistence=@Persistence(field="email"))
    private String email;

    @Property(persistence=@Persistence(field="secret"),
        updateIfNull=false,
        accessRestrictions={
            @AccessRestriction(ResourcePropertyAccess.SEE),
            @AccessRestriction(ResourcePropertyAccess.SUBMIT)
        })
    private String secretKeyHex;

    @Property(
        updateIfNull=false,
        accessRestrictions={
            @AccessRestriction(ResourcePropertyAccess.SEE)
        })
    private String password;

    @Property(persistence=@Persistence(field="pwddigest"),
        updateIfNull=false,
        accessRestrictions={
            @AccessRestriction(ResourcePropertyAccess.SEE),
            @AccessRestriction(ResourcePropertyAccess.SUBMIT)
        })
    private String passwordDigestHex;

    @Property(persistence=@Persistence(field="is_admin"),
        accessRestrictions={
            @AccessRestriction(value=ResourcePropertyAccess.SUBMIT, allowTo={ "admin" }),
            @AccessRestriction(value=ResourcePropertyAccess.UPDATE, allowTo={ "admin" })
        })
    private boolean admin;

    @Property(persistence=@Persistence(field="fname"))
    private String firstName;

    @Property(persistence=@Persistence(field="lname"))
    private String lastName;


    @Override
    public String getUsername() { return this.email; }

    @Override
    public String getOpaque() { return null; }

    @Override
    public SecretKey getSecretKey() {

        return new SecretKeySpec(Hex.decode(this.secretKeyHex), "AES");
    }

    @Override
    public byte[] getCredentials() {

        return Hex.decode(this.passwordDigestHex);
    }

    @Override
    public boolean hasRole(final String role) {

        return ("admin".equals(role) && this.admin);
    }

    @Override
    public boolean hasAnyRole(Set<String> roles) {

        return (roles.contains("admin") && this.admin);
    }


    // getters and setters
    ...

    public void setPassword(final String password) {

        this.password = password;

        if (password == null) {
            this.passwordDigestHex = null;
        } else {
            try {
                this.passwordDigestHex = Hex.encode(
                    MessageDigest.getInstance("SHA-1").digest(
                        password.getBytes(Charset.forName("UTF-8"))));
            } catch (final NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
    }

    ...
}
```

The secret key will be assigned automatically by our custom account endpoint handler when a new account is created. Attribute `updateIfNull=false` allows us to ignore the property in any incoming JSON and never update the field in the database. The access restrictions exclude the property from any JSON sent to the client ("SEE"), and ignore the property in any incoming JSON ("SUBMIT"), so it is always `null` and therefore left untouched in the database. The secret key is used to encrypt the authentication token. In our case, we use AES encryption algorithm with a 128-bit key. In the database it is stored in hexadecimal encoding.

Password is a transient property. We don't store the password in the database. Instead, we store its SHA-1 digest, which is a persistent property that contains the digest in hexadecimal encoding. Both properties$mdash;the password and the digest&mdash;have `updateIfNull=false`, which allows updating other account properties without touching the password. Both have "SEE" access restriction, so they are never sent to the client via the API. The digest property has also "SUBMIT" restriction&mdash;to change the password, plain password property is submitted and the digest is automatically calculated in the password setter method.

The "admin" flag has access restrictions that allow its modification only if the actor submitting the request is an admin. When a new account is submitted for creation via the API and the API caller is not an admin, the "SUBMIT" restriction leaves the field "false" no matter what's in the incoming JSON, so that a non-admin user cannot create an admin.

Let's create an account table in the database, create two test accounts&mdash;one admin and one non-admin&mdash;and proceed with our application development:

```sql
CREATE TABLE account (
    id INT PRIMARY KEY AUTO_INCREMENT,
    version INT NOT NULL,
    created_on TIMESTAMP NOT NULL,
    created_by VARCHAR(50) NOT NULL,
    modified_on TIMESTAMP NOT NULL,
    modified_by VARCHAR(50) NOT NULL,
    email VARCHAR(50) NOT NULL UNIQUE,
    secret CHAR(32) NOT NULL,
    pwddigest CHAR(40) NOT NULL,
    is_admin BOOLEAN NOT NULL,
    fname VARCHAR(50) NOT NULL,
    lname VARCHAR(50) NOT NULL
);

INSERT INTO account (version, created_on, created_by, modified_on, modified_by,
    email, secret, pwddigest, is_admin,
    fname, lname)
VALUES (1, CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 'admin',
    'admin@example.com', '1234567890abcdef1234567890abcdef', '5baa61e4c9b93f3f0682250b6cf8331b7ee68fd8', TRUE,
    'Maria', 'Zimmer');

INSERT INTO account (version, created_on, created_by, modified_on, modified_by,
    email, secret, pwddigest, is_admin,
    fname, lname)
VALUES (1, CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 'admin',
    'customer@example.com', '1234567890abcdef1234567890abcdef', '5baa61e4c9b93f3f0682250b6cf8331b7ee68fd8', FALSE,
    'Carl', 'Becker');
```

The digests are for word "password" and the secret keys&hellip; are not really a secret. Our accounts endpoint handler will handle creation of real keys later when we develop it.

Three items remain: we need an enpoint mapping for the account management, and we need an endpoint mapping for user login, and we need to configure a persistent resource-based actor authentication service. For the account management let's use default endpoint handler for now (we will have to replace it with a bit more sophisticated version later) and for the user login we can use a handler from the framework's toolbox. In our "web.xml":

```xml
...

<context-param>
    <param-name>x2.service.auth.provider</param-name>
    <param-value>org.bsworks.x2.services.auth.impl.prsrc.PersistentResourceActorAuthenticationServiceProvider</param-value>
</context-param>
<context-param>
    <param-name>x2.service.auth.prsrc.actorResourceClass</param-name>
    <param-value>org.bsworks.x2sample.resources.Account</param-value>
</context-param>
<context-param>
    <param-name>x2.service.auth.prsrc.usernameProperty</param-name>
    <param-value>email</param-value>
</context-param>
<context-param>
    <param-name>x2.service.auth.prsrc.actorProperties</param-name>
    <param-value>secretKeyHex,passwordDigestHex,admin</param-value>
</context-param>
<context-param>
    <param-name>x2.service.auth.prsrc.passwordDigestAlg</param-name>
    <param-value>SHA-1</param-value>
</context-param>

...

<context-param>
    <param-name>x2.app.endpoints</param-name>
    <param-value>
        /api/login
            GET org.bsworks.x2.toolbox.handlers.PasswordLoginEndpointCallHandler
        /api/products(?:/([1-9][0-9]*))?
            org.bsworks.x2sample.resources.Product
        /api/accounts(?:/([1-9][0-9]*))?
            org.bsworks.x2sample.resources.Account
    </param-value>
</context-param>

...
```

Let's try to login:

```http
GET /api/login?username=admin@example.com&password=password HTTP/1.1
...
```

And the response should be:

```http
HTTP/1.1 200 OK
Authentication-Info: nexttoken=oQgFR0z4dnUCpKw2UYCeDsNnX+rzUD7pAlUWRweYX5U=
Content-Type: application/json;charset=UTF-8
...

{
    "id": 1,
    "version": 1,
    "createdOn": "2014-10-18T19:09:19Z",
    "createdBy": "admin",
    "lastModifiedOn": "2014-10-18T19:09:19Z",
    "lastModifiedBy": "admin",
    "email": "admin@example.com",
    "admin": false,
    "firstName": null,
    "lastName": null
}
```

In the "Authentication-Info" response header we have our authentication token to use and in the response body we have our user record. The first and last name properties are `null`, because they are not included in the "x2.service.auth.prsrc.actorProperties" web-application context initialization parameter. This record is loaded only for authentication purposes, so it needs to include only those properties that are immediately necessary for the authentication. If a complete account record is needed, it can to be loaded separately as a persistent resource record.

### Securing the Products and Validation

There are several problems at this point with our product management endpoint. First, any authenticated user can create, modify and update products. That includes both admins and customers, while only admins should be allowed to make changes. Let's close this hole:

```java
...

@PersistentResource(persistentCollection="product", accessRestrictions={
    @AccessRestriction(value=ResourcePropertyAccess.SUBMIT, allowTo={ "admin" }),
    @AccessRestriction(value=ResourcePropertyAccess.DELETE, allowTo={ "admin" })
})
public class Product extends AbstractPersistentResource {

    ...
}
```

This declaration allows submitting product records via the application API only to authenticated users that have role "admin". With this restriction non-admin users will not be able to create or update product records, because they won't be able to send them to the application. We also add a "DELETE" restriction to allow deleting products only to the admins.

Now, if we login as a customer and attempt to create a new product or update or delete an existing one via the REST API, we are going to get an HTTP 403 (Forbidden) error. But an admin will be able to create a product using a "POST" request:

```http
POST /api/products HTTP/1.1
Authorization: AuthToken username=admin@example.com,token=...
Content-Type: application/json
...

{
    "title": "Another",
    "price": 10.99
}
```

If all good, the response contains the new product record:

```http
HTTP/1.1 201 Created
Location: /sample/api/products/5
ETag: "20141018170407-SNAPSHOT-1"
Last-Modified: Sat, 18 Oct 2014 21:06:51 GMT
Authentication-Info: nexttoken=...
Content-Type: application/json;charset=UTF-8
...

{
    "id": 5,
    "version": 1,
    "createdOn": "2014-10-18T21:06:51Z",
    "createdBy": "admin@example.com",
    "lastModifiedOn": "2014-10-18T21:06:51Z",
    "lastModifiedBy": "admin@example.com",
    "title": "Another",
    "price": 10.99
}
```

To change the product price we can use a "PUT":

```http
PUT /api/products/5 HTTP/1.1
Authorization: AuthToken username=admin@example.com,token=...
Content-Type: application/json
...

{
    "id": 5,
    "version": 1,
    "createdOn": "2014-10-18T21:06:51Z",
    "createdBy": "admin@example.com",
    "lastModifiedOn": "2014-10-18T21:06:51Z",
    "lastModifiedBy": "admin@example.com",
    "title": "Another",
    "price": 3.99
}
```

The response contains updated meta-information:

```http
HTTP/1.1 204 No Content
ETag: "20141018170407-SNAPSHOT-2"
Last-Modified: Sat, 18 Oct 2014 21:29:21 GMT
Authentication-Info: nexttoken=...
...
```

And to delete the product we can use "DELETE" method:

```http
DELETE /api/products/5 HTTP/1.1
Authorization: AuthToken username=admin@example.com,token=...
...
```

With successful response:

```http
HTTP/1.1 204 No Content
Authentication-Info: nexttoken=...
...
```

Another remaining issue with our product management endpoint is that submitting product data that violates database constraints leads to an internal server error (try creating a product without title). The correct reaction would be responding with an HTTP 400 (Bad Request) response. To achieve that we can add Bean Validation constraints to the product resource:

```java
...

@PersistentResource(persistentCollection="product", accessRestrictions={
    @AccessRestriction(value=ResourcePropertyAccess.SUBMIT, allowTo={ "admin" }),
    @AccessRestriction(value=ResourcePropertyAccess.DELETE, allowTo={ "admin" })
})
public class Product extends AbstractPersistentResource {

    @Property(persistence=@Persistence(field="title"))
    @NotNull
    @Size(min=1, max=50)
    private String title;

    @Property(persistence=@Persistence(field="price"))
    @NotNull
    @Digits(integer=3, fraction=2)
    @DecimalMin(value="0.00")
    private BigDecimal price;


    // getters and setters
    ...
}
```

Now, if we attempt to create a product without a title:

```http
POST /api/products HTTP/1.1
Authorization: AuthToken username=admin@example.com,token=...
Content-Type: application/json
...

{
    "price": 10.99
}
```

We are going to get an error response like the following:

```http
HTTP/1.1 400 Bad Request
Content-Type: application/json;charset=UTF-8
...

{
    "errorMessage": "Invalid request entity.",
    "errorDetails": {
        "invalidProperties": {
            "title": "may not be null"
        }
    }
}
```

### Custom Endpoint Handlers

There is still a problem with our products endpoint. The product title is made unique in the database and we cannot verify a new or updated product title uniqueness via Bean Validation constraints. It requires a query to the database before proceeding with the record creation transaction. This is when we are no longer satisfied with the default endpoint handler functionality and we must extend it with our custom validation logic:

```java
package org.bsworks.x2sample.handlers;

import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;

import org.bsworks.x2.EndpointCallContext;
import org.bsworks.x2.EndpointCallErrorException;
import org.bsworks.x2.app.DefaultPersistentResourceEndpointHandler;
import org.bsworks.x2.resource.FilterConditionType;
import org.bsworks.x2.resource.Resources;
import org.bsworks.x2sample.resources.Product;


public class ProductsEndpointHandler extends DefaultPersistentResourceEndpointHandler<Product> {

    public ProductsEndpointHandler(@SuppressWarnings("unused") ServletContext sc,
            Resources resources) {
        super(resources.getPersistentResourceHandler(Product.class));
    }


    @Override
    public void create(EndpointCallContext ctx, Product recTmpl)
        throws EndpointCallErrorException {

        if (ctx.getPersistenceTransaction()
                .createPersistentResourceFetch(Product.class)
                .setFilter(ctx.getFilterSpec(Product.class)
                        .addTrueCondition("title", FilterConditionType.EQ, recTmpl.getTitle()))
                .getCount() > 0)
            throw new EndpointCallErrorException(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "A product with the same title already exists.");

        super.create(ctx, recTmpl);
    }

    @Override
    public Set<String> update(EndpointCallContext ctx, Product rec, Product recTmpl)
        throws EndpointCallErrorException {

        if (ctx.getPersistenceTransaction()
                .createPersistentResourceFetch(Product.class)
                .setFilter(ctx.getFilterSpec(Product.class)
                        .addTrueCondition("title", FilterConditionType.EQ, recTmpl.getTitle())
                        .addTrueCondition("id", FilterConditionType.NE, rec.getId()))
                .getCount() > 0)
            throw new EndpointCallErrorException(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Another product with the same title exists.");

        return super.update(ctx, rec, recTmpl);
    }
}
```

Now we need to replace the endpoint mapping in our "web.xml":

```xml
...

<context-param>
    <param-name>x2.app.endpoints</param-name>
    <param-value>
        ...
        /api/products(?:/([1-9][0-9]*))?
            org.bsworks.x2sample.handlers.ProductsEndpointHandler
        ...
    </param-value>
</context-param>

...
```

Note, that resource endpoint handler constructors must have a certain signature.

We also have an unfinished business with accounts endpoint. We need to add validation constraints:

```java
...

@PersistentResource(persistentCollection="account")
public class Account extends AbstractPersistentResource implements Actor {

    ...
    @NotNull
    @Size(min=1, max=50)
    @Email
    private String email;

    ...
    @Pattern(regexp="[0-9a-f]{32}")
    private String secretKeyHex;

    ...
    @NotNull(groups={ Create.class })
    private String password;

    ...
    @Pattern(regexp="[0-9a-f]{40}")
    private String passwordDigestHex;

    ...
    private boolean admin;

    ...
    @NotNull
    @Size(min=1, max=50)
    private String firstName;

    ...
    @NotNull
    @Size(min=1, max=50)
    private String lastName;

    ...
}
```

Note, that password property has a constraint for `Create.class` group. The framework uses `Create.class` group for new records and `Update.class` group for updating existing records. Our constraint requires a password submitted with new records and leaves the password unchanged if it is not submitted with update account data.

And we need to create a custom resource endpoint handler that performs permissions checking, e-mail duplicates validation and secret key generation for new accounts. For the permissions the logic is that anyone can create a new account for themselves. Account owners can get their account details, update their account and delete their account, but they cannot have access to other accounts and they cannot search the accounts collection. Admin users can do anything. Here is our accounts handler:

```java
package org.bsworks.x2sample.handlers;

import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Set;

import javax.crypto.KeyGenerator;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;

import org.bsworks.x2.Actor;
import org.bsworks.x2.EndpointCallContext;
import org.bsworks.x2.EndpointCallErrorException;
import org.bsworks.x2.HttpMethod;
import org.bsworks.x2.app.DefaultPersistentResourceEndpointHandler;
import org.bsworks.x2.resource.FilterConditionType;
import org.bsworks.x2.resource.Resources;
import org.bsworks.x2.util.Hex;
import org.bsworks.x2sample.resources.Account;


public class AccountsEndpointHandler extends DefaultPersistentResourceEndpointHandler<Account> {

    public AccountsEndpointHandler(@SuppressWarnings("unused") ServletContext sc,
            Resources resources) {
        super(resources.getPersistentResourceHandler(Account.class));
    }


    @Override
    public boolean isAllowed(HttpMethod requestMethod, String requestURI, List<String> uriParams, Actor actor) {

        // admins are allowed anything
        if ((actor != null) && actor.hasRole("admin"))
            return true;

        // anyone can create a new account
        if (requestMethod == HttpMethod.POST)
            return true;

        // get addressed account id
        final Integer accountId = (uriParams.get(0) == null ? null : Integer.valueOf(uriParams.get(0)));

        // get caller account id
        final Integer callerId = (actor == null ? null : ((Account) actor).getId());

        // you can only access your own account
        return ((callerId != null) && callerId.equals(accountId));
    }

    @Override
    public void create(EndpointCallContext ctx, Account recTmpl)
        throws EndpointCallErrorException {

        // make e-mail lower-case
        recTmpl.setEmail(recTmpl.getEmail().toLowerCase());

        // make context authenticated if not yet
        if (ctx.getActor() == null)
            ctx.assumeActor(recTmpl);

        // make sure the e-mail is not used
        if (ctx.getPersistenceTransaction()
                .createPersistentResourceFetch(Account.class)
                .setFilter(ctx.getFilterSpec(Account.class)
                        .addTrueCondition("email", FilterConditionType.EQ, recTmpl.getEmail()))
                .getCount() > 0)
            throw new EndpointCallErrorException(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "This e-mail address is already used.");

        // create secret key
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(128);
            recTmpl.setSecretKeyHex(Hex.encode(keyGen.generateKey().getEncoded()));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("AES is not supported.", e);
        }

        // proceed with the account creation
        super.create(ctx, recTmpl);
    }

    @Override
    public Set<String> update(EndpointCallContext ctx, Account rec, Account recTmpl)
        throws EndpointCallErrorException {

        // make sure the e-mail is not used
        if (ctx.getPersistenceTransaction()
                .createPersistentResourceFetch(Account.class)
                .setFilter(ctx.getFilterSpec(Account.class)
                        .addTrueCondition("email", FilterConditionType.EQ, recTmpl.getEmail())
                        .addTrueCondition("id", FilterConditionType.NE, rec.getId()))
                .getCount() > 0)
            throw new EndpointCallErrorException(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "This e-mail address is used by another acccount.");

        // proceed with updating the account
        return super.update(ctx, rec, recTmpl);
    }
}
```

### Nested Objects as Resource Properties

Our user account resource needs more properties before it can be used for placing orders. First, we need billing and shipping address. Let's define a general purpose address object and use it for both:

```java
package org.bsworks.x2sample.resources;

import javax.validation.constraints.*;

import org.bsworks.x2.resource.annotations.*;


public class Address {

    @Property(persistence=@Persistence(field="name"))
    @NotNull
    @Size(min=1, max=50)
    private String name;

    @Property(persistence=@Persistence(field="street"))
    @NotNull
    @Size(min=1, max=50)
    private String street;

    @Property(persistence=@Persistence(field="unit"))
    @Size(min=1, max=10)
    private String unit;

    @Property(persistence=@Persistence(field="city"))
    @NotNull
    @Size(min=1, max=30)
    private String city;

    @Property(persistence=@Persistence(field="state"))
    @NotNull
    @Pattern(regexp="[A-Z]{2}")
    private String state;

    @Property(persistence=@Persistence(field="zip"))
    @NotNull
    @Pattern(regexp="[0-9]{5}")
    private String zipCode;


    // getters and setters
    ...
}
```

As you can see, a nested object is defined as a persistent resource but is not annotated with `@PersistentResource`. We can store the address information in the same table together with the account, in which case the address object is called *embedded nested object property*, but there is a little problem&mdash;we have associated column names with the address properties and if we store both billing and shipping address in the account table it is going to be a column name collision. What we can do, is to associate a prefix with each address property. For example, for the billing address let's use "bill\_" and for the shipping address let's use "ship\_". Here is our address properties in the account persistent resource:

```java
...

@PersistentResource(persistentCollection="account")
public class Account extends AbstractPersistentResource implements Actor {

    ...

    @Property(persistence=@Persistence(field="bill_"))
    @NotNull
    @Valid
    private Address billingAddress;

    @Property(persistence=@Persistence(field="ship_"))
    @Valid
    private Address shippingAddress;

    ...
}
```

In this case the "field" attribute is interpreted as a prefix for the nested object column names. Now, we can add the address columns to our account table:

```sql
ALTER TABLE account ADD (
    bill_name VARCHAR(50) NOT NULL,
    bill_street VARCHAR(50) NOT NULL,
    bill_unit VARCHAR(10),
    bill_city VARCHAR(30) NOT NULL,
    bill_state CHAR(2) NOT NULL,
    bill_zip CHAR(5) NOT NULL,
    ship_name VARCHAR(50),
    ship_street VARCHAR(50),
    ship_unit VARCHAR(10),
    ship_city VARCHAR(30),
    ship_state CHAR(2),
    ship_zip CHAR(5)
);
```

The shipping address is optional. Since the address object is embedded, absence of shipping address can be checked by checking the individual address fields&mdash;they are all going to be `null` if there is no shipping address.

Another piece of information we need with the account is payment information. For example, we can store some information about the customer credit card in our database. For our sample application, we are going to assume that complete credit card information is not stored locally, but is managed by some external payment processing service. All we need in our database is some information that helps customer to identify which card is used:

```java
package org.bsworks.x2sample.resources;

import javax.validation.constraints.*;

import org.bsworks.x2.resource.ResourcePropertyAccess;
import org.bsworks.x2.resource.annotations.*;
import org.bsworks.x2.resource.validation.constraints.*;


public class CreditCard {

    @Property(persistence=@Persistence(field="type"))
    @NotNull
    private CreditCardType type;

    @Property(
        accessRestrictions={
            @AccessRestriction(ResourcePropertyAccess.SEE)
        })
    @NotNull
    @CreditCardNumber
    private String number;

    @Property(persistence=@Persistence(field="lastdigits"),
        accessRestrictions={
            @AccessRestriction(ResourcePropertyAccess.SUBMIT)
        })
    @Pattern(regexp="\\d{4}")
    private String numberLastDigits;

    @Property(persistence=@Persistence(field="expdate"))
    @NotNull
    @Pattern(regexp="20\\d{2}-(0[1-9]|1[0-2])")
    private String expirationDate;

    @Property(
        accessRestrictions={
            @AccessRestriction(ResourcePropertyAccess.SEE)
        })
    @NotNull
    @Pattern(regexp="\\d{3,4}")
    private String code;


    // getters and setters
    ...

    public void setNumber(String number) {

        this.number = number;

        this.numberLastDigits = ((number != null) && (number.length() >= 4) ?
                number.substring(number.length() - 4) : null);
    }

    ...
}
```

As with the account, we have some fields that are "in only"&mdash;the client sends credit card number and CCV code when a new credit card is submitted, but these properties are neither stored in the database nor ever sent back to the client.

Let's store credit card information in a separate table with a foreign key field pointing back at the owner account record:

```sql
CREATE TABLE ccard (
    account_id INT NOT NULL,
    type VARCHAR(15) NOT NULL,
    lastdigits CHAR(4) NOT NULL,
    expdate CHAR(7) NOT NULL,
    FOREIGN KEY (account_id) REFERENCES account (id)
);
```

The property in the account is then:

```java
...

@PersistentResource(persistentCollection="account")
public class Account extends AbstractPersistentResource implements Actor {

    ...

    @Property(persistence=@Persistence(collection="ccard", parentIdField="account_id"))
    @NotNull
    @Valid
    private CreditCard creditCard;

    ...
}
```

What if besides the credit card we want to support payments via ACH (eCheck)? Then, instead of the credit card information we need to store user bank account information. This is when a polymorphic payment information property can become useful. We can create an abstract class for various types of payment information and derive credit card and bank account information from it. Here is the the abstract class:

```java
package org.bsworks.x2sample.resources;

import org.bsworks.x2.resource.annotations.TypeProperty;


public abstract class PaymentInfo {

    @TypeProperty(persistent=false)
    private final PaymentMethod method;


    protected PaymentInfo(PaymentMethod method) {

        this.method = method;
    }


    public PaymentMethod getMethod() {

        return this.method;
    }
}
```

To support a polymorphic nested object property in the account resource the abstract payment information class must define a type property. The type property determines which concrete payment information class is used. For out case, the payment method enumeration can be like this:

```java
package org.bsworks.x2sample.resources;


public enum PaymentMethod {
    CREDIT_CARD,
    ACH_TRANSFER
}
```

The type property is marked as non-persistent, so it is not stored in a database field, which works for our purpose because credit cards and bank accounts are going to be stored each in its own table, so that the framework can tell which is used by simply checking a record in which table exists. If we had payment information stored as an embedded nested object, we would have to have a persistent type field. In reality, having a persistent type field often help with performance, especially if an RDBMS is used for storage.

Now, we can rewrite our credit card class:

```java
...

public class CreditCard extends PaymentInfo {

    public CreditCard() {
        super(PaymentMethod.CREDIT_CARD);
    }

    ...
}
```

Then add a bank account class:

```java
package org.bsworks.x2sample.resources;

import javax.validation.constraints.*;

import org.bsworks.x2.resource.ResourcePropertyAccess;
import org.bsworks.x2.resource.annotations.*;
import org.bsworks.x2.resource.validation.constraints.*;


public class BankAccount extends PaymentInfo {

    @Property(persistence=@Persistence(field="type"))
    @NotNull
    private BankAccountType type;

    @Property(
        accessRestrictions={
            @AccessRestriction(ResourcePropertyAccess.SEE)
        })
    @NotNull
    @RoutingNumber
    private String routingNumber;

    @Property(
        accessRestrictions={
            @AccessRestriction(ResourcePropertyAccess.SEE)
        })
    @NotNull
    @Pattern(regexp="\\d{5,17}")
    private String number;

    @Property(persistence=@Persistence(field="lastdigits"),
        accessRestrictions={
            @AccessRestriction(ResourcePropertyAccess.SUBMIT)
        })
    @Pattern(regexp="\\d{4}")
    private String numberLastDigits;


    public BankAccount() {
        super(PaymentMethod.ACH_TRANSFER);
    }


    // getters and setters
    ...

    public void setNumber(String number) {

        this.number = number;

        this.numberLastDigits = ((number != null) && (number.length() >= 4) ?
                number.substring(number.length() - 4) : null);
    }

    ...
}
```

With table:

```sql
CREATE TABLE bankaccount (
    account_id INT NOT NULL,
    type VARCHAR(10) NOT NULL,
    lastdigits CHAR(4) NOT NULL,
    FOREIGN KEY (account_id) REFERENCES account (id)
);
```

And finally replace the credit card property in the account resource with the polymorphic payment information property:

```java
@PersistentResource(persistentCollection="account")
public class Account extends AbstractPersistentResource implements Actor {

    ...

    @Property(persistence=@Persistence(parentIdField="account_id"),
        updateIfNull=false,
        valueTypes={
            @ValueType(name="CREDIT_CARD", concreteClass=CreditCard.class,
                    persistentCollection="ccard"),
            @ValueType(name="ACH_TRANSFER", concreteClass=BankAccount.class,
                    persistentCollection="bankaccount")
        })
    @NotNull(groups={ Create.class })
    @Valid
    private PaymentInfo paymentInfo;

    ...
}
```

We made it `updateIfNull=false` so that the payment information can be left untouched when an account is updated and no new payment information is provided in the incoming data.

Now, data for a new account posted to the accounts management endpoint may look like this:

```json
{
    "email": "test@example.com",
    "password": "password",
    "firstName": "Moses",
    "lastName": "Pickle",
    "billingAddress": {
        "name": "Moe's",
        "street": "23 E 116th St.",
        "city": "New York",
        "state": "NY",
        "zipCode": "10029"
    },
    "paymentInfo": {
        "method": "CREDIT_CARD",
        "type": "VISA",
        "number": "4111111111111111",
        "expirationDate": "2020-01",
        "code": "474"
    }
}
```

Important moment to keep in mind is that with polymorphic nested objects the concrete type property ("method" in our case) always must be the first property in the incoming JSON.

### Application Resource Relationships

Now we can try to define the last remaining resource&mdash;the order. The order resource is connected to both the customer account and the product. In X2 such relationships between persistent resources are expressed using reference properties. Here is our order persistent resource:

```java
package org.bsworks.x2sample.resources;

import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.*;

import org.bsworks.x2.resource.Ref;
import org.bsworks.x2.resource.annotations.*;


@PersistentResource(persistentCollection="ord")
public class Order extends AbstractPersistentResource {

    @Property(persistence=@Persistence(field="account_id"))
    @NotNull
    private Ref<Account> account;

    @Property(persistence=@Persistence(collection="ord_item", parentIdField="ord_id"))
    @NotNull
    @Size(min=1)
    @Valid
    private Set<OrderItem> items;


    // getters and setters
    ...
}
```

And here is an order item nested object:

```java
package org.bsworks.x2sample.resources;

import javax.validation.constraints.*;

import org.bsworks.x2.resource.Ref;
import org.bsworks.x2.resource.annotations.*;


public class OrderItem {

    @IdProperty(handling=IdHandling.AUTO_GENERATED, persistentField="id")
    private Integer id;

    @Property(persistence=@Persistence(field="product_id"))
    @NotNull
    private Ref<Product> product;

    @Property(persistence=@Persistence(field="qty"))
    @Min(1)
    private int quantity;


    // getters and setters
    ...
}
```

In the database, the references are stored as target record ids:

```sql
CREATE TABLE ord (
    id INT PRIMARY KEY AUTO_INCREMENT,
    version INT NOT NULL,
    created_on TIMESTAMP NOT NULL,
    created_by VARCHAR(50) NOT NULL,
    modified_on TIMESTAMP NOT NULL,
    modified_by VARCHAR(50) NOT NULL,
    account_id INT NOT NULL,
    FOREIGN KEY (account_id) REFERENCES account (id)
);

CREATE TABLE ord_item (
    id INT PRIMARY KEY AUTO_INCREMENT,
    ord_id INT NOT NULL,
    product_id INT NOT NULL,
    qty INT NOT NULL,
    FOREIGN KEY (ord_id) REFERENCES ord (id),
    FOREIGN KEY (product_id) REFERENCES product (id),
    UNIQUE (ord_id, product_id)
);
```

Note, that a nested object used in a collection property must have its own id property. This allows the framework to update such collection properties efficiently. It also must have a parent record id field, which links it to the owning record (the parent record id field does not have to be a resource property).

From the point of view of an account, an order is a ''dependent resource'', because it contains a reference to the account and may exist only in the context of an account. If we want to include a list of orders in the account data when we receive it over the application API, we can define a dependent resource reference property in the account resource:

```java
...

@PersistentResource(persistentCollection="account")
public class Account extends AbstractPersistentResource implements Actor {

    ...

    @DependentRefProperty(reverseRefProperty="account", optional=true)
    private Set<Ref<Order>> orders;

    ...
}
```

There is no need to change anything in the database. Attribute `optional=true` tells the framework to use an outer join when fetching orders for an account so that accounts without orders are allowed.

To see how it works, let's create an order. If we assume that we have an account record with id 2 and product records with ids 1 and 2, we can post a new order like this (assuming also that we've mapped the orders endpoint to "/api/orders"):

```http
POST /api/orders HTTP/1.1
Authorization: AuthToken username=admin@example.com,token=...
Content-Type: application/json
...

{
    "account": "Account#2",
    "items": [
        {
            "product": "Product#1",
            "quantity": 1
        },
        {
            "product": "Product#2",
            "quantity": 5
        }
    ]
}
```

Note how references are represented in the JSON.

Now, if our new order has id 1, we are going to see it in the orders collection:

```http
GET /api/orders HTTP/1.1
Authorization: AuthToken username=admin@example.com,token=...
...
```

```http
HTTP/1.1 200 OK
Content-Type: application/json;charset=UTF-8
...

{
    "records": [
        {
            "id": 1,
            "version": 1,
            "createdOn": "2014-11-19T20:07:50Z",
            "createdBy": "admin@example.com",
            "lastModifiedOn": "2014-11-19T20:07:50Z",
            "lastModifiedBy": "admin@example.com",
            "account": "Account#2",
            "items": [
                {
                    "id": 1,
                    "product": "Product#1",
                    "quantity": 1
                },
                {
                    "id": 2,
                    "product": "Product#2",
                    "quantity": 5
                }
            ]
        }
    ],
    "refs": null,
    "totalCount": -1
}
```

And in the account:

```http
GET /api/accounts/2 HTTP/1.1
Authorization: AuthToken username=admin@example.com,token=...
...
```

```http
HTTP/1.1 200 OK
Content-Type: application/json;charset=UTF-8
...

{
    "id": 2,
    "version": 1,
    "createdOn": "2014-11-19T20:01:23Z",
    "createdBy": "init",
    "lastModifiedOn": "2014-11-19T20:01:23Z",
    "lastModifiedBy": "init",
    "email": "customer@example.com",
    "admin": false,
    "firstName": "Carl",
    "lastName": "Becker",
    "billingAddress": {
        ...
    },
    "shippingAddress": {
        ...
    },
    "paymentInfo": {
        ...
    },
    "orders": [
        "Order#1"
    ]
}
```

What if we want to get the order details in the same response? We can request the order references to be resolved:

```http
GET /api/accounts/2?e=orders HTTP/1.1
Authorization: AuthToken username=admin@example.com,token=...
...
```

```http
HTTP/1.1 200 OK
Content-Type: application/json;charset=UTF-8
...

{
    "records": [
        {
            "id": 2,
            "version": 1,
            "createdOn": "2014-11-19T20:01:23Z",
            "createdBy": "init",
            "lastModifiedOn": "2014-11-19T20:01:23Z",
            "lastModifiedBy": "init",
            "email": "customer@example.com",
            "admin": false,
            "firstName": "Carl",
            "lastName": "Becker",
            "billingAddress": {
                ...
            },
            "shippingAddress": {
                ...
            },
            "paymentInfo": {
                ...
            },
            "orders": [
                "Order#1"
            ]
        }
    ],
    "refs": {
        "Order#1": {
            "id": 1,
            "version": 1,
            "createdOn": "2014-11-19T20:07:50Z",
            "createdBy": "customer@example.com",
            "lastModifiedOn": "2014-11-19T20:07:50Z",
            "lastModifiedBy": "customer@example.com",
            "account": "Account#2",
            "items": [
                {
                    "id": 1,
                    "product": "Product#1",
                    "quantity": 1
                },
                {
                    "id": 2,
                    "product": "Product#2",
                    "quantity": 5
                }
            ]
        }
    },
    "totalCount": -1
}
```

The result is returned in a wrapper object and this time the "refs" property contains the referred records by references.

We can request even more information to be included in a single response (and, worth to mentioned, loaded from the database in a single transaction). For example, we also need information about the products:

```http
GET /api/accounts/2?e=orders,orders.items.product HTTP/1.1
Authorization: AuthToken username=admin@example.com,token=...
...
```

```http
HTTP/1.1 200 OK
Content-Type: application/json;charset=UTF-8
...

{
    "records": [
        ...
    ],
    "refs": {
        "Order#1": {
            "id": 1,
            "version": 1,
            "createdOn": "2014-11-19T20:07:50Z",
            "createdBy": "customer@example.com",
            "lastModifiedOn": "2014-11-19T20:07:50Z",
            "lastModifiedBy": "customer@example.com",
            "account": "Account#2",
            "items": [
                {
                    "id": 1,
                    "product": "Product#1",
                    "quantity": 1
                },
                {
                    "id": 2,
                    "product": "Product#2",
                    "quantity": 5
                }
            ]
        },
        "Product#1": {
            "id": 1,
            "version": 1,
            "createdOn": "2014-11-19T20:01:23Z",
            "createdBy": "init",
            "lastModifiedOn": "2014-11-19T20:01:23Z",
            "lastModifiedBy": "init",
            "title": "Buttons",
            "price": 10.99
        },
        "Product#2": {
            "id": 2,
            "version": 1,
            "createdOn": "2014-11-19T20:01:23Z",
            "createdBy": "init",
            "lastModifiedOn": "2014-11-19T20:01:23Z",
            "lastModifiedBy": "init",
            "title": "Needles",
            "price": 4.5
        }
    },
    "totalCount": -1
}
```

Now, if we request a list of accounts, a list of order references is included in the "orders" property for each account. The list of orders may grow long in time and if all we need is a list of accounts without any information about the orders, fetching the order lists becomes inefficient. We can exclude certain properties from the returned result. In our case, to exclude the orders and, for example, the payment information (which is loaded via a relatively expensive database table join), we could send a query:

```http
GET /api/accounts?x=orders,paymentInfo HTTP/1.1
Authorization: AuthToken username=admin@example.com,token=...
...
```

```http
HTTP/1.1 200 OK
Content-Type: application/json;charset=UTF-8
...

{
    "records": [
        {
            "id": 2,
            "version": 1,
            "createdOn": "2014-11-19T20:01:23Z",
            "createdBy": "init",
            "lastModifiedOn": "2014-11-19T20:01:23Z",
            "lastModifiedBy": "init",
            "email": "customer@example.com",
            "admin": false,
            "firstName": "Carl",
            "lastName": "Becker",
            "billingAddress": {
                ...
            },
            "shippingAddress": {
                ...
            },
            "paymentInfo": null,
            "orders": null
        },
        ...
    ],
    "refs": null,
    "totalCount": -1
}
```

Payment information and orders are not queried and are not included in the response.

This concludes a demonstration of the most basic features of the framework. A fully developed sample application can be found at https://github.com/boylesoftware/thymes2-sample.

## Reference

### Framework Configuration Parameters

These are web-application context parameters utilized by the framework and allowing to customize it for the application and/or its runtime environment:

| Parameter &amp; Default Value | Description |
| --- | --- |
| x2.app.manifest<br/>*Default:* /META-INF/MANIFEST.MF | Application manifest file path, relative to the web-application context. In live mode, the manifest file is used to get the application version ("Implementation-Version" manifest attribute). The application version is used, for example, in forming resource "ETag" values, so that resources cached by clients from different application releases do not mix up. |
| x2.threads.EndpointCallProcessors<br/>*Default:* 2 | Number of threads used to process regular endpoint requests. |
| x2.threads.JobRunners<br/>*Default:* 1 | Number of threads used for long-running background jobs and "long job" endpoint requests. |
| x2.threads.SideTaskProcessors<br/>*Default:* 1 | Number of threads used to execute asynchronous side tasks. |
| x2.maxRequestSize<br/>*Default:* 2048 | Maximum allowed HTTP request entity size in bytes. |
| x2.regularCallTimeout<br/>*Default:*10000 | Timeout in milliseconds for processing regular endpoint calls. Value of zero or less indicates no such timeout. |
| x2.longJobCallTimeout<br/>*Default:* 60000 | Timeout in milliseconds for processing "long job" endpoint calls. Value of zero or less indicates no such timeout. |
| x2.allowedOriginsPattern<br/>*Default:* \* | Regular expression for allowed CORS request origins. Special value of \* is used for the CORS wildcard. |
| x2.auth.tokenTTL<br/>*Default:* 1800000 | Number of milliseconds a newly issued authentication token is valid. |
| x2.auth.cache.discardAfter<br/>*Default:* 60000 | Number of milliseconds after which to expire results of actor lookup in the actor authentication service. If 0, caching of actor lookup results is disabled. |
| x2.auth.cache.refreshAfter<br/>*Default:* 5000 | Number of milliseconds after which to request refresh of cached actor lookup results. As opposed to the discard timeout, refreshing is performed asynchronously in the background, which the currently cached result is still returned. |
| x2.auth.cache.maxSize<br/>*Default:* 256 | Maximum size of the actor lookup results cache. |
| x2.service.serialization.provider<br/>*Default:* org.bsworks.x2.services.serialization.impl.json.JsonResourceSerializationServiceProvider | Class name of the resource serialization service provider used by the application. |
| x2.service.versioning.provider<br/>*Default:* org.bsworks.x2.services.versioning.impl.memory.MemoryPersistentResourceVersioningServiceProvider | Class name of the persistent resource versioning service provider used by the application. |
| x2.service.persistence.provider<br/>*Default:* org.bsworks.x2.services.persistence.impl.dummy.DummyPersistenceServiceProvider | Class name of the persistence service provider used by the application. |
| x2.service.auth.provider<br/>*Default:* org.bsworks.x2.services.auth.impl.dummy.DummyActorAuthenticationServiceProvider | Class name of the actor authentication service provider used by the application. |
| x2.service.monitor.provider<br/>*Default:* org.bsworks.x2.services.monitor.impl.dummy.DummyMonitorServiceProvider | Class name of the application internal monitor service provider used by the application. |
| x2.app.serviceProviders | Whitespace-separated list of additional application service providers each with an optional service instance id following a colon after the provider class name. |
| x2.app.persistentResources.packages | Whitespace-separated list of Java packages that contain application persistent resources. |
| x2.app.persistentResources.jarsPattern | Regular expression pattern for JAR file names to scan for persistent application resources. If empty, only classes in /WEB-INF/classes are scanned. |
| x2.app.endpoints | Service endpoint definitions. |

## Additional Information

* [API Reference](http://www.boylesoftware.com/thymes2/site/apidocs/)
* [Project on GitHub](https://github.com/boylesoftware/thymes2)
* [Sample Application on GitHub](https://github.com/boylesoftware/thymes2-sample)
