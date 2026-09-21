package com.reqres.automation.base;

import com.reqres.automation.clients.rest.UserRestClient;

/**
 * Builds a thread-local {@link UserRestClient} (and its underlying
 * RequestSpecification) once per test class, released after the class
 * finishes. Parallel-safe: no shared mutable instance state between test
 * methods running on different threads.
 */
public abstract class BaseRestTest implements BaseRestInterface{

}
