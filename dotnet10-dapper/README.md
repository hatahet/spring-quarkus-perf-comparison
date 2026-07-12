# ASP.NET Core 10 with Dapper

This implementation uses ASP.NET Core Minimal APIs, Dapper, and the Npgsql
connection pool. It performs no schema generation; the shared benchmark
infrastructure creates and seeds PostgreSQL.

```shell
dotnet publish Dotnet10Dapper -c Release -o publish
./publish/dotnet10-dapper
```
