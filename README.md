# Parallel Web Crawler

A parallel web crawler delivered as part of DevOps with Java Nano-degree by Swift and Udacity.

## Brief from the handout:

>Welcome! This is your first week at the startup, UdaciSearch. You've been hired on as an Engineer, and you're really excited to make a big splash. UdaciSearch is interested in figuring out popular search terms on the internet in order to improve the SEO of their clients. Everyone wants to pop up at the top of a potential user's search!

>You are given the source code for your company's legacy web crawler, which is single-threaded. You notice it's a bit slow, and you quickly realize a way to improve its performance and impress your new manager. You can upgrade the code to take advantage of multi-core architectures to increase crawler throughput. Furthermore, you will measure the performance of your crawler to prove that, given the same amount of time, the multi-threaded implementation can visit more web pages than the legacy implementation. It might just be your first week, but you're set to impress!


# Software Requirements

- Java JDK Version 17+
- Maven 3.6.3 or higher

# Build the Project

```
mvn package
```

This will print out various actions, and end as follows:

```
 ...
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  7.174 s
[INFO] Finished at: 2023-05-07T21:39:20+01:00
[INFO] ------------------------------------------------------------------------
```

# Test the Project

Test the newly compiled and packaged JAR with the following command:

```
java -classpath target/udacity-webcrawler-1.0.jar com.udacity.webcrawler.main.WebCrawlerMain src/main/config/sample_config_disallow_robots.json
```

The output should be similar to the following:

*resultData.txt*

```
{"wordCounts":{"learning":153,"data":151,"udacity":117,"machine":111,"with":103},"urlsVisited":8}%
```

*profileData.txt*

```
Run at Mon, 8 May 2023 01:23:24 GMT
com.udacity.webcrawler.parser.PageParserImpl#parse took 0m 4s 704ms (called 18 times)
[Thread ID: 32 (called 5 times)] - Average duration: 0m 0s 940ms
[Thread ID: 18 (called 6 times)] - Average duration: 0m 0s 784ms
[Thread ID: 21 (called 1 times)] - Average duration: 0m 4s 704ms
[Thread ID: 25 (called 6 times)] - Average duration: 0m 0s 784ms

com.udacity.webcrawler.ParallelWebCrawler#crawl took 0m 10s 113ms (called 1 times)
[Thread ID: 1 (called 1 times)] - Average duration: 0m 10s 113ms
```

**Note:** To see several threads in action you might need to use the VM option -XX:ActiveProcessorCount=<n> to override the number of CPUs that the VM automatically detects.

# Additional sample configs available

*sample_config_timeout_test.json*

- Helpful to test whether the crawler times out correctly.

*sample_config_disallow_robots.json*

 - Helpful to test whether the crawler skips web pages that declare a disallow in the robots.txt.

*sample_config_system_out.json*

- Helpful to test whether the crawler and profiler results are printed to system out when respective output paths are not given in the config.

*sample_config_multiple_pages.json*

- Helpful to test a greater depth, time out and multiple start pages.

# Clean up

To clean up run:

```
mvn clean
```
