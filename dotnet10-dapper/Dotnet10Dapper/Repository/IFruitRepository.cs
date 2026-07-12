using Dotnet10Dapper.Dto;

namespace Dotnet10Dapper.Repository;

public interface IFruitRepository
{
    Task<List<FruitDto>> ListAllAsync(CancellationToken cancellationToken = default);
    Task<FruitDto?> FindByNameAsync(string name, CancellationToken cancellationToken = default);
    Task<FruitDto> PersistAsync(FruitDto fruit, CancellationToken cancellationToken = default);
}
