# Active Directory Microservice
This is a simple REST microservice for some basic queries on a Microsoft
Active Directory.

# Configuration
A `configuration.properties` file must exist in the working directory
(or the corresponding environment variables, e.g. `ACTIVEDIRECTORY_USERNAME` for `activedirectory.username` are set) to
determine the Active Directory; see `defaults.properties` or `docker-compose.yml` for an example.

# HTTP API
## Get user information
`curl -X GET http://localhost:80/users/Dumbledore -H 'Content-Type:
application/json'`
```
{
    "username": "Dumbledore",
    "givenname": "Albus",
    "mail": "albus@hogwarts.edu",
    "cn": "Albus Percival Wulfric Brian Dumbledore"
}
```
