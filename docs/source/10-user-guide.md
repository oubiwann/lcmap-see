LCMAP SEE User's Guide

*SEE Inner Workings and Relationship to LCMAP REST*

# Introduction

## LCMAP

The intent of the Land Change Monitoring Assessment and Projection (LCMAP)
initiative is to enable analysis of a very large amount of continuously
acquired data to understand historical trends, assess current states, and
project future states at regular intervals.

The system capacity and functionality needed to support the magnitude of
long-term time series analysis is expected to employ Earth data science,
demanding highly specialized computing environments to execute large scale
analysis.

## LCMAP SEE

Key to delivery of a functional LCMAP initiative is the Science Execution
Environment (SEE). This represents the capability of executing LCMAP science
models on potentially diverse, configurable, and ideally distributed
computation platforms.

While the SEE has been running in LCMAP for several months, only recently has
it introduced the concept of and support for "swappable" (configurable)
execution backends. The original and prototypical execution platform for the
SEE was simply the native OS (now ported to the "native" backend). While useful
for demonstration purpose, something far more robust is required for the
execution of science models at scale.

The first version of this document captures the new feature of the see: a "mesos" backend that is capable of executing science models on a distributed compute infrastructure that has Apache Mesos installed and configured.

## Document Purpose

This document touches on two primary aspects of the LCMAP System:

1. **The Science Execution Environment** (SEE), which is responsible for
   running approved science models on infrastructure provided by USGS-EROS, and

2. **The web resources** for executing these models (the LCMAP REST server),
   allowing science programmers (using client libraries) and shell scripters
   (using curl) to run models and extract model results from LCMAP.

Both of these are written in Clojure. The former is the entirety of the LCMAP
SEE code under the lcmap.see namespace. The latter is only a small segment of
the LCMAP REST codebase, and lives under the lcmap.rest.api.models namespace.

While initially intended as documentation for the SEE, this document has also
become a guide and possibly even a best practices document for creators and
maintainers of SEE-ready science models. In particular, the last half of the
document that focuses on the system startup and execution flow walkthroughs
provide a great deal of information and guidance on how to create science
models, and how to understand them such that one may properly update or debug
them.

## System Highlights

The LCMAP System, as it applies to the web resources (REST) and model execution
(SEE) mentioned above, has the following features:

* a means of **receiving science model execution requests** from users

* a **native OS mode** of running science models (used primarily for
  development and testing)

* a **distributed mode** of running science models (Apache Mesos)

* a means of **dynamically selecting** these based on configuration

* a means of **tracking the progress** of long-running science model executions
  and returning immediate results for previously run science models

It is important to note that there are two primary events which make data
available to the system as a whole (and upon which both the REST web service
and the SEE components depend):

* initial start-up, when configuration data, database connections, job threads,
  are established, and

* user requests, when parameters and/or payloads are provided via the REST web
  service

Each of these events exercises different parts of the codebase and initiates
different flows of data through the system.

# Additional Resources

While this document provides a prose technical overview as well as detailed
walkthroughs of major portions of the system, there are also other resources
available for more detailed views of the system:

