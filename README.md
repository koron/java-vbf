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
      <version>0.0.2</version>
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

Example code to using VBF with Redis backend.

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
