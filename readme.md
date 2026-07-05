# Project Setup Guide

Follow the steps below to set up and run the project.

## Prerequisites

-   Java (required version for the project)
-   Maven or Gradle
-   Docker (recommended)
-   MySQL
-   RabbitMQ

## Step 1: Create the `.env` file

Copy the `.env.example` file and create a new `.env` file.

``` bash
cp .env.example .env
```

Fill in the required environment variables later in Step 5.

------------------------------------------------------------------------

## Step 2: Start required services

Use Docker (recommended) or install the services manually.

Required services:

-   MySQL
-   RabbitMQ

Example:

``` bash
docker compose up -d
```

------------------------------------------------------------------------

## Step 3: Initialize the database

Execute the `db.sql` script in your MySQL instance to create the
project's database schema.

------------------------------------------------------------------------

## Step 4: Create required API keys

Generate all API keys and credentials listed in `.env.example`.

Examples may include:

-   Cloudinary
-   Resend
-   Google OAuth
-   Cloudflare Turnstile
-   SePay
-   Any other third-party services used by the project

------------------------------------------------------------------------

## Step 5: Update the `.env` file

Replace all placeholder values in `.env` with the API keys and
credentials created in Step 4.

------------------------------------------------------------------------

## Step 6: Configure `application.yaml`

Complete any remaining configuration values in `application.yaml` that
are not provided through environment variables.

Verify items such as:

-   Database connection
-   RabbitMQ configuration
-   Server configuration
-   OAuth settings
-   Other project-specific properties

------------------------------------------------------------------------

## Step 7: Run the application

Start the project using your preferred method.

Using Maven:

``` bash
mvn spring-boot:run
```

Or build and run:

``` bash
mvn clean package
java -jar target/*.jar
```
