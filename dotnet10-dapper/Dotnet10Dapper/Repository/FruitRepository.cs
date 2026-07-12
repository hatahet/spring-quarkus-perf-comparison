using System.Data;
using Dapper;
using Dotnet10Dapper.Dto;
using Npgsql;

namespace Dotnet10Dapper.Repository;

public sealed class FruitRepository(NpgsqlDataSource dataSource) : IFruitRepository
{
    private const string SelectSql = """
        SELECT f.id AS FruitId, f.name AS FruitName, f.description AS Description,
               p.price AS Price, s.id AS StoreId, s.name AS StoreName,
               s.currency AS Currency, s.address AS Address, s.city AS City,
               s.country AS Country
          FROM fruits f
          LEFT JOIN store_fruit_prices p ON p.fruit_id = f.id
          LEFT JOIN stores s ON s.id = p.store_id
        """;

    public async Task<List<FruitDto>> ListAllAsync(CancellationToken cancellationToken = default)
    {
        await using var connection = await dataSource.OpenConnectionAsync(cancellationToken);
        var rows = await connection.QueryAsync<FruitRow>(
            new CommandDefinition(SelectSql + " ORDER BY f.id, s.id", cancellationToken: cancellationToken));
        return Map(rows);
    }

    public async Task<FruitDto?> FindByNameAsync(string name, CancellationToken cancellationToken = default)
    {
        await using var connection = await dataSource.OpenConnectionAsync(cancellationToken);
        var rows = await connection.QueryAsync<FruitRow>(new CommandDefinition(
            SelectSql + " WHERE f.name = @Name ORDER BY s.id", new { Name = name },
            cancellationToken: cancellationToken));
        return Map(rows).FirstOrDefault();
    }

    public async Task<FruitDto> PersistAsync(FruitDto fruit, CancellationToken cancellationToken = default)
    {
        await using var connection = await dataSource.OpenConnectionAsync(cancellationToken);
        await using var transaction = await connection.BeginTransactionAsync(cancellationToken);
        try
        {
            var id = await connection.ExecuteScalarAsync<long>(new CommandDefinition(
                """INSERT INTO fruits (id, name, description) VALUES (nextval('fruits_seq'), @Name, @Description) RETURNING id""",
                new { fruit.Name, fruit.Description }, transaction, cancellationToken: cancellationToken));
            await transaction.CommitAsync(cancellationToken);
            return new FruitDto(id, fruit.Name, fruit.Description, []);
        }
        catch
        {
            await transaction.RollbackAsync(cancellationToken);
            throw;
        }
    }

    private static List<FruitDto> Map(IEnumerable<FruitRow> rows)
    {
        var fruits = new Dictionary<long, FruitAccumulator>();
        foreach (var row in rows)
        {
            if (!fruits.TryGetValue(row.FruitId, out var fruit))
            {
                fruit = new FruitAccumulator(row.FruitId, row.FruitName, row.Description);
                fruits.Add(row.FruitId, fruit);
            }
            if (row.StoreId is not null)
            {
                var address = new AddressDto(row.Address!, row.City!, row.Country!);
                var store = new StoreDto(row.StoreId, row.StoreName!, row.Currency!, address);
                fruit.StorePrices.Add(new StoreFruitPriceDto(store, (float)row.Price!.Value));
            }
        }
        return fruits.Values.Select(f => new FruitDto(f.Id, f.Name, f.Description, f.StorePrices)).ToList();
    }

    private sealed record FruitRow(long FruitId, string FruitName, string? Description,
        decimal? Price, long? StoreId, string? StoreName, string? Currency,
        string? Address, string? City, string? Country);

    private sealed record FruitAccumulator(long Id, string Name, string? Description)
    {
        public List<StoreFruitPriceDto> StorePrices { get; } = [];
    }
}
