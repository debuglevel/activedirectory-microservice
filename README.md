<!--- some badges to display on the GitHub page -->
![Travis (.org)](https://img.shields.io/travis/debuglevel/activedirectory-microservice?label=Travis%20build)
![Gitlab pipeline status](https://img.shields.io/gitlab/pipeline/debuglevel/activedirectory-microservice?label=GitLab%20build)
![GitHub release (latest SemVer)](https://img.shields.io/github/v/release/debuglevel/activedirectory-microservice?sort=semver)
![GitHub](https://img.shields.io/github/license/debuglevel/activedirectory-microservice)

# Greeter Microservice
This is a simple REST microservice for some basic queries on a Microsoft Active Directory.

# HTTP API

## Swagger / OpenAPI
There is an OpenAPI (former: Swagger) specification created, which is available at <http://localhost:8080/swagger/activedirectory-microservice-0.2.0.yml> (or somewhere in the jar file). It can easily be pasted into the [Swagger Editor](https://editor.swagger.io) which provides a live demo for [Swagger UI](https://swagger.io/tools/swagger-ui/), but also offers to create client libraries via [Swagger Codegen](https://swagger.io/tools/swagger-codegen/).

## Users
To get information about a single user, send a GET request to the service:

```
$ echo "SECRET_USERNAME:SECRET_PASSWORD" | base64
U0VDUkVUX1VTRVJOQU1FOlNFQ1JFVF9QQVNTV09SRAo=

$ curl -X GET http://localhost:8080/users/Dumbledore -H "Authorization: Basic U0VDUkVUX1VTRVJOQU1FOlNFQ1JFVF9QQVNTV09SRA=="
{
    "username": "Dumbledore",
    "givenname": "Albus",
    "mail": "albus@hogwarts.edu",
    "cn": "Albus Percival Wulfric Brian Dumbledore"
}
```

To get information about all users, send a GET request to the service:

```
$ echo "SECRET_USERNAME:SECRET_PASSWORD" | base64
U0VDUkVUX1VTRVJOQU1FOlNFQ1JFVF9QQVNTV09SRAo=

$ curl -X GET http://localhost:8080/users/ -H "Authorization: Basic U0VDUkVUX1VTRVJOQU1FOlNFQ1JFVF9QQVNTV09SRA=="
[
    {
        "username": "alexaloah",
        "givenname": "Alex",
        "mail": "alex@aloah.de",
        "cn": "Alex Aloah",
        "disabled": false
    },
    {
        "username": "maxmustermann",
        "givenname": "Max",
        "mail": "max@mustermann.de",
        "cn": "Max Mustermann",
        "disabled": false
    }
]
```

## Computers
```
$ echo "SECRET_USERNAME:SECRET_PASSWORD" | base64
U0VDUkVUX1VTRVJOQU1FOlNFQ1JFVF9QQVNTV09SRAo=

$ curl -X GET http://localhost:8080/computers/ -H "Authorization: Basic U0VDUkVUX1VTRVJOQU1FOlNFQ1JFVF9QQVNTV09SRA=="
[
  {
    "cn": "Desktop",
    "logonCount": 42,
    "operatingSystem": "Windows 10 Pro",
    "operatingSystemVersion": "10.0 (17134)",
    "lastLogon": "2020-08-04 12:34:56",
    "whenCreated": "2019-12-24 12:34:56"
  },
  {
    "cn": "Laptop",
    "logonCount": 23,
    "operatingSystem": "Windows 7 Professional",
    "operatingSystemVersion": "6.1 (7601)",
    "lastLogon": "2020-08-04 12:34:56",
    "whenCreated": "2019-12-24 12:34:56"
  }
]
```

# Configuration
There is a `application.yml` included in the jar file. Its content can be modified and saved as a separate `application.yml` on the level of the jar file. Configuration can also be applied via the other supported ways of Micronaut (see <https://docs.micronaut.io/latest/guide/index.html#config>). For Docker, the configuration via environment variables is the most interesting one (see `docker-compose.yml`).
