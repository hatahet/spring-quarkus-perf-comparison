using Dotnet10Dapper.Dto;
using Dotnet10Dapper.Repository;

namespace Dotnet10Dapper.Service;

public sealed class FruitService(IFruitRepository repository)
{
    public Task<List<FruitDto>> GetAllFruitsAsync(CancellationToken cancellationToken) =>
        repository.ListAllAsync(cancellationToken);

    public Task<FruitDto?> GetFruitByNameAsync(string name, CancellationToken cancellationToken) =>
        repository.FindByNameAsync(name, cancellationToken);

    public Task<FruitDto> CreateFruitAsync(FruitDto fruit, CancellationToken cancellationToken) =>
        repository.PersistAsync(fruit, cancellationToken);
}