* [SEE API Reference](http://usgs-eros.github.io/lcmap-see/current/)

* [REST API Reference](http://usgs-eros.github.io/lcmap-rest/current/)

* [Client Documentation](http://usgs-eros.github.io/lcmap-client-docs/current/)

# Concepts

## Science Model

The idea of a "science model" is a fairly amorphous thing. Even as we begin to
make technical decisions based upon a growing understanding of a "science
model", it turns out that any definition of a science model is going to be, by
necessity, highly context-sensitive. However, operations on a science model
need to be very specific, at each point in the system, and below is what we've
come up with.

### Running a Model in REST

The primary responsibilities of a function that "runs" a science model in the
context of a REST server resource are simply the following:

* delegate the work to the LCMAP SEE

* prepare an appropriate HTTP response

The last bullet includes any miscellaneous or model-specific, **HTTP-related**
functions that need to be called, data that needs to be prepared, etc. The
run-model function in the REST API should *only* be concerned with HTTP
requests and responses, and delegate the rest.

In the context of the REST model API, a user passes parameters needed by a
science model, and the REST server makes a call, providing the parameters.
However, there are some thing that needs to happen around the REST server's
view of a science model:

* the SEE backend needs to be extracted from the request data

* any additional context needs to be set up and passed (this may not be
  necessary in the case of most models)

* the model execution needs to be "delegated" to the SEE backend (this may be a
  function call that looks like the model is being executed … but it probably
  isn't -- we'll address this in the next sub-section)

* the execution "result" needs to be included in the HTTP response

* the HTTP response needs to be returned

The second-to-last bullet is an important point: from the perspective of the
REST server, the result here is a resource URL or link, not an actual science
model result that contains processed data. The link, that the response will
provide to the user will actually be something the user (but more likely, the
user's application) will call to check on the status of the *actual* result.

### Running a Model in a Backend

The backend is simply the protocol that has been extended over the record and
associated protocol behaviours (function implementations). The "implementation"
object that gets passed around is this extended record. The run-model function
defined by the protocol has two primary responsibilities:

1. given the backend name and the model name, locate the model's run-model
   function (see the next section for this function)

2. apply an updated set of model args to this located model function, actually
   calling it

While at first glance this work may seem like taking the practice of software
abstraction too far, it was introduced out of necessity. This mechanism is
essential for the support of arbitrary backends and the capacity of the LCMAP
SEE system to locate a model and

### Running a Model in the Model's Context

The primary responsibilities of a function that "runs" a science model in the
context of a SEE backend are the following:

* prepare -- but don't run -- the actual functions to be called; this includes
  defining execution-specific workflows, defining how to collate and gather
  partial results, defining map-reduce functions, etc.

* perform a call to the job tracker, and return the job-tracker's result --
  which needs to be the job id that the job tracker assigns to that particular
  task

<table>
  <tr>
    <td>NOTE 

All of these functions need to respond immediately in order that the call made
in the HTTP server receives the data that it needs: the job id of the tracked
run of the science model.</td>
  </tr>
</table>


<table>
  <tr>
    <td>NOTE

The backend knows everything about the science model: the args that got passed
to it, the valid data types of the args, how to execute the model, how to
extract the results, how to perform any post-processing of the model, etc. All
of the necessary steps in the workflow of a model execution are defined in a
wrapping function -- and this wrapping function is what gets passed to the
tracker, is is ultimately what will be called by the tracker.

Keeping the complete knowledge of the model execution in a backend's
implementation isolates the rest of the SEE from details it doesn't need to
know.</td>
  </tr>
</table>


### Running a Model in a Tracker

The tracker is quite dumb when it comes to the model: it doesn't know anything
about it, other than the backend that was used to prepare the model execution
function it received (the tracker has to know the backend so that it can
perform the actual execution on it). At a high level, all the tracker is
responsible for are the following:

* generating a unique hash for a combination of the fully-qualified function
  and all the science model parameters that got passed

* setting the job's initial status

* by-passing a wasteful, needless science model execution if at all possible

* running the supplied function and parameters (the science model
  wrapper/set-up function provided by the backend)

* saving the results of the science model and updating the job's status

Additional details and more fine-grained responsibilities are covered later in
this document, but for a conceptual overview, the above represents the essence
of the tracker's world-view when it comes to science models.

<table>
  <tr>
    <td>NOTE 

The tracker knows nothing about the details of a science model -- it just runs
the science model function it has been given. This keeps job-tracking as simple
as possible (and makes porting job-tracking to another platform, mechanism,
technology, etc., dead-simple).</td>
  </tr>
</table>


## Implementations vs. Lookups

The SEE has two major types of inversion of control: 

* protocols + implementations, and

* model conventions + lookups

An example of the first is a collection of backend protocols, the associated
base behaviours, and then an extension of the protocol over a record with
behaviour overrides. When a backend method is called, the specific
implementation is accessed and the appropriate function is called. What makes
this inversion of control is the fact that the backend is driven by
configuration, the implementation is dynamically instantiated, and to look at
the calling code, all you see is the generic call. The important thing to keep
in mind here is that when this is used, here are the bits that are important:

* the data in the implementation's record fields

* the methods defined for that implementation

An example of the second type of control inversion in the SEE is the lookup
mechanism for a given model. Of of the things that a backend will do is look up
a model when provided a model name. This is possible due to SEE models
following a prescribed convention when it comes to the location of models: they
will always be at the following location:

  lcmap.see.backend.<backend type>.models.<model name>

What makes this usage a form of inversion of control is the fact that when
calling run-model for the "sample" model, you are actually using a generic
function that performs a lookup, based upon the type of backend configured (and
thus instantiated), and then the SEE "framework" calls into the particular
model found in the active backend.

The important thing to keep in mind here is:

* the model doesn't have an implementation

* a given model only works for a particular backend

* there is no intersection of responsibilities between backends (or job
  trackers) and models

# Component Descriptions

At this point, it would be useful to describe the key components, and what
their specific responsibilities are. With this knowledge, a detailed
walkthrough of a user request through the system will have more context and
make more sense.

## Execution Backends

In order for a model to run and provide results, a means of running it must
exist. In environments where an operating system is the primary mode of
execution, this is a given, and so often over-looked. In that, the first
backend supported by the SEE was the operating system UNIX shell, bash. But in
a distributed execution model, this may not be the case (especially for
efficient distributed execution systems).

An implementation of an execution backend extends a Clojure record (backend
data) with a protocol (Interface) and behaviours (method implementations). Note
that for every protocol and behaviour that is defined for the execution
backend, the same record type is extended.

### Data

The data tracked by an execution backend are the following:

* backend name

* configuration

* database connection

As such, any time a backend implementation is instantiated, the above data are
provided and the instance has access to those data at all times, obviating the
programmer from passing the required data through the system in order to be
present when a backend method is called. 

Note that execution backend data is saved (and accessed) via Clojure records.

### Protocols

There are two protocols (and thus two sets of protocol methods) defined for a
backend:

* IComponentable - defines the methods necessary for a backend to be brought up
  and shut down by the LCMAP component system (the "Component" library)

* IModelable - defines the methods necessary for a backend to execute and/or
  manage a science model

Note that a backend requires both protocols to be extended in order to be used
by LCMAP SEE.

### Behaviours

The *behaviours* are simply the protocol implementations for a given backend.
All the methods defined in the protocols must be implemented for the particular
backend.

### Currently Supported Backends

LCMAP SEE currently provides two execution backends:

* native - for running science models on the same host (using bash) as the REST
  service itself

* mesos - for running science models in a hosted, distributed execution
  environment using Apache Mesos frameworks

### Future Backends

There are other backends expected to be developed in the lifetime of the LCMAP
SEE project:

* marathon - for running science models in a fault-tolerant environment managed
  by Marathon (would likely use much of the code for the Mesos backend, but
  change some of the specifics for configuration and job execution)

* ec2 - for running science models remotely on Amazon's AWS compute
  infrastructure

Also possible, but less likely (at least near-term) are backends for the
following:

* Apache Spark

* Google App Engine

* RedHat OpenShift

* OpenStack

* Kubernetes

* NASA infrastructure

* Various HPC environments (e.g., SLURM, MPI-based deployments, OpenHMPP, etc.)

### Multiple Backends

Currently only one backend may be configured at a time, but there is no
technical limitation to supporting the dynamic selection of a backend at model
execution time. This would require that the start-up flow configure and connect
to all the supported backends, and then allow for users of the system to
indicate which backend their science model should run upon when submitting a
request.

## Job Tracking

While the presence of an execution backend is necessary for model execution, it
is not sufficient. The actual call of a model needs to take place such that the
following occur:

* an identifier for the function being called is recorded

* the specific argument keys are recorded

* the specific argument values are recorded

* a unique hash for all of the above is recorded (this becomes the "job ID")

* a lookup is performed to see if a given job ID already has a result available

* if not, the job status is set as pending and is then executed against the
  configured backend

* upon successful completion of the model execution, the model results are
  stored and the job status is set as completed

Most of these would be the same regardless of execution backend. The two that
do need to be updated, depending upon the configured execution backend are:

* actual science model execution (each execution backend will do this
  differently)

* collection of science model results (each execution backend will likely
  provide results in a different manner)

As such, a similar approach is used for job tracking as that used for selecting
an execution backend. An implementation of a job tracker extends a Clojure
record (tracker data) with a protocol (Interface) and behaviours (method
implementations). Note that, as with backends, for every protocol and behaviour
that is defined for the job tracker, the same record type is extended.

### Tracker Finite State Machine

Each tracker implements an FSM used to perform actions based on transition from
one state to another. A table of these is provided here:

<table>
  <tr>
    <td>Upon Transition to:</td>
    <td>Perform action:</td>
  </tr>
  <tr>
    <td>:job-track-init</td>
    <td>#'tracker/init-job-track</td>
  </tr>
  <tr>
    <td>:job-result-exists</td>
    <td>#'tracker/return-existing-result</td>
  </tr>
  <tr>
    <td>:job-start-run</td>
    <td>#'start-job-run</td>
  </tr>
  <tr>
    <td>:job-finish-run</td>
    <td>#'finish-job-run</td>
  </tr>
  <tr>
    <td>:job-save-data</td>
    <td>#'tracker/save-job-data</td>
  </tr>
  <tr>
    <td>:job-track-finish</td>
    <td>#'tracker/finish-job-track</td>
  </tr>
  <tr>
    <td>:job-done</td>
    <td>#'tracker/done</td>
  </tr>
</table>


Note that in the current implementations, only start-job-run and finish-job-run
are overridden -- all the others use the default protocol implementations. 

A transition from one state to another is accomplished by means of: 

1. sending messages to the event-thread (actor model fiber) for the current job
   being tracked, and 

2. executing message responses via the event handler (dispatch function) where
   the FSM table is defined.

### Data

The job trackers require similar access to data and services that the execution
backends do. As such, they are instantiated (using Clojure records) similarly:

* model name

* configuration

* database connection

* event thread

As one may infer from the first field, trackers are instantiated once per every
model run request.

### Protocols

There are two protocols (and thus two sets of protocol methods) defined for a
backend:

* ITrackable - defines the methods necessary for a job tracker to be brought
  up, shut down, and connected to the event thread by the LCMAP component system
  (the "Component" library)

* IJobable - defines the methods necessary for a job tracker start a science
  model "job", save its results, and send the necessary messages to other
  components during the lifetime of model execution

### Behaviours

As with the backends, the *behaviours* are simply the protocol implementations
for a given job tracker. All the methods defined in the protocols must be
implemented for the particular job tracker.

### Currently Supported Trackers

The currently supported trackers map (in name) to the supported backends; both
are needed for the successful use of LCMAP SEE. The currently supported job
trackers are:

* native

* mesos

### Future Trackers

As with execution backends, there are other backends expected to be developed
in the lifetime of the LCMAP SEE project -- both near- and far-term. See the
[Future Backends](#heading=h.pkh2vgfv5w50) section for more detail.

### Multiple Trackers

Currently only one tracker may be selected at a time, and this is contingent
upon having a corresponding execution backend of the same type. However, as
soon as multiple execution backends are supported, we will need to support
multiple simultaneous trackers. As with execution backends, there is no
technical limitation to supporting the dynamic selection of a tracker at model
execution time. This would require that the start-up flow configure and connect
to all the supported backends and trackers, and then allow for users of the
system to indicate which backend their science model should run upon when
submitting a request and this would select the matched tracker, behind the
scenes.

## Event Thread

The job tracking event thread is the means by which a job tracker component in
the LCMAP system transitions from one state to another, whether that is getting
previously run model results, setting the pending status of a model run, or
saving the results of a new run to the database.

### Current Implementation

The current implementation of the event thread is as a Java agent using
Parallel Universe's Quasar (actor model) library. LCMAP SEE and LCMAP REST
start the Java agent with the command lein trampoline run (using the agent
configuration in the project.clj file). LCMAP SEE interacts with the agent (the
event thread) using Parallel Universe's Pulsar (Clojure port of Quasar)
library.

In addition, both in the LCMAP REST and the LCMAP SEE projects, the "job"
component ultimately creates a reference to the event thread for use by the
LCMAP SEE codebase.

### Future

In the future, it is expected that the job-tracking thread will be replaced by
a message queue in the LCMAP event system.

# System Startup Walkthrough

When the LCMAP REST service first starts, the following components are started:

1. Job tracker agent startup

2. Configuration

3. Logging

4. Connection to the job tracker results database (and keyspace)

5. Selection of an execution backend

There are several services upon which the LCMAP REST service depends, none of
which are started by or controlled by the REST service. The REST service simply
has components which connect to the dependent services. Those which are
applicable to the SEE and which are expected to already be running are as
follows:

* A configuration file at ~/.usgs/lcmap.ini

* A Cassandra database (connection details provided in configuration)

* A Mesos master (connection details provided in configuration)

* A Mesos agent running on the same host as the REST service

Note that there are other, non-SEE-related services that the REST service
depends upon that must be started in order for LCMAP REST startup to complete
successfully -- in particular, the messaging/event service, lcmap.event. This
service is not currently used by SEE, but it is expected that the job-tracking
infrastructure currently used (Parallel Universe's actor model libraries and
async Java agents) will be updated to use lcmap.event. 

The following sections walk through -- in detail -- the actions taken when each
stage of the startup executes.

## Job Tracker Agent Startup

Until the SEE Job Tracker is updated to use lcmap.event, it is dependent upon
the JVM actor library provided by Parallel Universe's Quasar (Scala) and Pulsar
(Clojure) libraries. Inspired by the Erlang implementation of the actor model,
Quasar offers an analog to epmd (the Erlang daemon that acts as a message
broker between nodes on a single host) in the form of a Java agent defined by
Quasar. lein provides the capability of starting up Java agents, and this is
how we start the Quasar agent. 

Note that this is completely separate from the component startup procedure. If
lein is not used to start the REST server, then the Quasar agent will need to
be started manually, before the components are brought up.

## Configuration

The first component that is brought up in the system of components in any given
lcmap.* project is the configuration component. A standard configuration file
is read into memory (it is expected to be at ~/.usgs/lcmap.ini) and parsed into
a Clojure data structure which is then available for use by all remaining
components -- both during startup as well as in the lifetime of the associated
component objects (e.g., HTTP server, DB connections, messaging system, etc.)

## Logging

After configuration, the logging component is brought up. Once the logging
component is running, log messages will be processes as configured (by default,
they are sent to stdout in coloured output). 

Note that the log level is set in two places:

* before the logging component is brought up, it is manually set to level INFO
  in the top-level app namespace

* once the logging component is running, the bootstrapped log level is
  overridden by what is configured in the project.clj file.

## Job Tracker Database

The Job Tracker uses different namespaces and tables from the rest of the LCMAP
project. In particular, it has a keyspace + table for tracking jobs and then a
separate keyspace + table for storing results. A connection to the database for
accesses these is brought up as part of this component.

<table>
  <tr>
    <td>NOTE 

The job tracker database connection is only available as a child of the SEE
component (see the next section).</td>
  </tr>
</table>


## SEE Backend Selection

Finally, the SEE backend is selected, configured, and brought up. The backend
implementation is attached to the :see component as a child component;
similarly, the :jobdb component is as well. As such, the :see component merely
a data structure containing actual components, not a component in itself.

# Model Execution Walkthrough

At this point, a big-picture of LCMAP SEE and related LCMAP REST have been
provided, with some details of the high-level concepts as well as the more
detailed components of LCMAP SEE. Now we will examine what happens when a user
requests the execution of a science model.

The key actions in a model execution flow are the following:

* A user makes a model execution request to the REST service

* The REST service makes a call to the backend that was included in the SEE
  component during system start-up

* The configured backend issues a call to the job tracker

* If the result for the request already exists (identified by model function
  and execution parameters) the job terminates with success (never having needed
  to run the model)

* If a result for the request has not yet been saved, (NOTE:  This phrase was
  chosen deliberately and written with precision: the only criteria for a job not
  to execute the model is if a result for the calculated job ID already exists
  (as been saved) in the database. If such is not found upon querying the
  database, the job continues. This means that there is ample time for multiple,
  identical long-running model run calls to be made before the first of them to
  finish has had a chance to save its results to the database. There is a ticket
  opened (LCMAP-147) that proposes a means of shrinking this race condition
  window to a much smaller period (using the LCMAP Event System).) the job
  tracker initiates a model run, using all the information provided to it by the
  backend

In the following sections, these are walked through in detail.

## User Request

When a science user or application wishes to run a science model in LCMAP, a
client library (including raw HTTP with a tool like curl) is utilized to
construct the proper headers, payload, and URL to form an acceptable HTTP
request. The client documentation covers all of that, so we will simply refer
to a model resource's path, /api/models/sample/os-process, where a model run's
data is POSTed.

## REST Model Run

This part of the walkthrough focuses on the preparatory steps for model
execution taken by the web services (REST) portion of the codebase.

If the user-supplied arguments match the expected values defined by that
model's schema in lcmap.rest.api.models.sample, then the code defined in that
namespace for model execution is given the go-ahead to continue.

### Make Backend Model Run Call

The execution at this level is comprised of sending the configured backend the
following information:

* the request model to run (as a string; in this example, that would be "sample")

* the arguments to pass to the requested model, as provided by the user (and
  validated by the REST server)

<table>
  <tr>
    <td>IMPORTANT!

The requested model is given as a string which must exactly match the Clojure
name of the filename. 

For instance, if we have configured the backend as being ec2, and we have
defined a model in src/lcmap/see/backends/ec2/models/my_model.clj, which will
be in namespace lcmap.see.backends.ec2.models.my-model, then the name we give
in the REST request must match the Clojure value of the filename in the
namespace, i.e., my-model.

The reason for this is that the backend does a dynamic lookup to locate a
resolvable namespace + function, and the last portion of the namespace is taken
from the provided string.</td>
  </tr>
</table>


<table>
  <tr>
    <td>NOTE

The protocol method run-model takes two arguments: the implementation of the
protocol and the list of args that will be used to run the model. The reason
for the second argument being a list instead of defining each argument in the
protocol is that protocol definitions don't allow for variable args. 

We would have to support polymorphic protocol methods for each model that had a
different number of args, and even these wouldn't have args in the same
position with the same meaning across all models with the same number of args. 

Furthermore, every time a model was added which had a different number of args
(or if a models args changed), the protocol that defines run-model would have
to be updated. To keep things simple (one set of args for run-model and no need
to keep updating the protocol definition), we have simply made the second
argument a list. </td>
  </tr>
</table>


### Obtain Job ID

When making a call to the backend to run the given model, a job ID is returned.

### Prepare and Return HTTP Response

The job ID that is obtained is then used to create the payload to send as a
response back to the client. What they should receive is:

* a link to a job resource (with the job ID as part of the link)

* the status of the newly created job

The user or application may then use this link to check on the status of the
submitted job; when the job is completed and the results are available,
requesting that same link will return the result set.

## Backend Model Run

The following sections cover the details of what happens when the REST server
initiates a model execution on the user's or application's behalf, walking
though what is mentioned above in the subsection [Make Backend Model Run
Call](#heading=h.pj4dy4gidgpd).

### Call Protocol Function

When a call is made from the REST server to execute a science model, the
function called is a function whose spec is defined in the
lcmap.see.backend.IModelable and whose implementation is provided by the
provided behaviour for the given backend type. While overrides for the default
behaviours are done in the appropriate backend namespace in
lcmap.see.backend.<backend type>, run-model itself is defined in the
lcmap.see.backend.base namespace. This is the run-model function that is
ultimately called by REST server, and is the one mentioned above that take two
arguments: the implementation of the backend (extended record) and the args
that will ultimately be passed to the model function.

### Lookup and Apply Backend Model

The run-model protocol function looks up the backend-specific model function
that every model is required to provide (by convention; when/if an IModel
protocol is defined, this would become a protocol function definition). The
lookup expects to find the model function in the following location:

  lcmap.see.backend.<backend-type>.models.<model name>/run-model

As you can see, the convention here requires consistent use of the model name,
knowledge of the name of the backend that is currently configured to run, and
the presence of a run-model function in the identified namespace.

Once the model is located, it does the following:

* prepends the backend implementation to the list of provided args

* calls the located model's run-model function using the updated args

This stage may seem an unnecessary level of abstraction, but it is actually
necessary in order to support configurable or dynamic backend with arbitrary
science models offered in its namespace.

## Science Model Run

This part of the walkthrough focuses on the preparatory steps for model
execution taken by the science model itself, providing all the required inputs
for science model execution management, which is done by the LCMAP SEE Job
Tracker.

### The Science Model Entrypoint

Once concept that needs to be made clear in this context:  the run-model that
is expected to exist in the namespace of a given science model is **_not _**a
method, but rather an **_entrypoint_**. A whole world of code may exist behind
this entrypoint, thus providing complete and unlimited flexibility for science
model developers. 

For native backend science models, this function usually points to a wrapper.
For science models built on Mesos frameworks, it points to the function that
kicks off a Mesos run. While initially all science models exist in the SEE
codebase, the existence and use of this run-model function allow for science
models to live anywhere and be written in any JVM language (or, as a docker
container, written in anything at all). 

<table>
  <tr>
    <td>IMPORTANT!

When creating models in the SEE, the top-level model namespace MUST contain a
run-model function.</td>
  </tr>
</table>


Back to the details of this run-model entrypoint: it takes the following
arguments:

* the backend implementation (extended record)

* the model name, as a string value

* successive args that were originally passed by the user to the REST server
  that are arguments for the actual science model itself

As such, in general the total number of args are arbitrary in number, but in
the particular case are n+2, where n is the number of science model arguments
for the given model.

While this is the entrypoint for science model execution, actual execution does
not occur here (as described in the section [Science
Model](#heading=h.ar87l3vx3mt4)). Instead, this function prepares for the
execution -- which will ultimately be performed by the science model tracker --
by initiating a job with the SEE Job Tracker. The run-model entrypoint function
does the following:

* instantiates a job tracker implementation

* identifies a model wrapper function that the tracker will call

* collects all the arguments needed by the model wrapper function

* calls the track-job protocol function, passing the tracker implementation,
  the model wrapper, and the collected args

### Model Definition

While not a step in its own right, this section offers elucidation for model
creators and maintainers in the context of the namespace where the run-model
entrypoint function is defined. There are several guiding concepts that should
be called out here, each in their own subsection below.

#### Understanding the Wrapper

As mentioned above, the entrypoint run-model function is responsible for
identifying (and then passing to the job-tracker function) the model wrapper.
But what is a model wrapper?

A model wrapper is a function that will ultimately be called by the SEE Job
Tracker, when it initiates the actual run of the science model execution. This
"wrapping" simplifies the fact that  a science model can be just about anything
that the JVM can execute (see next subsection for more details).

Here are the key features a wrapper function provides at this point in the
execution flow:

* provides a simple and clean way for the job tracker to run science models
  defined in any supported framework or backend

* provide a context for gathering science model results

#### Defining Model Execution

A science model wrapper defines the executable code and context for the science
model **_as it will be executed by the job tracker_**. It may initiate any code
execution supported by the JVM, such as:

* calling another SEE function, or via any number of community libraries; 

* calling code or system executables locally, or making remote calls; 

* executing simple functions, or calling to distributed compute infrastructure

As mentioned in the section [The Science Model
Entrypoint](#heading=h.cyenk5j110ic), in the case of science models built to
run on the native backend, the wrapper function is more or less procedural code
that performs executes a given function or set of functions. These functions
are usually placed in the same namespace as the entry-point function, due to
the relative simplicity of native OS model execution. 

In the case of science models built to run on the Mesos backend, the wrapper
function will usually be a function defined in the model's Mesos framework that
kicks of the framework and initiates the various executors.

These differences further highlight the benefits of having a wrapper function
at this point in the code: neither the entrypoint run-model function nor the
job trackers track-job function need to know any of the details about how a
given backend executes a particular model. Each model is backend-specific, so
the wrapper allows for the clean and easy maintenance of that responsibility
within the model itself.

#### Defining Results Handling

In the simplest case, the wrapper can define how results are handled.
Particularly, for backends that use a blocking mode of execution, while the
first set of functions to execute may be responsible for processing data, the
last set of functions will be responsible for gathering the results of those
calls in a manner that the science model has defined for successful outputs.
The is the "gather" of scatter-gather, and the "reduce" of map-reduce.

In the case of more complex science model implementations (i.e., ones that
require remote access to other services or utilize parallelization), the
results gathering will likely not happen at this point, but rather as part of
the processing in the model itself. Most scenarios of this nature will be
employing asynchronous communications, and thus the means by which results are
obtained will be via the job tracker receiving a message from the science
model.

### Issue Call to Job Tracker

The track-job function is called with the following arguments:

* a job tracker implementation that was instantiated by the run-model
  entrypoint function

* the model wrapper that this science model has defined for execution by the
  job tracker

* the collected arguments that will be needed by the job tracker when it calls
  the provided model wrapper function

<table>
  <tr>
    <td>NOTE

The return value of the call to track-job is a job ID, and this chain of
execution is what results in the REST server obtaining the job ID that is used
in the HTTP response it prepares. Furthermore, this is the point at which the
science model execution flow becomes asynchronous. (Whereas the
blocking/synchronous calls to this point is what allows the REST server to
obtain the job ID.)</td>
  </tr>
</table>


## Job Tracker Model Run

The final part of this document -- and the last part of the science model
execution flow -- focuses on the life-cycle of a science model that is
submitted to the LCMAP SEE Job Tracker. The Job Tracker is a Finite State
Machine (FSM) that governs the transitions from one state to another,
performing such actions is initiation, model execution, and results storage.

### The Job Tracker Event Handler and the FSM

In order to understand the subsequent stages of model execution, you'll need to
be aware that the FSM mentioned above has been defined and has functions
associated with each state transition. This was done behind the scenes when the
Job Tracker was instantiated for our science model by the run-model entrypoint
function for our given backend + science model combination. When a tracker/new
function is called, not only is an implementation of a tracker created and then
returned, it also connects the Job Tracker "dispatch" function as an event
handler for the Job Tracker event thread. (NOTE:  Micro thread, not operating
system thread. This is done using the Quasar library's implementation of the
Erlang actor model.)

This event handler (NOTE:  Its actual function name is dispatch-handler and is
defined as part of the ITrackable protocol.) function is where the FSM state
table (NOTE:  The state table is laid out in the "Job Tracking" section under
the Tracker Finite State Machine subsection.) is set up, thus defining the
actions that are followed for each state transition in the lifecycle of the Job
Tracker.

### "Trackable" Protocol Function track-job

In the last step of the previous section of the walktrough ([Issue Call to Job
Tracker](#heading=h.qfwbfz6nxyd7)), we saw how a science model's entrypoint
function calls the job tracker with everything the job tracker will need in
order to successfully execute the given science model on the configured
backend.

To review, the track-job function is called with the following arguments:

* a job tracker implementation that was instantiated by the run-model
  entrypoint function

* the model wrapper that this science model has defined for execution by the
  job tracker

* the collected arguments that will be needed by the job tracker when it calls
  the provided model wrapper function

The track-job function is defined in the lcmap.see.job.tracker.ITrackable
protocol. Each backend will have its own tracker implementation that is
instantiated once every time a science model needs to execute.

The track-job function is responsible for the following:

* generating the job ID for the given function, which is a hash of the function
  (including namespace) and its arguments 

* making the call that initiates the lifecycle of a job in the Job Tracker. 

For the last bullet above, this is done by sending a message to the Job Tracker
instance's event thread. In the payload of the message, the :type value is set
as :job-track-init. As we will see shortly, that value is what causes the job
tracker to commence the job life cycle for the given model.

### "Jobable" Protocol

In addition to the ITrackable protocol, an LCMAP SEE Job Tracker also
implements the lcmap.see.job.tracker.IJobable protocol. This protocol defines
all the functions which govern the state transitions of the Job Tracker's FSM.
Each of these is explored below, in the context of the execution flow of a
science model.

### Job Initialization

When the track-job function sends a message to the event thread, it sets the
message types as :job-track-init. When the event thread handler gets a message
with the type field set to this value, it calls the tracker/init-job-track
function, where tracker is a reference to a namespace that is
backend-dependent.

The init-job-track function then checks for the presence of already-extant
results for the given job ID.

### Check for Results

If results already exist for the given job ID, a message of type
:job-result-exists is sent to the event thread whereupon the event handler will
call the function tracker/return-existing-result. If no results are found in
the database for the given job ID, a message of type :job-start-run is sent to
the event thread and the tracker's implementation of the start-job-run function
(NOTE:  Backend-specific.) will be called.

### Results Exist

When the science model results already exist for the given function name and
parameters, the base tracker method return-existing-result (ultimately located
the the lcmap.see.job.tracker.base namespace) is called. The
return-existing-result function then sends a message to the event thread of
type :job-done. As we will see, this essentially short-circuits the model
execution process, while still taking advantage of the state transitions
defined for it.

### No Results, Execution Required

If, in function init-job-track, a query to the database revealed that no
results existed for the given job ID, the model execution job is started for
the given backend. In the case of the native backend, the location of that
function is here:

  lcmap.see.job.tracker.native/start-job-run

For the mesos backend, the function is here:

  lcmap.see.job.tracker.mesos/start-job-run

### Run Model

When a job run starts -- when the event handler calls start-job-run -- the job
status is set to pending. More importantly, the function and arguments that
were passed to the tracker's track-job function in the model's entrypoint
function run-model, get called -- thus ultimately being the point where an
actual science model begins running. 

* In the blocking case, the function is called, the result is obtained, and a
  message is sent to the event thread with the data from the obtained result.

* In the async case, the function is called with no expected result and no
  message is sent to the event thread, thus not initiating any state transition.
  However, the message-sending function and the next state to transition to are
  sent to the science model framework. Thus the science model 

<table>
  <tr>
    <td>IMPORTANT!

For asynchronous execution of science models, the science model becomes
responsible for executing a callback for getting results to the event thread.

This will often take the form of passing a partial function that has everything
it needs -- except the actual results -- for sending a completion message back
to the event thread. The science model only need call this callback with the
single argument of the results to have the job tracking life cycle picked up
again.
</td>
  </tr>
</table>


### Obtain Results

When the event thread has received a message of type :job-finish-run, the event
handler will call finish-job-run which lives in the same namespace as the
start-job-run function. This function is responsible for taking the results
that it receives and sending a message to the event thread saying it has
results and they are ready to be saved.

<table>
  <tr>
    <td>NOTE

The two functions start-job-run and finish-job-run were initially a single
function. However, as soon as we needed to support asynchronous processing of
science models, we needed two different state transitions for running a job:
kicking it off, and in the case of async results, obtaining the results. 

For blocking code, these are run sequentially -- still as two functions -- but
as if they were one, emulating the original single-function use case.</td>
  </tr>
</table>


Any post-processing required by the backend model run can be done in the
finish-job-run function, as this is the last chance to do so before the results
are saved to the database.

The final responsibility of the finish-job-run function is to send a
:job-save-data message.

### Save Results

When the dispatch function processing the :job-save-data message, it runs the
save-job-data callback function for the backend. This function then sends the
:job-track-finish message.

### Update Job Status and Completion

Upon receiving the :job-track-finish message, the dispatch function runs the
finish-job-track callback, which is responsible for end-of-lifecycle actions
such as setting the appropriate status for the job. This function's final act
is to send the :job-done message which, in turn, will cause the done function
to execute. This function has few responsibilities other than to indicate the
completion of the job tracking lifecycle. Mostly this function is used to send
messages or create logging events for the completion of the job.

# Appendices

## Appendix I: Using Compiled Mesos Libraries

export MESOS_NATIVE_JAVA_LIBRARY=yourbuilddir/src/.libs/libmesos.so

or:

export MESOS_NATIVE_JAVA_LIBRARY=yourinstalldir/libmesos.so

## Appendix II: Starting a Local Mesos Cluster

nohup ./bin/mesos-master.sh \

    --work_dir=/tmp/mesos-master \

    --ip=yourip > master.log &

sudo nohup ./bin/mesos-slave.sh \

    --work_dir=/tmp/mesos-agent \

    --ip=yourip --master=yourip:5050 > agent.log &

tail -f *.log

## Appendix III: Who Does What, When, & For How Long?

[Resources in executors and tasks and how these provide the level of
flexibility needed for fine-grained control of access to compute resources for
the various projects and roles which will be utilizing a general Mesos
deployment at USGS/EROS.]

## Appendix IV: Developing Models for the SEE

TBD

### Command Line Models

TBD

### Docker Models

TBD

### Java and Clojure Models

TBD

### Python Models

TBD

