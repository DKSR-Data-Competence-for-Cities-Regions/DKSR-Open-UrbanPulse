# UPSecurityRealm documentation
## Overview
This project integrates the Shiro security framework for authentication and authorization. It can be pulled in as a dependency and configured for an authentication service based on the credentials in the UPManagement database `up_users`. It consists of two implementations of Shiro's `AuthorizingRealm`, one for HMAC authentication and one for HTTP Basic authentication. Both use a DAO to check the provided credentials against UPManagement's `up_users` table.

The configuration of a project will be described in the following paragraph. Further information on Shiro can be found in the following documents:
* `\Dropbox ([ui!])\Dev\Spikes\Spike 4779 - Investigate in tools and frameworks to support the user authentication and right management`
* `\Dropbox ([ui!])\Dev\Technologies\Apache Shiro\InfoQ-Pairing-Apache-Shiro-and-Java-EE7.pdf`

## Integration into Projects
When integrating our custom Shiro authentication into a given project, following files have to be modified or added. Sample files can be found in `CockpitV2/DashboardBackends/Environment` or `UrbanPulseEP/UrbanPulseJEE/UrbanPulseManagement`

* `pom.xml`: simply add the dependency:
```xml
<dependency>
    <groupId>de.urbanpulse.dist.jee</groupId>
    <artifactId>UPSecurityRealm</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

* Copy a `ShiroListener.java` and a `ShiroFilterActivator.java` class into an adequate package.

 Instead of adding `ShiroFilterActivator.java`, one may add following to the `web.xml`
 ```xml
 <listener>
     <listener-class>{package}.ShiroListener</listener-class>
 </listener>
 ```

* `ShiroConfiguration.java`: copy a `ShiroConfiguration.java` into the project and configure as needed, i.e. setting the realm, the filter chain, session configuration etc.

* ehcache: For now, the ehcache cache is not used but with it we will be to able to distribute the session cache. To add, simply add dependency
   ```xml
   <dependency>
       <groupId>org.apache.shiro</groupId>
       <artifactId>shiro-ehcache</artifactId>
       <version>1.4.0-RC2</version>
       <type>jar</type>
   </dependency>
   ```
   and configure it in `ShiroConfiguration.java`:
   ```java
   securityManager = new DefaultWebSecurityManager(realm);
   CacheManager cacheManager = new EhCacheManager();
   ((EhCacheManager) cacheManager).setCacheManagerConfigFile("classpath:ehcache.xml");
   securityManager.setCacheManager(cacheManager);
   ```
   Put a file `ehchache.xml` into `src/main/resources` with following content:
   ```
   <ehcache name="shiro" updateCheck="false">
    <diskStore path="/path/to/storage"/>

    <defaultCache
            maxElementsInMemory="10000"
            eternal="false"
            timeToIdleSeconds="120"
            timeToLiveSeconds="120"
            overflowToDisk="false"
            diskPersistent="false"
            diskExpiryThreadIntervalSeconds="120"
    />
</ehcache>
   ```

## Some notes

### DashboardFrontend
The DashboardFrontend project has been modified to use the shiro realms and uses a provided `authc` filter that redirects to the loginpage of the Dashboard when not logged in/no credentials are provided.

### Backend Tiles
The Backend Tiles have a custom implementation of a Basic filter that also checks for a `token` query parameter with basic auth credentials that are used by the websockets established by the frontend to send credentials to the backend.

### UrbanPulseManagement
Here, the HMAC filter and a custom BodyWrapperFilter are used. The BodyWrapperFilter internally wraps a ServletRequest to be readable several times instead of only once. This has to be done because the body gets read for the HMAC check first and then has to trigger the intended command itself .

## Integrated Projects
To get example configurations, you can have a look at the already configured projects
* UrbanPulseManagement
* Backend Tiles (Environment)
* DashboardFrontend

