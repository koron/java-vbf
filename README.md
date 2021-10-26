# Volatile Bloom Filter for Java

java-vbf is an implementation of Volatile Bloom Filter for Java.

## Pre-requirements

java-hlltc is available on [GitHub Packages][gp].
([Japanese version][gp-ja])

[gp]:https://docs.github.com/en/packages
[gp-ja]:https://docs.github.com/ja/packages

### for Maven

1.  Create a personal access token with `read:packages` permission at <https://github.com/settings/tokens>

2.  Put username and token to your ~/.m2/settings.xml file with `<server>` tag.

    ```pom
    <settings>
      <servers>
        <server>
          <id>github</id>
          <username>USERNAME</username>
          <password>YOUR_PERSONAL_ACCESS_TOKEN_WITH_READ</password>
        </server>
      </servers>
    </settings>
    ```

3.  Add a repository to your `repositories` section in project's pom.xml file.

    ```pom
    <repository>
      <id>github</id>
      <url>https://maven.pkg.github.com/koron/java-vbf</url>
    </repository>
    ```

4.  Add a `<dependency>` tag to your `<dependencies>` tag.

    ```pom
    <dependency>
      <groupId>net.kaoriya</groupId>
      <artifactId>vbf</artifactId>
      <version>0.2.0</version>
    </dependency>
    ```

Please read [public document](https://docs.github.com/en/packages/guides/configuring-apache-maven-for-use-with-github-packages) also. ([Japanese](https://docs.github.com/ja/packages/guides/configuring-apache-maven-for-use-with-github-packages))

### for Gradle

1.  Create a personal access token with `read:packages` permission at <https://github.com/settings/tokens>

2.  Put username and token to your ~/.gradle/gradle.properties file.

    ```
    gpr.user=YOUR_USERNAME
    gpr.key=YOUR_PERSONAL_ACCESS_TOKEN_WITH_READ:PACKAGES
    ```

3.  Add a repository to your `repositories` section in build.gradle file.

    ```groovy
    maven {
        url = uri("https://maven.pkg.github.com/koron/java-vbf")
        credentials {
            username = project.findProperty("gpr.user") ?: System.getenv("USERNAME")
            password = project.findProperty("gpr.key") ?: System.getenv("TOKEN")
        }
    }
    ```

4.  Add an `implementation` to your `dependencies` section.

    ```groovy
    implementation 'net.kaoriya:vbf:0.0.2'
    ```

Please read [public document](https://docs.github.com/en/packages/guides/configuring-gradle-for-use-with-github-packages) also. ([Japanese](https://docs.github.com/ja/packages/guides/configuring-gradle-for-use-with-github-packages)).

## Getting Started

### with VBF3 Redis

Example code to using VBF3 with Redis backend.
There more information about VBF3 in [this document][vbf3readme].

[vbf3readme]:./docs/vbf3-readme.md

```java
import net.kaoriya.vbf3.RedisVBF3;

// connect Redis with Jedis
JedisPool pool = new JedisPool(new JedisPoolConfig(), "localhost");
try (Jedis jedis = pool.getResource()) {

  ///////////////////////////////////////////////////////////////////////////
  // (1/6) Setting up a filter

  // Parameters for VBF3
  final long  m = (long)1000;       // size in bytes of this VBF
  final short k = (short)7;         // number of hashes
  final short maxLife = (short)52;  // max of life, for `put()`

  // Open an existing VB-Filter, or create new one.
  // This will be failed if existing one have wrong parameters.
  RedisVBF3 f = new RedisVBF3.open(jedis, "redis-key-base-of-filter", m, k);

  ///////////////////////////////////////////////////////////////////////////
  // (2/6) Putting data

  // put some values to a VBF (with max life time)
  f.put((short)52, "value001");
  f.put((short)52, "value002", "value003");

  // put a value with short life time.
  f.put((short)10, ,"value004".getBytes());

  // put a string value. (`put(String, short)` is provoided.
  f.put((short)52, "value005");

  ///////////////////////////////////////////////////////////////////////////
  // (3/6) Checking data existence

  // Check with `byte[]`
  f.check("value001".getBytes());  // this should return `true`

  // Check with `String`
  f.check("value002");             // this should return `true`

  // Check a data which not put
  f.check("no-exists");            // this should return `false`

  ///////////////////////////////////////////////////////////////////////////
  // (4/6) Advance generation (forget old data)

  // Forward 10 generations. as a result, above `value004` (which have short
  // life time) will be forgotten.
  f.advanceGeneration((short)10);

  // This should return `false` now
  f.check("value004");

  ///////////////////////////////////////////////////////////////////////////
  // (5/6) Sweep expired data

  f.sweep();

  ///////////////////////////////////////////////////////////////////////////
  // (6/6) Drop an unused filter. It removes corresponding values from Redis.

  f.drop();
}
```

### with VBF(1) Redis

Example code to using VBF with Redis backend.
"8" in "VBF8" means 8-bits for managing generation.

```java
JedisPool pool = new JedisPool(new JedisPoolConfig(), "localhost");
try (Jedis jedis = pool.getResource()) {
  final int m = 1000; // size in bytes of this VBF
  final int k = 7;    // number of hashes
  RedisVBF8 vbf = new RedisVBF8(jedis, "redis-key-for-this-vbf", m, k);

  // put some values to a VBF
  vbf.put("value001".getBytes());
  vbf.put("value002".getBytes());
  vbf.put("value003".getBytes());

  // check VBF has values
  vbf.check("value001".getBytes(), 0);  // this should return `true`
  vbf.check("value002".getBytes(), 0);  // this should return `true`
  vbf.check("no-exists".getBytes(), 0); // this should return `false`

  // subtract 1 from TTL of all flags.
  vbf.subtract(1);

  // (OPTION) delete/clear the key from Redis
  vbf.delete();
}
```

## Test with redis on docker

Redis on docker:

```console
# Start redis on docker
$ docker run --rm --name vbf-redis -p 6379:6379 -d redis:6.2.3-alpine3.13

# Connect by cli
$ docker exec -it vbf-redis redis-cli

# Stop redis on docker
$ docker stop vbf-redis
```

Test

```console
# Run all tests
$ ./gradlew test

# Run partial tests (only vbf3)
$ ./gradlew test --tests '*.vbf3.*'
```
