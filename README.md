# Search Engine

## Overview
This project is a multi-threaded search engine written from scratch. The data structure behind the search engine is an <b>inverted index</b>, which stores the locations and counts of each word. 

## How it Works
`Driver.java` starts the search engine and accepts a number of flags to customize how the behavior of the search engine. When the driver code runs, it will build the inverted index with multiple threads by crawling the web starting from the source URL provided. After the inverted index has been built, the server will start (default port 8080 unless otherwise specified) and the user can navigate to the search engine home page and perfom searches.
