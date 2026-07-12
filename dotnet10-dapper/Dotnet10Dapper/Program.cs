using Dotnet10Dapper;
using Dotnet10Dapper.Dto;
using Dotnet10Dapper.Health;
using Dotnet10Dapper.Repository;
using Dotnet10Dapper.Service;
using Npgsql;
using OpenTelemetry.Metrics;
using OpenTelemetry.Resources;
using OpenTelemetry.Trace;

var startTime = DateTime.UtcNow;
var builder = WebApplication.CreateSlimBuilder(args);

builder.Services.ConfigureHttpJsonOptions(options =>
    options.SerializerOptions.TypeInfoResolverChain.Insert(0, FruitJsonContext.Default));

var connectionString = builder.Configuration.GetConnectionString("DefaultConnection")
    ?? "Host=localhost;Port=5432;Database=fruits;Username=fruits;Password=fruits;Pooling=true;Minimum Pool Size=0;Maximum Pool Size=50;Max Auto Prepare=100;Auto Prepare Min Usages=2";
builder.Services.AddSingleton(_ => NpgsqlDataSource.Create(connectionString));
builder.Services.AddScoped<IFruitRepository, FruitRepository>();
builder.Services.AddScoped<FruitService>();
builder.Services.AddHealthChecks().AddCheck<DatabaseHealthCheck>("postgresql");

builder.Services.AddOpenTelemetry()
    .ConfigureResource(resource => resource.AddService("dotnet10-dapper"))
    .WithTracing(tracing => tracing
        .SetSampler(new TraceIdRatioBasedSampler(0.1))
        .AddAspNetCoreInstrumentation()
        .AddSource("Npgsql")
        .AddOtlpExporter())
    .WithMetrics(metrics => metrics
        .AddMeter("Microsoft.AspNetCore.Hosting")
        .AddMeter("Microsoft.AspNetCore.Server.Kestrel")
        .AddRuntimeInstrumentation()
        .AddOtlpExporter());

builder.WebHost.UseUrls("http://0.0.0.0:8080");
var app = builder.Build();
var fruits = app.MapGroup("/fruits");

fruits.MapGet("/", async (FruitService service, CancellationToken ct) =>
    Results.Ok(await service.GetAllFruitsAsync(ct)));
fruits.MapGet("/{name}", async (string name, FruitService service, CancellationToken ct) =>
{
    if (string.IsNullOrWhiteSpace(name)) return Results.BadRequest();
    var fruit = await service.GetFruitByNameAsync(name, ct);
    return fruit is null ? Results.NotFound() : Results.Ok(fruit);
});
fruits.MapPost("/", async (FruitDto dto, FruitService service, CancellationToken ct) =>
{
    if (string.IsNullOrWhiteSpace(dto.Name))
        return Results.ValidationProblem(new Dictionary<string, string[]> { ["name"] = ["Name is mandatory"] });
    return Results.Ok(await service.CreateFruitAsync(dto, ct));
});

app.MapHealthChecks("/health/live");
app.MapHealthChecks("/health/ready");
app.Lifetime.ApplicationStarted.Register(() =>
    app.Logger.LogInformation("dotnet10-dapper started in {Elapsed:F3}s",
        (DateTime.UtcNow - startTime).TotalSeconds));
app.Run();
