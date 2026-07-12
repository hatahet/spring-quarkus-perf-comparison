using System.ComponentModel.DataAnnotations;

namespace Dotnet10Dapper.Dto;

public record FruitDto(
    long? Id,
    [property: Required(ErrorMessage = "Name is mandatory")] string? Name,
    string? Description,
    IReadOnlyList<StoreFruitPriceDto>? StorePrices = null)
{
    public IReadOnlyList<StoreFruitPriceDto> StorePrices { get; init; } = StorePrices ?? [];
}
